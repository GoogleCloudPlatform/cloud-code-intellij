# Change Log
All notable changes to this project will be documented in this file.
## [unreleased]

## [17.2.1]

Happy new year Cloud Tools for IntelliJ users! This year's first release is primarily a maintenance
release. If you are having authentication problems using Cloud Source Repositories and our 
plugin, check out [this possible solution](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1174).

Here is a list of the visible changes:

### Added
  - Support for multiple cloud source repositories for a single GCP project. ([#1024](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1024))
  - App Engine initialization and region selection. ([#1232](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1232))
  
### Fixed
  - Stopping dev_appserver on Windows always fails with com.intellij.execution
  .ExecutionException. ([#1215](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1215))
  - New AE standard project wizard should generate web.xml with servlet 2.5. ([#1194](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1194))
  - Clear datastore checkbox for app engine standard local server does not work. ([#1188](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1188))
  - Don't show projects scheduled for deletion in the project selector. ([#1119](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1119))
  
Visit our [17.2 Release Milestone](https://github.com/GoogleCloudPlatform/google-cloud-intellij/milestone/19?closed=1) for complete details.

## [16.11.6]

### Added
- Expanded Google Cloud Tools menu item with various action shortcuts. ([#1061](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1061)).
- Check for minimum support Cloud SDK version. ([#1051](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1051)).
- Automatically create all relevant run configuration for App Engine Standard apps. ([#1063](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1063)).
- App Engine framework is now a child of Web framework in the new project wizard. ([#1065](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1065)).

### Fixed
- Unique deployment sources in application server deployment panel now appear as separate line items. ([#821](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/821)).
- Validation of invalid Cloud SDK paths on Windows. ([#1091](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1091)). 

## [16.10.5]

IMPORTANT: 
This plugin requires the use of Cloud SDK v 133.0.0 for correct execution of the local
development server with the latest Java 8 SDK. Please run 'gcloud components update' from your 
shell to ensure you have the latest Cloud SDK release.

### Fixed
- Fixed issue with local development server debug mode when changes are made while the server is 
running. ([#972](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/972))
- Better wording when the development server has an invalid Cloud SDK path. 
([#1043](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1043)).
- Update run configuration names to be prefixed with 'Google ..'
([#1021](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1021)).

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
