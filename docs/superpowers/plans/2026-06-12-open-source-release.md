# Never Forget Open-Source Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a privacy-safe Never Forget repository with tested `.ics` calendar interoperability and complete open-source maintenance files.

**Architecture:** A pure Kotlin calendar codec converts between portable event values and RFC 5545 text. The Android view model maps database entities to the codec and exposes import/export actions to the existing Settings data section.

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit 4, Gradle, GitHub Actions

---

### Task 1: Calendar Codec

**Files:**
- Create: `app/src/main/kotlin/com/thefadghost/neverforget/calendar/CalendarIcsCodec.kt`
- Create: `app/src/test/kotlin/com/thefadghost/neverforget/calendar/CalendarIcsCodecTest.kt`

- [ ] Write tests for escaping, yearly recurrence, line unfolding, date-time import, and malformed-event reporting.
- [ ] Run the focused test and confirm it fails because the codec does not exist.
- [ ] Implement the minimal pure Kotlin codec.
- [ ] Run the focused test and confirm it passes.

### Task 2: Database and Settings Integration

**Files:**
- Modify: `app/src/main/kotlin/com/thefadghost/neverforget/NeverForgetViewModel.kt`
- Modify: `app/src/main/kotlin/com/thefadghost/neverforget/ui/AppScreens.kt`

- [ ] Add view-model export and duplicate-aware import functions.
- [ ] Add `.ics` create/open document actions to Settings.
- [ ] Trigger reminder repair after successful imports.
- [ ] Run app unit tests.

### Task 3: Public Maintenance Layer

**Files:**
- Create: `LICENSE`
- Create: `CONTRIBUTING.md`
- Create: `CODE_OF_CONDUCT.md`
- Create: `SECURITY.md`
- Create: `.github/ISSUE_TEMPLATE/bug_report.yml`
- Create: `.github/ISSUE_TEMPLATE/feature_request.yml`
- Create: `.github/pull_request_template.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `app/build.gradle.kts`

- [ ] Document setup, architecture, privacy, contribution workflow, and roadmap.
- [ ] Update the version to 1.1.0.
- [ ] Document the `.ics` feature in the changelog.

### Task 4: Verification and Publication

- [ ] Run `gradlew test lintDebug assembleDebug`.
- [ ] Scan tracked files for credentials, personal paths, databases, and private configuration.
- [ ] Initialize a fresh Git repository and create one initial commit.
- [ ] Create the public GitHub repository and push `main`.
- [ ] Verify public visibility and Git history.
