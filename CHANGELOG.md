# Release notes
This page documents production updates to Cloud Tools for IntelliJ. You can check this page for announcements about new or updated features, bug fixes, known issues, and deprecated functionality.

## 17.9.1

### Added
  - Added the ability to change the name of the staged artifact for App Engine flexible deployments.
    [1610](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1610)

### Changed
  - App Engine flexible deployment configurations now default to deploy the artifact as-is, without
    renaming to `target.jar` or `target.war`. [1151](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1151)
  - Updated the name of the placeholder artifact name in the generated Dockerfile templates to make 
    it clearer that it needs to be updated by the user. [1648](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1648) 
  - App Engine standard deployment configurations now default to update dos, dispatch, cron, queues,
    and datastore indexes. [1613](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1613)
  - Native projects that add support for Cloud Endpoints Frameworks for App Engine will now use
    Endpoints V2. [1612](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1612)

### Fixed
  - Fixed the `Deployment source not found` error when deploying Maven artifacts.
    [1220](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1220)
  - Fixed the scale of the user icon on HiDPI displays.
    [1633](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1633)
  - Fixed an issue where the plugin was downgraded on the IDEA 2017.3 EAP.
    [1631](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1631)

## 17.8.2 

### Fixed
  - Fixed "Error: invalid_scope" issue when logging in with your Google Account. [1598](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1598)

## 17.8.1

### Added
  - Added a feedback & issue reporting link to the Google Cloud Tools shortcut menu. [1560](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1560)

### Changed 
  - Users can now save deployment run configurations that are partially completed or in an error state. [1407](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1407)

### Fixed
  - Fixed registered Docker language conflict causing issues running plugin alongside .ignore plugin. [1535](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1535)
  - Fixed NPE parsing Stackdriver Debugger breakpoint timestamps. [1537](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1537)
  - Removed EAR as acceptable App Engine artifact type for local dev server runs. [1190](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1190)
  - Deployments are now displayed across multiple IDE windows. [1432](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1432)
  - Fixed crash caused by attempting to modify a read-only collection. [1571](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1571)

## 17.6.2

### Fixed
  - Fixed NPE occurring when there is a local dev server configuration but no standard facet. [1525](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1525)

## 17.6.1

### Added
  - App Engine flexible facet with app.yaml and Dockerfile configuration. [1514](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1514)
  - App Engine flexible framework support detection. [1277](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1277)
  
### Changed 
  - Allow user to specify a Docker directory instead of just a Dockerfile for flexible deployments. [1304](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1304)
  - Refresh the user experience of the deployment dialog (both standard and flexible). [1477](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1477)
  
### Fixed
  - Fixed Google avatar size for HiDPI displays. [1391](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1391)

## 17.2.5_2017

### Added
  - Environment variables in the App Engine standard local run configuration are now passed in to the dev server. [#1364](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1364)
  - Environment variables configured in appengine-web.xml are now honored and passed in to the dev server. [#377](https://github.com/GoogleCloudPlatform/appengine-plugins-core/issues/377)

## 17.2.4_2017

### Added
  - Added a checkbox to deploy all App Engine config files during service deployment. [#1346](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1346)

## 17.2.3_2017

### Changed
  - Removed the Clear Datastore flag from the App Engine standard local development server configuration since the current version of the server doesn't support it. ([#1345](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1345))

## 17.2.2_2017

### Fixed
  - Invalid Java Runtime Environment (JRE) on 
staging an App Engine standard app ([#1316](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1316)):

        > Unable to stage app: Cannot get the System Java Compiler. Please use a JDK, not a JRE.

## 17.2.1

Happy New Year Cloud Tools for IntelliJ users! This year's first release is primarily a maintenance
release. If you are having authentication problems using Cloud Source Repositories and our 
plugin, check out [this possible solution](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1174).

Here is a list of the visible changes:

### Added
  - Support for multiple cloud source repositories for a single GCP project. ([#1024](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1024))
  - App Engine initialization and region selection. ([#1232](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1232))
  
### Fixed
  - Stopping dev_appserver on Windows always fails with com.intellij.execution.ExecutionException. ([#1215](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1215))
  - New AE standard project wizard should generate web.xml with servlet 2.5. ([#1194](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1194))
  - Clear datastore checkbox for app engine standard local server does not work. ([#1188](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1188))
  - Don't show projects scheduled for deletion in the project selector. ([#1119](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1119))
  
Visit our [17.2 Release Milestone](https://github.com/GoogleCloudPlatform/google-cloud-intellij/milestone/19?closed=1) for complete details.

## 16.11.6

### Added
- Expanded Google Cloud Tools menu item with various action shortcuts. ([#1061](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1061)).
- Check for minimum support Cloud SDK version. ([#1051](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1051)).
- Automatically create all relevant run configuration for App Engine Standard apps. ([#1063](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1063)).
- App Engine framework is now a child of Web framework in the new project wizard. ([#1065](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1065)).

### Fixed
- Unique deployment sources in application server deployment panel now appear as separate line items. ([#821](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/821)).
- Validation of invalid Cloud SDK paths on Windows. ([#1091](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1091)). 

## 16.10.5

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

## 16.10.1
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

## 1.0-beta - 2016-09-14
### Added
- App Engine standard environment support ([#767](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/767))
- Extra fields now available in the deployment config ([#868](https://github.com/GoogleCloudPlatform/google-cloud-intellij/pull/868))

## 0.9.7.5-beta - 2016-08-29
### Added
- Check to ensure that deployment is valid for credentialed user with prompt to add a new user if not.
([837](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/837))

## 0.9.6-beta - 2016-06-23
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

## 0.9.4-beta - 2016-04-20
### Added
- Deploy to App Engine flexible environment tools menu item. ([#635](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/635))
- Support for Maven based projects as deployment sources for App Engine flexible environment deployments ([#600](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/600))

### Changed
- App Engine flexible environment deployment can be cancelled by disconnecting to our App Engine application server. ([#581](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/581))
- App Engine flexible environment generated Dockerfile and app.yaml now default to the recommended location in a Maven structured Java project. ([#575](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/575))

### Fixed
- Login bug that could result in no active user being selected when adding a user. ([#644](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/644))
- Undeploying an App Engine deployment could cause an error. ([#599](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/599))
