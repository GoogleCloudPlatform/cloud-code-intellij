## Frequently Asked Questions (FAQ)

If you have a question that is not answered below, please [submit an issue](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues).

[Could not find Skaffold installation.](#could-not-find-skaffold-installation)\
[Blah.](#gcr-authentication-path-missing)

### Could not find Skaffold installation

[Skaffold](https://skaffold.dev/) is a tool required by Cloud Tools plugin for Kubernetes 
deployment and continuous development. The plugin attempts to find the Skaffold binary on the system 
PATH. If you don't have Skaffold installed, or the plugin simply could not detect the installation 
(TODO link to more general PATH issue), you may see the follow error:

<img src="images/missing-skaffold-warning.png" alt="missing-skaffold-warning" width="700"/>

To fix this, either
1) [Install Skaffold](https://skaffold.dev/docs/getting-started/#installing-skaffold) and ensure that it is added to the PATH.
2) If Skaffold is installed and it is still not detected, visit `Settings > Google > Kubernetes`, 
and manually browse to the executable:

<img src="images/skaffold-manual-select.png" alt="skaffold-manual-select" width="700"/>

