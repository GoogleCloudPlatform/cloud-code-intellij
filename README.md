![Google Cloud Platform Logo](https://cloud.google.com/_static/images/gcp-logo.png)
# Google Cloud Platform IntelliJ IDEA plugin 
[![Build Status](https://travis-ci.org/GoogleCloudPlatform/gcloud-intellij.svg?branch=master)](https://travis-ci.org/GoogleCloudPlatform/gcloud-intellij)

**This plugin is BETA quality.**

The plugin integrates the [Google Cloud Platform](https://cloud.google.com/)
into the IntelliJ IDEA UI. Currently this includes:

* [Google Cloud Source Repositories](https://cloud.google.com/tools/cloud-repositories/) 
  Fully-featured private Git repositories hosted on Google Cloud Platform.
* The [Google Cloud Debugger](https://cloud.google.com/tools/cloud-debugger/) 
  The Cloud Debugger can inspect the state of a Java application running on 
  [Google App Engine](https://cloud.google.com/appengine/)
  at any code location without stopping the application.
* [Google App Engine](https://cloud.google.com/appengine/docs/) deployment via the Cloud SDK.


## Supported Platforms

* IntelliJ IDEA Community Edition 15.0.6 or later
* IntelliJ IDEA Ultimate Edition 15.0.6 or later

## Installation

You can find our plugin in the Jetbrains plugin repository by going to IntelliJ -> Settings -> Browse Repositories, and search for 'Google Cloud Tools'. 
You will see a prompt to install the 'Google Account' plugin which provides common Google settings to the plugin.

### Pre-releases 

The pre-release binaries are being deployed to the Jetbrains plugin repository on an alpha
channel. To install them please perform the following steps:

1. Install the Google Account plugin
    1. Copy this URL `https://plugins.jetbrains.com/plugins/alpha/8078`
    1. Use the copied URL as the Custom Plugin URL when following [these instrucions](https://www.jetbrains.com/idea/help/managing-enterprise-plugin-repositories.html)
    1. Search for the 'Google Account' plugin in the plugin manager and install it.
1. Install the Google Cloud Tools plugin
    1. Use the same steps as step 1 but use the following URL `https://plugins.jetbrains.com/plugins/alpha/8079`
    1. When installing look for the 'Google Cloud Tools' plugin.

If you wish to build this plugin from source, please see the
[contributor instructions](https://github.com/GoogleCloudPlatform/gcloud-intellij/blob/master/CONTRIBUTING.md).

## FAQ


**None yet**
