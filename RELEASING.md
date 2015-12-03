# Creating Plugin Releases

This page describes how to cut releases of our plugin. It's strictly for the owners of the project.

1. Create a new branch as if you were starting a normal bugfix PR

    ```
    git checkout -b release_v1.0
    ```

1. Modify the root build.gradle version field to the release version number. (do not include the 'v' prefix)

    ```
        version = '1.0'
    ```

1. Commit your change with a clear commit message.

    ```
    git commit -a -m "Set plugin version to 1.0"
    ```

1. Push your change.

    ```
    git push origin release_v1.0
    ```

1. Create a PR and send your change for review to another team member. Label it with the 'Release' tag.
1. Merge when approved.
1. Now we need to return to a snapshot release. Follow the same flow but update the version number to something like:

    ```
        version = '1.1-SNAPSHOT'
    ```

1. Commit your changes, push, send a PR, and merge.
1. The final step is to label the release in Github.
1. From the 'releases' tab in Github
1. Click 'Draft a new release'
1. Add a tag version which is 'v' + the version number. e.g. 'v1.0'
1. Include the release version name in the release title.
1. Add any relevant release documents
1. Click 'Publish release'
1. You're done. Assuming all goes well Travis will upload the release binaries to the Github release
 details page and to the Jetbrains plugin repository.
