## Frequently Asked Questions (FAQ)

If you have a question that is not answered below, please [submit an issue](https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues).

[How do I fix missing Skaffold installation errors?](#how-do-i-fix-missing-skaffold-installation-errors)\
[How do I fix 'executable not found on PATH' errors?](#how-do-i-fix-executable-not-found-on-path-errors)

### How do I fix missing Skaffold installation errors? 

[Skaffold](https://skaffold.dev/) is a tool required by Cloud Tools plugin for Kubernetes 
deployment and continuous development. The plugin attempts to find the Skaffold binary on the system 
PATH. If you don't have Skaffold installed, or the plugin simply could not detect the installation 
(possibly due to [PATH issues](#how-do-i-fix-executable-not-found-on-path-errors)), you may see the 
follow error:

<img src="images/missing-skaffold-warning.png" alt="missing-skaffold-warning" width="700"/>

To fix this, either
1) [Install Skaffold](https://skaffold.dev/docs/getting-started/#installing-skaffold) and ensure that it is added to the PATH.
2) If Skaffold is installed and it is still not detected, visit `Settings > Google > Kubernetes`, 
and manually browse to the executable:

<img src="images/skaffold-manual-select.png" alt="skaffold-manual-select" width="700"/>

### How do I fix 'executable not found on PATH' errors?
