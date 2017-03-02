#New Projects
To create a new IDEA project with App Engine Flexible support by default, go to File -> New -> Project… and check the “Google App Engine Flexible” check box from the Additional Libraries and Frameworks pane.

![](https://googlecloudplatform.github.io/google-cloud-intellij/images/flexible/new-project.png)

_Warning_: Due to the way the New Project/Module is designed, it is possible to create a project or module with both Google App Engine Flexible and Google App Engine Standard options selected. If a project or module is created with both check boxes checked, App Engine support will be Standard only.

#Imported projects or modules
App Engine Flexible support needs to be explicitly added to an existing module. No automatic framework detection exists for Flexible like there is for Standard, but this will be addressed soon.

In the meantime, you can add Flexible support to an existing module by right-clicking the module name, selecting Add Framework Support and checking the Google App Engine Flexible option, on the left-hand side menu, then pressing OK.

![](https://googlecloudplatform.github.io/google-cloud-intellij/images/flexible/add-framework.png)

#App Engine Flexible configuration
After Flexible support is added to a module, the application is configurable through the Google App Engine Flexible facet. The facet is available through the Project Structure -> Facets menu.

![](https://googlecloudplatform.github.io/google-cloud-intellij/images/flexible/facet.png)

From the Google App Engine Flexible facet, it is possible to configure the location of the app.yaml and Dockerfile configuration files.

If the files don’t exist, it is also possible to generate them from there, or choose other existing ones from the file system.

#Deployment
Adding Flexible support to a module automatically creates a Flexible deployment run configuration.

![](https://googlecloudplatform.github.io/google-cloud-intellij/images/flexible/runconfig-outside.png)

Running the run configuration deploys your local artifact to a version on Google App Engine. However, the created run configuration has to be configured before being run.

![](https://googlecloudplatform.github.io/google-cloud-intellij/images/flexible/runconfig-inside.png)

Following is an explanation of the customizable fields in the App Engine Flexible Deployment run configuration:
* Server: Application server that runs the app.
  * Google App Engine is the only option. A new application server instance has to be created if none exists.
* Deployment: Artifact to be deployed. 
  * The _Filesystem JAR or WAR file_ option should always appear. In this case, a Deployment Archive (JAR or WAR file) must also be selected from the file system.
  * _[moduleName]:war_ appears for Web-supported modules. IDEA’s build system is used to build these artifacts. It is possible, under certain circumstances, for a Web-supported module to have no such artifacts, in which case they can be manually created from Project Structure -> Facets or Project Structure -> Artifacts.
  * _Maven build: [moduleName]_ appears for Maven-supported modules. Maven is used to build these artifacts.
* Service: The App Engine service to which this version will be deployed to.
  * This value is read from the app.yaml file.
* Version: Google App Engine version to deploy to.
* Promote version: If checked, version becomes the default version, with all production traffic migrated to it by default.
* Stop previous version: Stops the previous default version, turning off all instances.
* app.yaml from module: Selects the app.yaml file from the Flexible facet of a module in the project.
  * If no module has Flexible support, an app.yaml file has to be selected from the file system.
* Module Settings…: Allows setting an app.yaml and Dockerfile for the selected module.
* Use a different app.yaml file: Allows the selection of another app.yaml file from the file system.
* Dockerfile: Dockerfile to be used for this deployment.
  * Only visible if runtime is set to “custom” in the selected app.yaml.
  * Default value is selected from the Flexible facet configuration of the selected module in the modules combo box.
* Use a different Dockerfile: Allows the selection of another Dockerfile file from the file system.

In order to be runnable, a run configuration cannot contain any errors. Trying to save a run configuration with errors will result in an error.

After a run configuration is successfully saved, it can be run by selecting it from the run configurations combo box and pressing the green play button on the right.
