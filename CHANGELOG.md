<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Second-Order SQLi Field Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Real field-sensitive, two-phase, whole-project points-to analysis:
  catalogs `@Entity` String fields, correlates a write site (tainted
  HTTP input stored into a field) with a read site (that same field
  concatenated into a SQL sink) anywhere in the project, even across
  completely unrelated classes -- second-order SQL injection (CWE-89).

[Unreleased]: https://github.com/GapHunterLabs/second-order-sqli-field-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/second-order-sqli-field-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/second-order-sqli-field-companion/commits/0.1.0
