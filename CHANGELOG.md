# Change Log

All notable changes to this project will be documented in this file. This
change log follows the conventions of
[keepachangelog.com](https://keepachangelog.com/).

## [Unreleased]

[Unreleased]: https://github.com/ligurio/elle-cli/compare/0.1.8...HEAD

### Changed

- Bump Elle version to 0.2.4.
- Bump Knossos version to 0.3.12.

## [0.1.8] - 2024-11-05

[0.1.8]: https://github.com/ligurio/elle-cli/compare/0.1.7...0.1.8

### Added

- An option `--allow-negative-balances`.

### Fixed

- A `set` checker (#44).
- A `long-fork` checker (#43).

### Changed

- Bump Elle version to 0.2.1.
- Bump Jepsen version to 0.3.7.

### Removed

- Model names with prefixes `jepsen-` and `knossos-`.

## [0.1.7]

[0.1.7]: https://github.com/ligurio/elle-cli/compare/0.1.6...0.1.7

### Added

- Shell script that runs a JAR file.

### Changed

- Bump Elle version to 0.1.7.
- Bump Jepsen version to 0.3.3.
- Bump Jepsen version to 0.3.2.
- Bump Knossos version to 0.3.9.

## [0.1.6]

[0.1.6]: https://github.com/ligurio/elle-cli/compare/0.1.5...0.1.6

### Changed

- Bump Elle version to 0.1.6.
- Bump Jepsen version to 0.3.0.

## [0.1.5] - 2022-12-06

[0.1.5]: https://github.com/ligurio/elle-cli/compare/0.1.4...0.1.5

### Added

- Add a checker for comments test (#32).
- Add a checker for sequential test (#33).

### Changed

- Knossos register model is removed. Use Elle's register instead. (#42).

## [0.1.4] - 2022-06-30

[0.1.4]: https://github.com/ligurio/elle-cli/compare/0.1.3...0.1.4

### Fixed

- Fix setting of headless mode (#50).
- Fix exit code when history is not valid (#53).

### Changed

- Updated test descriptions in a README. (#37)
- Bump Elle version to 0.1.5.

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
