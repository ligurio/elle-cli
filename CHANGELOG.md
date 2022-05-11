# Change Log

All notable changes to this project will be documented in this file. This
change log follows the conventions of
[keepachangelog.com](https://keepachangelog.com/).

## [Unreleased]

[Unreleased]: https://github.com/ligurio/elle-cli/compare/0.1.3...HEAD

### Added

### Fixed

### Changed

## [0.1.3] - 2022-05-11

[0.1.3]: https://github.com/ligurio/elle-cli/compare/0.1.2...0.1.3

### Fixed

- Fixed defaults for anomalies in a README.
- Fixed processing of Elle's list-append histories (#30).

### Changed

- Checker names have been removed in models name. (#38)
- Model "register" is planned to be removed in next releases. It is removed in
  documentation and usage and it is recommended to use "rw-register" instead. (#42)

## [0.1.2] - 2022-02-23

[0.1.2]: https://github.com/ligurio/elle-cli/compare/0.1.1...0.1.2

### Fixed

- Fix --cycle-search-timeout, --plot-timeout, and --max-plot-bytes CLI arguments
- Fix default value for --directory CLI argument

### Changed

## [0.1.1] - 2022-02-11

[0.1.1]: https://github.com/ligurio/elle-cli/compare/0.1.0...0.1.1

### Added

- Add an example of rw-register history in JSON and EDN formats.
- Add a test script that runs elle-cli against histories.

### Fixed

- Fixes by passing consistency models and anomalies via CLI (#4).
- Fix --plot-format CLI argument.
- Converts the first argument of :value vectors to keyword (#1).

### Changed

- Allow passing an empty consistency model.
- Update default values for CLI arguments so that they are aligned with Elle's.
- Bump Jepsen version to 0.2.6.
- Bump Elle version to 0.1.4.
- Use :strict-serializable as a default consistency model.
- Fix link for unreleased changes in changelog.
- Fix publishing workflow.
- Bump Knossos version to 0.3.8.
- Bump Elle version to 0.1.3.

## [0.1.0] - 2021-12-25

### Added

- Add integration with Elle (list-append and rw-register checkers).
- Add integration with Jepsen (bank, counter, long-fork, set and set-full checkers).
- Add integration with Knossos (register, cas-register and mutex checkers).
- Support histories in EDN and JSON formats.

[0.1.0]: https://github.com/ligurio/elle-cli/compare/dd0c1874...0.1.0
