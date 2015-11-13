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
```

There are three modules: google-account-plugin, core-plugin, and common-lib.

On Linux/Mac OS X we use gradle as our build system. 
Gradle is self-installing. This one command

$ ./gradlew plugin

compiles everything, runs the test, and builds the plugins. The output appears in google-account-plugin/build and core-plugin/build.

Other useful targets while developing include:

* $ ./gradlew compileJava: compile
* $ ./gradlew test: run tests
* $ ./gradlew check: run static analysis tools
* $ ./gradlew clean: remove all build artifacts

Gradle builds fail on Windows due to reliance on the fetchIdea.sh shell script.
This script simply downloads and installs IntelliJ IDEA code for building against
and shouldn't be too hard to port to Windows. See 
[Issue 167](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/167).
As a quick workaround you can manually download and install the relevant 
IDEA distribution and comment out the loadIdea task in 
[build.gradle](https://github.com/GoogleCloudPlatform/gcloud-intellij/blob/master/build.gradle).

On Windows you should be able to build the plugins inside IntelliJ by
selecting "Prepare All Plugin Modules For Deployment" from the Build menu.

## Configuring and Debugging in IntelliJ

### Import Project 

To work in IDEA, just "Open" the cloud-tools-for-intellij directory 
(the root directory cloned from Github) from the IDEA opening screen.

Alternately you can select "Import  Project" from the IDEA opening screen and 
choose the root build.gradle file. In this case, IDE features for IDEA plugin
development may not work; and run and debug configurations will not
be available from within the IDE. However, you can run and debug unit tests.

### Optional:  Downloading source for IDEA to debug.

[Download the IDEA source tarball from JetBrains](http://www.jetbrains.org/display/IJOS/Download) 
into your home (or any other convenient) directory. Extract it and 
add the resulting directory (usually something like ideaIC-141.1532.4) 
in the Sourcepath tab of Project Structure > SDKs for the IntelliJ CE 141.1532 SDK.


## Contributing code

1. First, please [sign either individual or corporate contributor license agreement](https://cla.developers.google.com/), whichever is applicable.
2. Fork the repository into your own Github account.
3. Please include unit tests for all new code. (Yes, we know not all 
   existing code has tests. We're slowly fixing that, and contributions of tests
   for existing code are much appreciated.
4. Make sure all existing tests pass. (gradlew test)
5. Associate the change with an existing issue or [file a new issue](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/new). 
6. Create a pull request and send it to gcloud-intellij:master. 


Unless otherwise noted, our source files are distributed under
the Apache license found in the LICENSE file.

A number of issues in the 
[issue tracker](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/new)
have been tagged as "[Help Wanted](https://github.com/GoogleCloudPlatform/gcloud-intellij/labels/help%20wanted)." 
These are relatively small, self-contained changes that are good places to start. 

## FAQ

### java.lang.OutOfMemoryError: PermGen space when running the tests inside IDEA

From the Run menu select “Edit Configurations...” In the "VM options" field add -XX:MaxPermSize=256m (and if that doesn't work try 512m instead).


