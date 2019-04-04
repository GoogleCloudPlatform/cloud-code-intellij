# Getting started with a Kubernetes deployment using IntelliJ, Java and Spring Boot

* [Set up](#set-up)
* [Specifying image repository](#specifying-image-repository)
* [Continuous development on Kubernetes](#continuous-development-on-kubernetes)
* [Adding new features](#adding-new-features)
* [Deployment to Kubernetes](#deployment-to-kubernetes)

### Set up

The plugin works in any of the  [JetBrains family of IDEs](https://www.jetbrains.com/products.html). The following shows an example using Kubernetes with Java and Spring Boot in IntelliJ IDEA (Community or Ultimate editions). Follow the [installation steps](../README.md#installing-the-plugin-into-your-ide) to install the plugin. Restart your IDE if prompted to activate the plugin.

Before we start, make sure [all required dependencies](../README.md#prerequisites-and-required-dependencies) are available on your machine.

Clone the repository to your local machine to get your copy of the repository and switch to the example project directory:
```
git clone https://github.com/GoogleCloudPlatform/google-cloud-intellij.git
cd kubernetes/examples/hello-spring-boot
```

Open the `hello-spring-boot` example project in your IDE. The project opens and loads:

<img src="images/sb-hello-world-project.png" alt="opened Spring Boot hello world project" width="500"/> 

This project is a simple web application created with [the popular Spring Boot framework](https://spring.io/projects/spring-boot). It uses the [Jib Maven plugin](https://github.com/GoogleContainerTools/jib) to build a container image for the project, without needing to create a Dockerfile.

Once the project loads, the plugin will detect the Skaffold configuration and prompt to create the Kubernetes targets. The notification shows (if it disappears, you can find it in the Event Log in the bottom right of the IDE):

<img src="images/k8s-skaffold-notification.png" alt="Kubernetes with Skaffold notification" width="400"/> 

Click `Create run configurations for Kubernetes with Skaffold` link to automatically create Kubernetes deployment and continuous development IDE run targets for the project:

<img src="images/k8s-skaffold-run-targets.png" alt="Kubernetes with Skaffold pre-configured run targets" width="400"/> 

Now the new run targets can be used to build the project and deploy it to Kubernetes or develop on Kubernetes cluster continuously. With continuous development, the plugin uses Skaffold to watch the sources and dependencies of your project for changes, so that on any change, Skaffold builds and deploys your application to a Kubernetes cluster.

### Specifying image repository

However, before we can deploy and develop, we need to make sure we have access to the image repository where the project image is about to be pushed. By default the project is configured to use [Google Container Registry](https://cloud.google.com/container-registry/) and a development project for the plugin which you probably don’t have access to. Once you have your repository set up ([Google Container Registry](https://cloud.google.com/container-registry/), [DockerHub](https://hub.docker.com/), private repository, etc.), you can edit the run targets and specify it as a *default image repository* in run target settings:

![specify your repository in run target settings](images/default-image-repo-settings.png)

Here are examples of how to specify the default image repository for some common registries:

* Docker Hub: `docker.io/{account}`
* GCP Container Repository (GCR): `gcr.io/{project_id}`
* AWS Container Repository (ECR): `{aws_account_id}.dkr.ecr.{region}.amazonaws.com/{my-app}`
* Azure Container Registry (ACR): `{my_acr_name}.azurecr.io/{my-app}`

The resulting image name is concatenated from the specified default image repository and the image name from the project Kubernetes resources. For this `hello-spring-boot` example, and GCR image repository as the default one, the resulting full image name would be `gcr.io/{project_id}/gcr.io/gcp-dev-tools/hello-spring-boot`. 

*Note*: this step is not required when you work with your own Kubernetes manifests and Skaffold configuration where you specify a repository and an image name that are accessible to you.

### Continuous development on Kubernetes

Now you can set up a continuous development iteration cycle in your IDE. Click the run action for `Develop on Kubernetes` to start development cycle on your Kubernetes cluster:

<img src="images/k8s-develop-run-icon.png" alt="run target click" width="400"/> 

The development cycle initiates and console window with the logs opens. The plugin uses Skaffold to build an image for the project, tag it, push it to the configured repository, and then uses `kubectl` to deploy the project Kubernetes manifests:

![develop on Kubernetes console window](images/console-skaffold-build.gif)

Once the build completes, the image is pushed and deployment starts, the console begins to stream logs from your Kubernetes deployment:

![Spring Boot logs from Kubernetes deployment](images/console-spring-boot-init.gif)

As you can see, Spring Boot application initializes and launches built-in web server. Be default, Spring Boot web server uses port 8080 to serve the content. The plugin makes sure you don’t have to worry about accessing the deployment via remote addresses - all declared container ports are port-forwarded automatically!

![automatic port-forwarding](images/auto-port-forward-hello-world.png)

Navigate your browser to [localhost:8080](http://localhost:8080) to access the Spring Boot application running on your Kubernetes cluster. Alternatively, use `curl` command to interact with the application:

<img src="images/browser-root.png" alt="browser showing root page of the application" width="350"/> 

```
$ curl localhost:8080
Hello, World of Kubernetes with IntelliJ!
```

You can check the details of the Kubernetes deployment and service using standard Kubernetes CLI commands (`kubectl get deploy`, etc.) or using Kubernetes dashboard for your Kubernetes cluster. The Kubernetes resources for the project are located in the `k8s` directory - there is one deployment and one service YAML file.

#### Adding new features

Now, let’s add more features to our Spring Boot project and see how they get deployed to your Kubernetes cluster without stopping and removing the deployment, manually building and tagging the image, or updating the cluster. Open `HelloController.java` file from `src` and add a new HTTP request mapping:

```java
   @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return String.format("Hello from Kubernetes with IntelliJ, %s!", name);
    }
```

Save the changes (`Ctrl-S`) or build the project (use `Build -> Build Project` menu or the toolbar icon). The plugin picks up the changes, re-builds the project and image, and deploys the updated image to your Kubernetes cluster. You can watch the progress and deployment logs in the console window. Once the changes are propagated, we can confirm the updates by visiting the newly created endpoint at [localhost:8080/greeting?name=User](http://localhost:8080/greeting?name=User):

![browser showing new greeting page of the application](images/browser-greeting.png)

```
$ curl localhost:8080/greeting?name=User
Hello from Kubernetes with IntelliJ, User!
```

You can continue adding and testing new features and have them redeployed automatically to your Kubernetes cluster from your IDE on every change. Once you are finished, click `stop` to end the continuous development session. The plugin deletes all Kubernetes resources used for the development session.

### Deployment to Kubernetes

You can use the other Kubernetes run target to build the image and deploy the project to your Kubernetes cluster once. Unlike continuous development, your project sources and dependencies are not watched, and the Skaffold process finishes once the image and deployment are complete.

<img src="images/deploy-k8s-action.png" alt="deploy run target click" width="400"/>
 
