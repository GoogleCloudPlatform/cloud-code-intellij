---
title: Gcloud IntelliJ Tutorial
---

# Gcloud IntelliJ IDEA Tutorial

**<span style="color:red">Warning: Work in Progress. Definitely incomplete and sometimes incorrect.</span>**

You can use the Google Cloud Debugger to capture and inspect the call stack 
and local variables of a live application running in the cloud
(more specifically App Engine or the Google Compute Engine.)
 
It works much like the IntelliJ IDEA debugger you're already used to,
and provides the same user interface, with two key differences:

* The cloud debugger does not pause execution of the running application.

* You cannot step through an application in the cloud debugger.

In other words, the Cloud Debugger is a *forensic* debugger, not an *interactive* debugger.

The Cloud Debugger is enabled automatically for Java 
applications running on [Google App Engine](https://cloud.google.com/appengine/docs) and
[Managed VMs](https://cloud.google.com/appengine/docs/managed-vms/).

## Installing the Plugins

*<span style="color:red">Note: these instructions don't work yet. They will soon. Meanwhile you'll need to follow [these instrucitons](https://github.com/GoogleCloudPlatform/gcloud-intellij/blob/master/README.md#installation).</span>* 

The plugin can be installed from the 
[IntelliJ IDEA Plugin Repository](https://www.jetbrains.com/idea/plugins/).

From inside IDEA:

1. File > Settings

2. In the left-hand pane, select Plugins.

3. Click the "Browse repositories..." button.

4. In the dialog that opens, select "Google Core Plugin" (exact name TBD????). 

5. Click the Green Install button.

6. Click Close.

7. Click OK in the Settings dialog.

8. Click Restart (or you can click Postpone, but the plugins will not be available until you do restart IDEA.)

 ![](images/restartintellij.png)



## Setting up the app:

For purposes of this tutorial we'll debug a simple servlet that detects the
user's browser and says "Hello Firefox", "Hello Chrome", and so forth, depending on the browser.
You can find this code in the Github project ????. It is built with Maven version 3.1 or later. 

(If you happen to spot the bug by eye before running it, pretend you don't and just read along.)

You can clone the project to your own repository.

*TBD: can someone run the debugger without being able to commit to the repository?*


1.  Commit and push the source code of the application to a Git repository (a
    [Cloud Source Repository](https://cloud.google.com/tools/cloud-repositories/docs/) or connected
    Github or Bitbucket repository). 

2. Register your project on the [Google Developer's Console](https://console.developers.google.com/). You'll need to pick a project name. In this tutorial, I use hellobrowser, but you'll need to choose something else since that's now taken.

3. In your local copy of the source, open the file pom.xml in a text editor and change 
   the `artifactId` element from `hellobrowser` to the project name you registered in the developer console.

4. Build and test the application using`mvn clean install`. Note that all unit tests pass. (And if you're feeling really ambitious, check the code coverage.)

5.  Deploy your application using
    [maven](https://cloud.google.com/appengine/docs/java/tools/maven#uploading_your_app_to_production_app_engine):

        $ mvn appengine:update


6. Visit the application at http://*projectname*.appspot.com/hellobrowser. You'll see it say:
 
 Aha! That's a bug. It's supposed to say "Hello Firefox" (or "Hello Chrome", etc.) Why doesn't it? Let's debug. 


## Using the Debugger

### Enabling the application for the Cloud Debugger

First you need to connect your Github repository to the Google Cloud Source Repository
so that the Cloud Debugger can see your source code. This happens inside a web browser.


### Setting up a Cloud Debug Configuration

This is done inside IDEA.


1. Set up a new Java project from the Github repository you cloned. (reference IntelliJ instructions for doing this? Anything special to note here?)

2. Run > Edit Configurations...

3. Click the Green + icon on the upper right hand side. Select "Cloud Debug" from the popup menu. (If this option doesn't appear, check whether the plugin is installed and activated.)

4. Set the name of the configuration to "My First Debugging Session."

5. At the right-hand side of the Project popup, click the little arrow. You'll see a sign in pane.

6. Click the "Sign In" button.

7. A browser window will open outside of IntelliJ.

8. (May not happen.) If you have more than one Google account logged into the browser, 
select the one that has access to the application you want to debug. 
If that account is not shown, click "Add Account" and login to the 
account that can manage the application.

9. You'll be asked to grant a list of permissions to IDEA. After waiting a few seconds for the button to activate, click "Allow".

 (Yes, the list of permissions requested is overly long. 
 The Cloud Debugger doesn't actually need or use all of them.
 We're working on fixing that.)

10. You will see a window saying "IntelliJ is now authorized to access your account." Close the browser window and return to IntelliJ.

11. At the right-hand side of the Project popup, click the little arrow again. This time you'll see a list of applications managed by your account. Select "Sample Project".

12. Click the OK button.
 

### Set a breakpoint

Once you've attached  to a running application, you can set breakpoints in the 
source code by clicking at the line you want to snapshot in the left hand bar,
just as you would when debugging a local application using the regular IDEA debugger.

Here set a breakpoint at ????.

Since you can't single step through an application in the cloud debugger,
it's more common to put the breakpoint at the end of the relevant block
of code rather than at the beginning. That way all variables have been set and can
be inspected. 

If you're interested in variables in multiple scopes, (e.g. inside and 
outside a loop) you'll need to set multiple breakpoints.

If you're interested in variables at particular points in time,
(e.g. on the last iteration of a loop) then you'll want to set a 
conditional breakpoint.


### Run the application

1. Run > Debug 'My First Debugging Session'

 If some other configuration happens to appear in the Run menu instead, 
 then select "Debug.." and choose "My First Debugging Session" in the dialog that appears.

2. A dialog will appear. TBD: set the Module? Click the blue Attach button.

3. The debugger pane will appear at the bottom of the screen. 

4. In your web browser, visit the application (e.g. ????) and exercise its functionality 
such that the code at the breakpoint will execute.

5. In IDEA you will see a brief popup saying "New snapshot received." 
(Don't blink or you'll miss it.) The new snapshot appears in the lower lefthand pane. 
Click it.


In the middle frame you'll see the stack trace:

In the right hand frame you can all local variables and method arguments 
in scope at that point in the code and their values at that point in time. 
Fields are also available by expanding the "this" variable. For instance here


Here we can see ????

You can use the right click context menu to copy values, inspect variables, and more. 


Admittedly this is a very simple example. You probably spotted at least one of the two 
bugs immediately. If not, you could have found and debugged them using a locally running instance.
However, in more complex applications that interact with backend data stores and 
other live network services, local tests may not be able to reproduce the exact problems
you see in production. For these scenarios, the cloud debugger is invaluable. 



### Conditional breakpoints


### Watch expressions

### Closing the debugger

When you're finished debugging, click the red square in the upper right corner 
of the debug pane. You'll be asked whether to continue listening for snapshots in the background:

![](images/continuelistening.png)


If you choose to continue listening, then ????.






