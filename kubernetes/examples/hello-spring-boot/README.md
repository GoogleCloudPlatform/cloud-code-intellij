# Spring Boot Hello World with IntelliJ and Kubernetes

This project is a simple web application created with [the popular Spring Boot framework](https://spring.io/projects/spring-boot). It uses the [Jib Maven plugin](https://github.com/GoogleContainerTools/jib) to build a container image for the project, without needing to create a Dockerfile. It defines one HTTP endpoint ("/") and starts built-in Spring Boot web server on port 8080.

The Kubernetes resources for the project are located in the `k8s` directory - there is one deployment and one service YAML file.

Please see ["Getting started"](https://github.com/GoogleContainerTools/google-container-tools-intellij#getting-started) section of the main README on how to configure the Kubernetes run targets and use the plugin features for this project.
