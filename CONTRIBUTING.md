# Contributing to Cloud Tools for IntelliJ IDEA

Cloud Tools for IntelliJ IDEA is an open source project.
We appreciate your help!


## Filing issues

When filing an issue, please answer these questions:

1. What version of IDEA are you using?
2. What version of Java are you using?
3. What did you do?
4. What did you expect to see?
5. What did you see instead?


## Building

All code can be checked out from our Github repository in the usual way.
That is, clone the repository with HTTPS or SSH: 

```
$ git clone https://github.com/GoogleCloudPlatform/gcloud-intellij.git
Cloning into 'gcloud-intellij'...
Username for 'https://github.com': your_username 
Password for 'https://elharo@github.com': 
remote: Counting objects: 6441, done.
remote: Compressing objects: 100% (256/256), done.
remote: Total 6441 (delta 112), reused 0 (delta 0), pack-reused 6121
Receiving objects: 100% (6441/6441), 9.04 MiB | 17.41 MiB/s, done.
Resolving deltas: 100% (2915/2915), done.
Checking connectivity... done.
```

There are two subprojects, google-login-plugin and core-plugin.

We use gradle as our build system. Gradle is self-installing. This one command

$ ./gradlew plugin

compiles everything, runs the test, and builds the plugins. The output appears in google-login-plugin/build and core-plugin/build.

Other useful targets while developing include:

* $ ./gradlew compileJava: compile
* $ ./gradlew test: run tests
* $ ./gradlew check: run static analysis tools
* $ ./gradlew clean: remove all build artifacts


## Configuring and Debugging in IntelliJ 


### Set up the Global libraries. 

Assuming you have installed IDEA in your home directory:

1. Project Defaults/Project Structure
2. Click “Platform Settings/Global Libraries”
3. Click the plus icon and add a new library named “android” pointing to
   “~/IntelliJ/AndroidStudio141/plugins/android/lib”
4. Click the plus icon and add a new library named “groovy” 
   pointing to “~/IntelliJ/AndroidStudio141/plugins/Groovy/lib”
5. Click the plus icon and add a new library named “git4idea” 
   pointing to “~/IntelliJ/Idea-IC-141.1532/plugins/git4idea/lib”
6. Click the plus icon and add a new library named “gradle” 
   pointing to “~/IntelliJ/Idea-IC-141.1532/plugins/gradle/lib”

Of course, change the path to your IDEA installation as necessary.

*Important!*  If you create these global libraries with the project open, do *not* add them to any projects. (IDEA will ask you if you want to add them).  Doing so will create duplicate, incorrect entries.

### Import Project 

To work in IDEA, select "Import  Project" from the IDEA opening screen and 
choose the root build.gradle file. 

You can do this without setting up the global libraries as above. However
when you attempt to run the tests from inside IDEA, they will likely
fail with strange errors about missing Groovy plaugins and the like.


### Optional:  Downloading source for IDEA to debug.

Download IDEA source into your home (or any other convenient) directory:

```
$ mkdir ~/IntelliJ-Src
$ cd ~/IntelliJ-Src
$ git clone git://git.jetbrains.org/idea/community.git idea
```

In the IntelliJ CE 141.1532 SDK, add ~/IntelliJ-Src in the Sourcepath tab or Project Structure > SDKs.

*Warning*: this code is from HEAD and is not exactly the same version that the actual release was cut from. In the debugger line numbers will be off in some classes. 
TBD: where is the correct source?


## Contributing code

1. Is there a contributor license to sign????
2. Fork the repository into your own Github account.
3. Please include at least minimal unit tests for all new code. (Yes, we know not all 
   the existing code has tests. We're slowly fixing that, and contributions of tests
   for existing code are much appreciated.
4. Make sure all existing tests pass. (gradlew test)
5. Associate the change with an existing issue or file a new issue. 
6. Create a pull request and send it to who????. 


Unless otherwise noted, our source files are distributed under
the Apache license found in the LICENSE file.

