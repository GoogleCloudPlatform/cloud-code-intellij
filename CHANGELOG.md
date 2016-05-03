# Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]


## [0.9.4-beta] - 2016-04-20
### Added
- Deploy to App Engine flexible environment tools menu item. ([#635](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/635))
- Support for Maven based projects as deployment sources for App Engine flexible environment deployments ([#600](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/600))

### Changed
- App Engine flexible environment deployment can be cancelled by disconnecting to our App Engine application server. ([#581](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/581))
- App Engine flexible environment generated Dockerfile and app.yaml now default to the recommended location in a Maven structured Java project. ([#575](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/575))

### Fixed
- Login bug that could result in no active user being selected when adding a user. ([#644](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/644))
- Undeploying an App Engine deployment could cause an error. ([#599](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/599))
