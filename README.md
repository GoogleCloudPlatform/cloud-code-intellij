[![experimental](http://badges.github.io/stability-badges/dist/experimental.svg)](http://github.com/badges/stability-badges)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/container-tools-ubuntu-master-orb.svg)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/container-tools-windows-master-orb.svg)
![Build Status](https://storage.googleapis.com/cloud-tools-for-java-kokoro-build-badges/container-tools-macos-master-orb.svg)

# Google Container Tools for IntelliJ

This plugin adds support for [Kubernetes](https://www.kubernetes.io) development into the [JetBrains family of IDEs](https://www.jetbrains.com/products.html), including IntelliJ (both Community and Ultimate editions), GoLand, PyCharm, WebStorm (and others).

**Note**: This plugin is still in development.

## Features

* One click deployment to Kubernetes clusters right from your IDE using [Skaffold](https://skaffold.dev/docs/getting-started/). Configure Skaffold to use your desired build and deployment strategies: works with kubectl, Helm, Google Cloud Build (for remote builds), Jib and Kanico.
* Continuous development on Kubernetes. Watches the dependencies of your docker image or Jib Java project for changes, so that on any change, Skaffold builds and deploys your application to a Kubernetes cluster.
* Automatic discovery and support for project with existing Skaffold configuration, in any language supported by your preferred JetBrains IDE.
* Initial support for Skaffold configuration file editing and smart templates.

## Prerequisites and required dependencies

This plugin uses familiar Kubernetes and container tools to bring you a rich Kubernetes experience in IntelliJ and other JetBrains IDEs. At the minimum, the following tools are expected to be installed and setup on your system and available in the system path:

* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) for working with Kubernetes clusters and managing Kubernetes deployments.
* [Skaffold](https://skaffold.dev/docs/getting-started/) to support continuous development on a Kubernetes cluster, smart image building and tagging, and an array of supported deployment and build types.
* [Docker](https://www.docker.com/) for building and pushing your container images. *Note*: Docker is optional if you are using [Jib to build your container images](https://github.com/GoogleContainerTools/jib).
* Configured Kubernetes cluster. It could be a cluster for local development, such as [Minikube](https://kubernetes.io/docs/setup/minikube/) or [Docker Kubernetes](https://docs.docker.com/docker-for-mac/kubernetes/) cluster, or remote cluster, such as [Google Kubernetes Engine](https://cloud.google.com/kubernetes-engine/) cluster. We recommend [Minikube](https://kubernetes.io/docs/setup/minikube/) cluster for local development.

## Installing the plugin into your IDE

(coming soon)

## Getting started

(coming soon)
