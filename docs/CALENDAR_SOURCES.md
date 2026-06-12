# Calendar Sources

Country packs are versioned data files under `core/data/src/main/resources/countries`. Each event stores its authority, URL, and verification date.

## United Kingdom

- Bank holidays and regional substitutions: [GOV.UK](https://www.gov.uk/bank-holidays)
- Easter, Christmas, and Mothering Sunday: [Church of England](https://www.churchofengland.org/prayer-and-worship)
- Father's Day background: [UK Parliament](https://commonslibrary.parliament.uk/research-briefings/cdp-2022-0114/)
- St George's Day: [Historic England](https://historicengland.org.uk/listing/what-is-designation/heritage-highlights/st-georges-day/)

The pack distinguishes England and Wales, Scotland, and Northern Ireland.

## Bulgaria

- Statutory public holidays: [Bulgarian Labour Code, Article 154](https://www.lex.bg/bg/laws/ldoc/1594373121)
- Government holiday overview: [Council of Ministers of Bulgaria](https://government.bg/en/About-Bulgaria/BULGARIAN-PUBLIC-HOLIDAYS)
- Orthodox Easter and Name Day calendar: [Bulgarian Orthodox Church](https://bg-patriarshia.bg/calendar)
- Father's Day: [State Agency for Child Protection](https://sacp.government.bg/en/node/1414)

The Name Day pack is a curated common-name set for 1.0.1, not a claim to include every regional or parish tradition. Users choose which suggested matches to follow.

## Calculation rules

The app calculates recurring dates rather than hard-coding one year:

- Gregorian Easter and relative dates
- Orthodox Easter and relative dates
- UK Mothering Sunday
- Third-Sunday Father's Day
- Nth weekday and last weekday rules
- UK Christmas/Boxing Day and weekday substitution rules

Sources were reviewed on June 8, 2026. Country packs should be reviewed before each annual data release.
