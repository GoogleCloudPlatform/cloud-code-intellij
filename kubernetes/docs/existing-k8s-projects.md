# Setup existing Kubernetes projects with the plugin

You can open any existing project already configured with Kubernetes manifests and a Dockerfile (or [Jib](https://github.com/GoogleContainerTools/jib)), and use it with the plugin. The only additional bit of configuration is a Skaffold YAML file that you can create from a provided template. Here is a quick tutorial:
* Create new file named `skaffold.yaml` in the root directory of your project (right-click -> `New` -> `File`).
* Type `skaffold` and accept proposed Skaffold live template:

![create Skaffold YAML from template](images/skaffold-yaml-template.gif)
* Populate `image` field with your project image name and `manifests` field with a list of your Kubernetes resources you'd like to be deployed from the IDE. 
   * Example for Dockerfile based builds:
   ```
   build:
     artifacts:
       - image: gcr.io/gcp_project_id/image_name
   deploy:
     kubectl:
       manifests:
         - k8s/web.yaml
         - k8s/backend.yaml
   ```
   * Example `build` section for Java Maven/Gradle projects with the [Jib plugin](https://github.com/GoogleContainerTools/jib) (`deploy` section stays the same):
   ```
   build:
     artifacts:
     - image: gcr.io/gcp_project_id/image_name
       jibMaven: {}
   ```
  
* Once `skaffold.yaml` is valid, the plugin will prompt you to create Kubernetes run targets automatically:
 
<img src="images/k8s-skaffold-notification.png" alt="Kubernetes with Skaffold notification" width="400"/>