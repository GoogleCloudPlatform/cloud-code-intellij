# Creating Plugin Releases

This page describes how to cut releases of our plugin. It's strictly for the owners of the project.

1. Create a new branch named release_v{$version}.  It's critical that you create the branch according to this schema or the release will fail.

    ```
    git fetch
    git checkout -b release_v1.0 origin/master
    ```

1. From the command line execute:
    
    ```
    gradlew release
    ```
    
1. At the prompt set the version for release to {$version}
1. The next prompt will ask you for the next snapshot release which you should set as {$nextVersion}-SNAPSHOT
1. Once execution completes the tag and release should appear automatically on Github and Travis will trigger a deployment of the release artifacts to Github and the Jetbrains plugin repository.
1. Go to your new release branch in Github, create a PR, and merge to master.