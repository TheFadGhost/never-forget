package com.thefadghost.neverforget.backup

import java.time.MonthDay

data class CsvPerson(
    val name: String,
    val relationship: String,
    val month: Int,
    val day: Int,
    val year: Int?,
)

data class CsvDecodeResult(
    val validRows: List<CsvPerson>,
    val errors: List<String>,
)

object PeopleCsvCodec {
    private const val header = "name,relationship,month,day,year"

    fun encode(rows: List<CsvPerson>): String = buildString {
        appendLine(header)
        rows.forEach { person ->
            appendLine(
                listOf(
                    person.name,
                    person.relationship,
                    person.month.toString(),
                    person.day.toString(),
                    person.year?.toString().orEmpty(),
                ).joinToString(",") { encodeField(it) },
            )
        }
    }

    fun decode(csv: String): CsvDecodeResult {
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty() || lines.first().trim().lowercase() != header) {
            return CsvDecodeResult(emptyList(), listOf("Missing CSV header: $header"))
        }
        val valid = mutableListOf<CsvPerson>()
        val errors = mutableListOf<String>()
        lines.drop(1).forEachIndexed { index, line ->
            val lineNumber = index + 2
            val fields = parseLine(line)
            val month = fields.getOrNull(2)?.toIntOrNull()
            val day = fields.getOrNull(3)?.toIntOrNull()
            val yearText = fields.getOrNull(4).orEmpty()
            val year = yearText.takeIf { it.isNotBlank() }?.toIntOrNull()
            val name = fields.getOrNull(0).orEmpty().trim()
            val relationship = fields.getOrNull(1).orEmpty().trim().uppercase()
            val validDate = if (month != null && day != null) {
                runCatching { MonthDay.of(month, day) }.isSuccess
            } else {
                false
            }
            if (fields.size != 5 || name.isBlank() || relationship.isBlank() || !validDate ||
                (yearText.isNotBlank() && year == null)
            ) {
                errors += "Invalid data on line $lineNumber"
            } else {
                valid += CsvPerson(name, relationship, requireNotNull(month), requireNotNull(day), year)
            }
        }
        return CsvDecodeResult(valid, errors)
    }

    private fun encodeField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()
        return fields
    }
}

