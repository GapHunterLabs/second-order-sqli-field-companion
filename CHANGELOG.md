<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Second-Order SQLi Field Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Real field-sensitive, two-phase, whole-project points-to analysis:
  catalogs `@Entity` String fields, correlates a write site (tainted
  HTTP input stored into a field) with a read site (that same field
  concatenated into a SQL sink) anywhere in the project, even across
  completely unrelated classes -- second-order SQL injection (CWE-89).

[Unreleased]: https://github.com/GapHunterLabs/second-order-sqli-field-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/second-order-sqli-field-companion/commits/0.1.0
