---
title: IntelliJ IDEA Google Cloud Debugger Tutorial
---

# IntelliJ IDEA Google Cloud Debugger Tutorial

The Google Cloud Debugger enables you to capture and inspect the call stack 
and local variables of a live application running in the cloud
(more specifically on [Google App Engine](https://cloud.google.com/appengine/docs), Google [Compute Engine](https://cloud.google.com/compute/), or, with some additional configuration, [Managed VMs](https://cloud.google.com/appengine/docs/managed-vms/).)
 
It works much like the IntelliJ IDEA debugger you're already used to,
and provides the same user interface, with two key differences:

* The Cloud Debugger does not pause execution of the running application.

* You cannot step through an application in the Cloud Debugger.

In other words, the Cloud Debugger is a *forensic* debugger, not an *interactive* debugger.

The Cloud Debugger is enabled automatically for Java 
applications running on App Engine and Managed VMs. On GCE, you'll need to 
[turn it on by running a bootstrap script](https://cloud.google.com/tools/cloud-debugger/setting-up-on-compute-engine#enable_the_cloud_debugger_agent).

## Prerequisites

In order to follow along with this tutorial, you need the following software installed:

* IntelliJ IDEA 14 or later, either Community or Ultimate Edition
* [JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html). (Java 6 won't work with the sample. Java 8 cannot yet be deployed on App Engine.)
* [Apache maven](https://maven.apache.org/download.cgi) 3.1 or later
* git
* [Google Cloud SDK](https://cloud.google.com/sdk/)
* Chrome browser (since the bug we're looking for is browser specific)
* A Google account that can deploy AppEngine apps. (Personal accounts, 
  i.e. gmail.com accounts work. Users with Google Apps for Work accounts may
  need to contact their administrator to enable GCP on their account.)


## Installing the Plugins

*<span style="color:red">Note: these instructions don't work yet. They will soon. Meanwhile you'll need to follow [these instructions](https://github.com/GoogleCloudPlatform/gcloud-intellij/blob/master/README.md#installation).</span>* 

The plugin can be installed from the 
[IntelliJ IDEA Plugin Repository](https://www.jetbrains.com/idea/plugins/).

From inside IDEA:

1. File > Settings

2. In the left-hand pane, select Plugins.

3. Click the "Browse repositories..." button.

4. In the dialog that opens, select "Google Cloud Tools." 

5. Click the Green Install button.

7. Click Close.

8. Click OK in the Settings dialog.

9. Click Restart (or you can click Postpone, but the plugins will not be available until you do restart IDEA.)

 ![](images/restartintellij.png)

## Setting up the app

For purposes of this tutorial you'll debug a simple servlet that detects the
user's browser and says "Hello Firefox", "Hello Chrome", and so forth, depending on the browser.
You can find this code in the Github project [cloud-debugger-idea-sample](https://github.com/GoogleCloudPlatform/cloud-debugger-idea-sample). 
It is built with Maven version 3.1 or later. 

(If you happen to spot the bug by eye before running it, pretend you don't and just read along.)

*TODO: make this all work in IntelliJ without using the command line*

1. Clone the project to a local repository:
 
        $ git clone https://github.com/GoogleCloudPlatform/cloud-debugger-idea-sample.git
        Cloning into 'cloud-debugger-idea-sample'...
        remote: Counting objects: 108, done.
        remote: Total 108 (delta 0), reused 0 (delta 0), pack-reused 108
        Receiving objects: 100% (108/108), 31.41 KiB | 0 bytes/s, done.
        Resolving deltas: 100% (30/30), done.


2. Register your project on the [Google Developer's Console](https://console.developers.google.com/). You'll need to pick a project name. The sample code uses the project id "hellobrowser," 
   but you'll need to choose something else since that's now taken.

3. In your local copy of the source, open the file pom.xml in a text editor and change 
   the `artifactId` and  `app.id` elements from `hellobrowser` to the project name you registered in the Developer Console.

4. Build and test the application using `mvn clean install`. Note that all unit tests pass. (And if you're feeling really ambitious, check the code coverage.)

5. Commit your changes:

        $ git commit -a -m "set project ID"

6. Run gcloud init to initialize the Google Cloud SDK with the project ID 
   you registered in the Developer Console:

        $ gcloud init
        ...
        Enter project id you would like to use:  *projectname*
        Your current project has been set to: [*projectname*].
        
   Depending on local configuration, you may also have to choose a specific 
   Google account and perform a few other initialization steps. 

7. Authorize with the credential helper script. On Linux and Mac OS X:
        
        $ git config credential.helper gcloud.sh
        
   On Windows:
   
        C:\> git config credential.helper gcloud.cmd
        
8. Add Cloud Source Repositories as a remote for the project:

        $ git remote add google https://source.developers.google.com/p/*projectname*/

9.  Commit and push the source code of the application to the
    [Cloud Source Repository](https://cloud.google.com/tools/cloud-repositories/docs/) associated with the project you just created:

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

10.  Deploy your application using
    [maven](https://cloud.google.com/appengine/docs/java/tools/maven#uploading_your_app_to_production_app_engine):

        $ mvn appengine:update


11. Visit the application at http://*projectname*.appspot.com/hellobrowser using Chrome. You'll see it say:
 
  ![](images/HelloBrowser.png)
 
 Aha! That's a bug. It's supposed to say "Hello Chrome" when you visit in Chrome.
 If you like, try it in Safari or Opera. In fact, it almost always says "Hello Firefox."
 If you can find a case where it doesn't say that, that would give you a big clue
 as to the bug; but it's easier to use the Cloud Debugger.

## Using the Debugger

### Setting up a Cloud Debug Configuration


1. Inside IDEA, set up a new Java project from the project you cloned. 
   In the "Welcome to IntelliJ IDEA" window, pick "Import Project" and choose the pom.xml
   file.
   
  ![](images/choosepom.png)
   
2. If not already checked, check the box for "Import Maven projects automatically."

  ![](images/importproject.png)
  
  From this point the default options in the rest of the wizard should be fine,
  though you may need to set up a Java 1.7 SDK if you have not already done so.

3. Once IntelliJ has loaded the project, pull down the Run menu 
   and select Edit Configurations... (Run > Edit Configurations...)
   
     ![](images/rundebugconfigurations.png)

4. Click the + icon on the upper left hand side. Select "Cloud Debug" from the popup menu. 

     ![](images/clouddebugconfiguration.png)

  (If this option doesn't appear, check whether the plugin is installed and activated.)

5. Set the name of the configuration to "My First Debugging Session."

     ![](images/entersessionname.png)

6. At the right-hand side of the Project popup, click the little arrow. You'll see a sign in pane.

  ![](images/MyFirstDebuggingSession.png)

7. Click the "Sign In" button.

8. A browser window opens outside of IDEA.

9. If you have more than one Google account logged into the browser, 
select the one that has access to the application you want to debug. 
If that account is not shown, click "Add Account" and login to the 
account that can manage the application.

10. You are now asked to grant a list of permissions to IDEA. After waiting a few seconds for the button to activate, click "Allow".

  ![](images/permissions.png)

 (Yes, the list of permissions requested is overly long. 
 The Cloud Debugger doesn't actually need or use all of them.
 We're working on fixing that.)

11. You will see a window saying "IntelliJ is now authorized to access your account." Close the browser window and return to IntelliJ.

  ![](images/IntelliJIsNowAuthorized.png)


12. At the right-hand side of the Project popup, click the little arrow again. This time you'll see a list of applications managed by your account. Select the one you just created.

  ![](images/devconsoleprojects.png)


13. Click the OK button.

14. From the Run menu select Debug 'My First Debugging Session' (Run > Debug 'My First Debugging Session')

15. Select the module in the dialog that pops up. 
You may have to wait a few seconds for this to populate.

  ![](images/attachtoanapplication.png)

The Debug pane will open, usually at the bottom of the window.

### Set a snapshot location

Once you've attached to a running application, you can set snapshot locations in the 
source code. 

First open the class you want to debug in the IDEA editor.
In this case that's HelloBrowserServlet.java. 

Now click the line you want to snapshot in the left gutter area,
just as you would when setting a line breakpoint for a local application using 
the regular IDEA debugger.

Here set a snapshot location at the line.

        if (userAgent != null) {
            
  ![](images/snapshotlocation.png)

The little blue circle that appears in the gutter when you click represents the snapshot location.
When you mouse over it, a tooltip will show you exactly where it's set.
Clicking the blue circle a second time removes the snapshot location.

Once you've set a snapshot location, a pending snapshot appears in the lower left hand pane. 
In this example, it's set at line 31. 

You can set snapshot locations on executable lines. 
You cannot set snapshot locations on non-executable lines such as
comments, declarations, and empty lines.
You can set multiple snapshot locations at once, in one or more classes. 


### Run the application

Go to the browser and reload the page.
If the IDEA window is visible at the same time, 
you'll see a brief popup saying "New snapshot received." 
(Don't blink or you'll miss it. If your browser occupies your entire display,
this popup will probably disappear before you switch back to IDEA. 
Don't worry. It's not especially important.) 

Return to IDEA and you should see that the snapshot in the lower lefthand panel
is no longer pending:

  ![](images/receivedsnapshot.png)

Click it. The bottom middle frame loads the stack trace and
the bottom right hand frame loads local variables and method arguments 
in scope at that point in the code and their values at that point in time. 
Fields are also available by expanding the "this" variable.

  ![](images/expandedsnapshot.png)

Inspect the `userAgent` variable in the right hand frame. You 
can either make the frame wider or use the context menu and select
"Inspect..."

  ![](images/inspectvariable.png)

Now you see that userAgent is "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36". 

Guess what? Chrome is sending a user agent string that contains the word "Mozilla," even though 
Chrome has almost nothing to do with the Mozilla project. (Believe it or not, this brain 
damage goes back two decades, well before Chrome was conceived. Chrome isn't actually pretending to be Firefox. It's pretending to be Netscape, a browser that hasn't been updated since 2008; 
but I digress.)

### Fix the bug

Now that you see what's going on, let's fix it.

1. Change the line:

        if (userAgent.contains("Mozilla")) {

    to:
            
        if (userAgent.contains("Firefox")) {

2. Deploy your application using:

        $ mvn appengine:update

3. Visit the web page again.

  ![](images/hellosafari.png)

Surprise! It's another bug. 
Try using the Cloud Debugger to find it. 
This bug is related to the first, and has a similar cause; though the fix is a little
different. 

Admittedly this is a simple example. These particular bugs could 
have been found in a locally running instance on the development server.
However, in more complex applications that interact with backend data stores and 
other live network services, local tests may not be able to reproduce the exact problems
you see in production. Furthermore, you can configure a live
production instance to alert you to a problem that you don't immediately know how to reproduce. 
For example, imagine that you knew the servlet was sending incorrect data but you didn't
know the problem was triggered by a particular browser. This is normally accomplished with
a conditional snapshot location.

####Some notes:

Since you can't single step through an application in the Cloud Debugger,
it's more common to put the snapshot location at the end of the relevant block
of code rather than at the beginning. That way all variables have been set and can
be inspected. 

If you're interested in variables in multiple scopes, (e.g. inside and 
outside a loop) you need to set multiple snapshot locations.

If you're interested in variables at particular points in time,
(e.g. on the last iteration of a loop), you can set a 
conditional snapshot location.

The server only sends a single snapshot per location. This keeps you from being 
inundated with snapshots from a high volume application. 
If you want to take another snapshot, 

You can generally inspect the values of variables in your own code.
(In this example, that's code in the `com.google.gct.tutorial` package.)
However, due to security restrictions in the App Engine sandbox,
you won't be able to delve as deeply into system classes such as 
`java.util.HashMap` or `javax.servlet.http.HttpServletRequest`
as you would in a local debugger. These restrictions are reduced,
though not completely eliminated, when running on Managed VMs or GCE.


### Close the debugger

When you're finished debugging, click the red square in the upper right corner 
of the debug pane. IntelliJ asks whether to continue listening for snapshots in the background:

![](images/continuelistening.png)


If you continue listening, then additional snapshots will be delivered to
IDEA and displayed in the Debug panel as your application executes.

### Advanced features


#### Conditional Snapshot Locations

A snapshot condition is a simple boolean Java expression 
that tells the Cloud Debugger to take a snapshot only when it evaluates to true. 
For example, `x > 3` or `userAgent.contains("Mozilla")`.
You can only define one condition per snapshot location.

To set a condition, right click on the blue circle indicating the snapshot location
and fill in a boolean expression that is valid at that line.

![](images/conditionalsnapshotlocation.png)


#### Watch Expressions

Sometimes the information you need to debug a problem is 
not immediately apparent in the application's local variables and fields.
In particular, when running on App Engine, the security manager 
prevents you from delving too deeply into the private members of 
system classes such as `java.util.Hashmap.` In cases like this,
a watch expression serves as an effective temporary local variable
to expose additional information. 
Watch expressions can evaluate complex expressions
and traverse object hierarchies when a snapshot is taken. 

You can specify a watch expression after you have set the snapshot location. To specify the watch expression:

1. Right click (Open the context menu) on the blue circle that represents the snapshot location.

2. Click the "More" link.

3. Click the + sign.

4. Type the expression you want evaluated and hit return.

![](images/watchexpressions.png)



