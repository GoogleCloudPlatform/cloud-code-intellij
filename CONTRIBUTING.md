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

There are four modules:

* google-clout-tools: Cloud debugger and code inspections
* common-lib: Code shared between the above two
* common-test-lib: test infrastructure code

When adding new dependencies, the jar files are loaded from Maven Central
when available. If Maven Central doesn't have the necessary version,
the jar is placed in the third_party directory.

On Linux/Mac OS X we use gradle as our build system. 
Gradle is self-installing. This one command

$ ./gradlew buildPlugin

compiles everything, runs the tests, and builds the plugins. The output appears in
google-cloud-tools-plugin/build/distributions.

Other useful targets while developing include:

* $ ./gradlew compileJava: compile
* $ ./gradlew test: run tests
* $ ./gradlew check: run static analysis tools
* $ ./gradlew clean: remove all build artifacts
* $ ./gradlew runIdea: run IntelliJ preconfigured with the plugins from this project.

## Configuring and Debugging in IntelliJ

### Import Project 

1. 'New project from existing sources'
1. Select the root build.gradle file to import
1. Git revert changes to the .idea folder because IDEA Gradle import blows it away
 (https://youtrack.jetbrains.com/issue/IDEA-146295)

### Run/Debug
The **Cloud Tools on IntelliJ** run configuration can be use to run or debug the project. To set which IntelliJ IDEA edition to launch when running or debugging, update the ideaEdition flag in [gradle.properties](https://github.com/GoogleCloudPlatform/google-cloud-intellij/blob/c5bbf8d018806f370d664622b70d5744480edc86/gradle.properties#L17) using IU for Ultimate Edition and IC for Community Edition.

## Contributing code

1. First, please [sign either individual or corporate contributor license agreement](https://cla.developers.google.com/), whichever is applicable.
1. Set your git user.email property to the address used for step 1. E.g.
   ```
   git config --global user.email "janedoe@google.com"
   ```
   If you're a Googler or other corporate contributor, 
   use your corporate email address here, not your personal address.
1. Read the [Google Java Style Guide](http://google.github.io/styleguide/javaguide.html)
1. Setup IntelliJ IDEA
    1. Install the [error-prone plugin](http://errorprone.info/docs/installation) from the plugin
     repo and configure your project compiler to use it.
    1. Install the [Google-java-style IntelliJ plugin](https://github.com/google/google-java-format)
        1. Make sure to run Code -> Reformat with google-java-format at the beginning of every IDE 
        session.
        1. Configure the IDE to automatically reformat your code and optimize imports.
            1. Hint - you can do this by configuring it to do so on git push. 
        1. If you're not using IntelliJ you can run reformat from the command line using 
        `gradlew googleJavaFormat`.
1. Fork the repository into your own Github account.
1. Please include unit tests for all new code. (Yes, we know not all
   existing code has tests. We're slowly fixing that, and contributions of tests
   for existing code are much appreciated.)
1. Make sure all existing tests pass. (gradlew test)
1. For significant user facing changes add your change to the 'Unreleased' section of CHANGELOG.md.
1. Associate the change with an existing issue or [file a new issue](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/new).
1. Create a pull request and send it to gcloud-intellij:master.


Unless otherwise noted, our source files are distributed under
the Apache license found in the LICENSE file.

A number of issues in the 
[issue tracker](https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/new)
have been tagged as "[Help Wanted](https://github.com/GoogleCloudPlatform/gcloud-intellij/labels/help%20wanted)." 
These are relatively small, self-contained changes that are good places to start.

## Project Utilities

### `CloudToolsRule`
We have a custom JUnit `TestRule` that reduces the amount of boilerplate code we have in our unit
tests. Notably, this rule handles a lot of the set-up and tear-down required for a proper IntelliJ
project context.

For example:

```
@Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
```

A summary of its features can be found below.

**Always:**
1. Initializes the fields annotated with `@Mock` by calling
   `MockitoAnnotations.initMocks(testInstance)`
2. Creates an `IdeaProjectTestFixture`, which sets up static application state for the IntelliJ SDK
   and creates a `Project`

**Testing UI components:**

If your unit test is testing platform UI components such as toolbars/panels/dialogs, then you should instantiate these components on the Swing event-dispatch thread.

For example:

```
ApplicationManager.getApplication().invokeAndWait(() -> // initialize your component);
```

**Additional Annotations:**

`@TestService`:
- Annotates fields of any type, but usually accompanies a `@Mock` annotation
- Before the test runs, it swaps the actual implementation of the service that is registered in the
  application's `PicoContainer` with the field's value
- After the test runs, it swaps the real implementation back
- Sample:
    ```
    @Mock @TestService private CloudSdkService mockCloudSdkService;

    @Test
    public void myTest() {
      when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());
      // Do something that depends on this mocked call...
    }
    ```

`@TestFixture`:
- Annotates fields of type `com.intellij.testFramework.fixtures.IdeaProjectTestFixture`
- Injects the created `IdeaProjectTestFixture` into the annotated field, giving access to the
  underlying `Project`
- Sample:
    ```
    @TestFixture private IdeaProjectTestFixture testFixture;

    @Test
    public void myTest() {
      Project project = testFixture.getProject();
      // Do something with the project...
    }
    ```

`@TestModule`:
- Annotates fields of type `com.intellij.openapi.module.Module`
- Creates a new `Module` and adds it to the test fixture's `Project`, then injects the value of the
  created `Module` into the annotated field
- Optionally allows for the addition of a `Facet` to the `Module` by specifying the ID of the
  associated `com.intellij.facet.FacetType`
- Samples:
    ```
    @TestModule private Module myModule;
    @TestModule(facetTypeId = MyFacetType.ID) private Module myModuleWithFacet;
    ```

`@TestFile`:
- Annotates fields of type `java.io.File`
- Creates a new `File` with the given name and optionally the given contents
- Manages the deletion of the file after the test is torn down
- Samples:
    ```
    @TestFile(name = "my.file")
    private File myFile;

    @TestFile(name = "my-file-with.contents", contents = "Some contents")
    private File myFileWithContents;
    ```

## FAQ

Nothing here yet.
