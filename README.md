# Cloud Tools for IntelliJ plugin

![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-ubuntu-master-orb.svg)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-windows-master-orb.svg)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-macos-master-orb.svg)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-ubuntu-master-previous-version-orb.svg)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/intellij-ubuntu-master-eap-orb.svg)

Cloud Tools for IntelliJ is a plugin that helps facilitates cloud development in the JetBrains 
family of IDEs. This includes the following areas:

* <img src="kubernetes/docs/images/kubernetes.png" alt="kubernetes" width="20" /> [Kubernetes](https://github.com/GoogleCloudPlatform/google-cloud-intellij/tree/master/kubernetes)
* Google Cloud Platform
  * [Google Cloud Source Repositories](https://cloud.google.com/tools/cloud-repositories/) 
    Fully-featured private Git repositories hosted on Google Cloud Platform.
  * The [Google Cloud Debugger](https://cloud.google.com/tools/cloud-debugger/) 
    The Cloud Debugger can inspect the state of a Java application running on 
    [Google App Engine](https://cloud.google.com/appengine/)
    at any code location without stopping the application.
  * [Google App Engine](https://cloud.google.com/appengine/docs/) deployment via the Cloud SDK.
  * [Google Cloud Storage](https://cloud.google.com/storage/) 
    Browse your Google Cloud Storage buckets.
  * [Google Cloud Java Client Libraries](https://cloud.google.com/tools/intellij/docs/client-libraries) 
    Add Java client libraries to your project, enable Google Cloud APIs, and create service accounts.

    (_For detailed user documentation on GCP features, visit our documentation_
 [website](https://cloud.google.com/tools/intellij/docs/?utm_source=github&utm_medium=google-cloud-intellij&utm_campaign=ToolsforIntelliJ)).

## Supported Platforms

The Cloud Tools for IntelliJ plugin supports the entire JetBrains family of IDEs, versions 2018.2 or 
later for Kubernetes support, and 2017.2 for everything else. Both the free and paid editions of the
IDEs are supported. 

For GCP functionality, full support is available for IntelliJ IDEA Ultimate Edition, with limited
support for the other platforms. See this [feature matrix](docs/gcp-feature-matrix.md) 
for more details.

## Installation

You can find our plugin in the Jetbrains plugin repository by going to IntelliJ -> Settings -> Browse Repositories, and search for `Google Cloud Tools`. 

### Pre-releases 

The pre-release binaries are being deployed to the Jetbrains plugin repository on an alpha
channel. To install them please perform the following steps:

1. Install the Google Cloud Tools plugin
    1. Copy this URL `https://plugins.jetbrains.com/plugins/alpha/8079`
    1. Use the copied URL as the Custom Plugin URL when following [these instrucions](https://www.jetbrains.com/idea/help/managing-enterprise-plugin-repositories.html)
    1. Search for the 'Google Cloud Tools' plugin and install it.

You can also grab the latest nightly build of the plugin by following the same steps as above but 
replacing 'alpha' with 'nightly' in the URLs.

If you wish to build this plugin from source, please see the
[contributor instructions](https://github.com/GoogleCloudPlatform/google-cloud-intellij/blob/master/CONTRIBUTING.md).

## FAQ


**None yet**
