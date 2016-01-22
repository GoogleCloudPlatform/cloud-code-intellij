---
title: IntelliJ IDEA Google Cloud Debugger Tutorial
---

# IntelliJ IDEA Google Cloud Debugger Tutorial

**<span style="color:red">Warning: Work in Progress. Definitely incomplete and sometimes incorrect.</span>**

You can use the Google Cloud Debugger to capture and inspect the call stack 
and local variables of a live application running in the cloud
(more specifically on [Google App Engine](https://cloud.google.com/appengine/docs), [Managed VMs](https://cloud.google.com/appengine/docs/managed-vms/), or the Google Compute Engine.)
 
It works much like the IntelliJ IDEA debugger you're already used to,
and provides the same user interface, with two key differences:

* The cloud debugger does not pause execution of the running application.

* You cannot step through an application in the cloud debugger.

In other words, the Cloud Debugger is a *forensic* debugger, not an *interactive* debugger.

The Cloud Debugger is enabled automatically for Java 
applications running on App Engine and MVMs. On GCE, you'll need to 
[turn it on by running a bootstrap script](https://cloud.google.com/tools/cloud-debugger/setting-up-on-compute-engine#enable_the_cloud_debugger_agent).

## Prerequisites

In order to follow along with this tutorial, you need the following software installed:

* IntelliJ IDEA 14 or later
* Java 1.7 or later
* maven
* git
* [Google Cloud SDK](https://cloud.google.com/sdk/)


## Installing the Plugins

*<span style="color:red">Note: these instructions don't work yet. They will soon. Meanwhile you'll need to follow [these instructions](https://github.com/GoogleCloudPlatform/gcloud-intellij/blob/master/README.md#installation).</span>* 

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
You can find this code in the Github project [cloud-debugger-idea-sample](https://github.com/GoogleCloudPlatform/cloud-debugger-idea-sample). 
It is built with Maven version 3.1 or later. 

(If you happen to spot the bug by eye before running it, pretend you don't and just read along.)

1. Clone the project to your own local repository:

        $ git clone https://github.com/GoogleCloudPlatform/cloud-debugger-idea-sample.git
        Cloning into 'cloud-debugger-idea-sample'...
        remote: Counting objects: 88, done.
        remote: Compressing objects: 100% (8/8), done.
        remote: Total 88 (delta 0), reused 0 (delta 0), pack-reused 71
        Unpacking objects: 100% (88/88), done.
        Checking connectivity... done.

2. Register your project on the [Google Developer's Console](https://console.developers.google.com/). You'll need to pick a project name. In this tutorial, I use hellobrowser, but you'll need to choose something else since that's now taken.

3. In your local copy of the source, open the file pom.xml in a text editor and change 
   the `artifactId` and  `app.id` elements from `hellobrowser` to the project name you registered in the developer console.

4. Build and test the application using `mvn clean install`. Note that all unit tests pass. (And if you're feeling really ambitious, check the code coverage.)

5. Commit your changes.

        $ git commit -a -m "set project ID"

6.  Commit and push the source code of the application to the
    [Cloud Source Repository](https://cloud.google.com/tools/cloud-repositories/docs/) associated with the project you just created. 
    *TBD: need more complete instructions here*
    *TBD: is there a way to do this with a maven command?*

        $ gcloud init
        $ git config credential.helper gcloud.sh
        $ git remote add google https://source.developers.google.com/p/*projectname*/
        $ git push --all google
        Counting objects: 30, done.
        Delta compression using up to 4 threads.
        Compressing objects: 100% (17/17), done.
        Writing objects: 100% (30/30), 2.11 KiB | 0 bytes/s, done.
        Total 30 (delta 7), reused 0 (delta 0)
        remote: Storing objects: 100% (30/30), done.
        remote: Processing commits: 100% (3/3), done.
        To https://source.developers.google.com/p/hellobrowser/
           530f08f..a5e90b0  master -> master

7.  Deploy your application using
    [maven](https://cloud.google.com/appengine/docs/java/tools/maven#uploading_your_app_to_production_app_engine):

        $ mvn appengine:update


8. Visit the application at http://*projectname*.appspot.com/hellobrowser using Chrome. You'll see it say:
 
  ![](images/HelloBrowser.png)
 
 Aha! That's a bug. It's supposed to say "Hello Chrome" when you visit in Chrome.
 If you like, try it in Safari or Opera. In fact, it almost always says "Hello Firefox."
 If you can find a case where it doesn't say that, that would give you a big clue
 as to the bug, but it's easier to use the cloud debugger.

## Using the Debugger

### Setting up a Cloud Debug Configuration


1. Inside IDEA, set up a new Java project from the project you cloned. 
   In the "Welcome to IntelliJ IDEA" window, pick "Import Project" and choose the pom.xml
   file.
   
2. If not already checked, check the box for import maven projects automatically.

3. Run > Edit Configurations...

3. Click the + icon on the upper left hand side. Select "Cloud Debug" from the popup menu. (If this option doesn't appear, check whether the plugin is installed and activated.)

4. Set the name of the configuration to "My First Debugging Session."

5. At the right-hand side of the Project popup, click the little arrow. You'll see a sign in pane.

  ![](images/MyFirstDebuggingSession.png)

6. Click the "Sign In" button.

7. A browser window will open outside of IDEA.

8. If you have more than one Google account logged into the browser, 
select the one that has access to the application you want to debug. 
If that account is not shown, click "Add Account" and login to the 
account that can manage the application.

9. You'll be asked to grant a list of permissions to IDEA. After waiting a few seconds for the button to activate, click "Allow".

  ![](images/permissions.png)

 (Yes, the list of permissions requested is overly long. 
 The Cloud Debugger doesn't actually need or use all of them.
 We're working on fixing that.)

10. You will see a window saying "IntelliJ is now authorized to access your account." Close the browser window and return to IntelliJ.

  ![](images/IntelliJIsNowAuthorized.png)


11. At the right-hand side of the Project popup, click the little arrow again. This time you'll see a list of applications managed by your account. Select the one you just created.

  ![](images/devconsoleprojects.png)


12. Click the OK button.

13. Run > Debug 'My First Debugging Session'

14. Select the module in the dialog that pops up. 
You may have to wait a few seconds for this to populate.
There may be only one of these.
 

### Set a breakpoint

Once you've attached  to a running application, you can set breakpoints in the 
source code by clicking at the line you want to snapshot in the left hand bar,
just as you would when debugging a local application using the regular IDEA debugger.

Here set a breakpoint at the line.

        if (userAgent != null) {

This way we can see what the `userAgent` variable is.

Go to the browser and reload the page.

Return to IDEA and you should see that a snapshot has appeared in the lower lefthand panel:


Click it and inspect the variables:

  ![](images/inspectuseragent.png)

Now you see that userAgent is "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36". 

Guess what? Chrome is sending a user agent string that contains the word Mozilla, even though 
Chrome has almost nothing to do with the Mozilla project. (Believe it or not, this brain 
damage goes back two decades, well before Chrome was conceived. Chrome isn't actually pretending to be Firefox here. It's pretending to be Netscape, but I digress.)

Now that we see what's going on, let's fix it.

1. Change the line

        if (userAgent.contains("Mozilla")) {

    to
            
        if (userAgent.contains("Firefox")) {

2. Deploy your application using

        $ mvn appengine:update

3. Visit the web page again:


Success!

Some notes:

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






