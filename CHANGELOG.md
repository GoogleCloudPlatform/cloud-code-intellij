# Change Log
All notable changes to this project will be documented in this file.
## [unreleased]

## [16.10.5]

### Fixed
- Fixed issue with local development server debug mode when changes are made while the server is running. ([#972](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/972))

## [16.10.1]
- Note we are changing the versioning scheme to YY.MM.i. We plan on a monthly release cadence to
minimize the disruption of updates. Also notice we have dropped the 'Beta' label. 
- BE AWARE: The local App Engine development server is broken with the latest JDK 8 releases. 
([#920](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/920)).
This should be fixed with the next App Engine SDK release coming soon.

### Added
- App Engine Standard Library importer in the Facet and Project wizard.
([#866](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/866))
- Standard App Engine apps using Java 8 language level will be notified to use language level 7
([#966](https://github.com/GoogleCloudPlatform/google-cloud-intellij/pull/966))

### Changed
- Updated run config labels and icons. (Cloud Debugger is now Stackdriver Debug)
([#936](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/936))

### Fixed
- Local Development server debug mode is fixed.
([#928](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/928))
- Flex deployment broken on Windows 10.
([#937](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/937))
- Cloud Debugger object inspector working again.
([#929](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/929))
- Cloud Debugger snapshot timestamps causing NPE
([#919](https://github.com/GoogleCloudPlatform/google-cloud-intellij/pull/919))

## [1.0-beta] - 2016-09-14
### Added
- App Engine standard environment support ([#767](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/767))
- Extra fields now available in the deployment config ([#868](https://github.com/GoogleCloudPlatform/google-cloud-intellij/pull/868))

## [0.9.7.5-beta] - 2016-08-29
### Added
- Check to ensure that deployment is valid for credentialed user with prompt to add a new user if not.
([837](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/837))

## [0.9.6-beta] - 2016-06-23
### Added
- Deploy to App Engine flexible _compat_ environment support.
([#720](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/720))
- Deploy to App Engine standard environment support.
([#665](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/665))
- Check for Cloud Tools and Account plugin compatibility.
([#651](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/650))

### Changed
- Moved version input to be a top level configuration within the deployment configuration dialog.
([#639](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/639))

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
