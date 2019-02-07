# Example

This directory contains example projects for the [JetBrains family of IDEs](https://www.jetbrains.com/products.html) that demonstrate how to work with the plugin and use it to deploy and develop on Kubernetes.

*Note*: before deploying the application to Kubernetes using these samples, ensure that you update the image path to match your registry and image name. By default it is configured to use the [Google Container Registry](https://cloud.google.com/container-registry/) (`grc.io/gcp-dev-tools` repository). A simple way to do this without updating the image string in all locations is to set a *default image repository* in the Kubernetes Run Configuration.

![specify your repository in run target settings](../docs/images/default-image-repo-settings.png)
