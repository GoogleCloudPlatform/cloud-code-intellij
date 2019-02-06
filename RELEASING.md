# Creating Plugin Releases

This page describes how to cut releases of our plugin. It's strictly for the owners of the project.

1. Update [CHANGELOG.md](https://github.com/GoogleCloudPlatform/google-cloud-intellij/blob/master/CHANGELOG.md) with any major release notes (see [guidelines](http://keepachangelog.com/en/0.3.0/)). Replace the contents of the `<change-notes>` tag in [plugin.xml](https://github.com/GoogleCloudPlatform/google-cloud-intellij/blob/master/google-cloud-tools-plugin/resources/META-INF/plugin.xml) with the same notes compiled from Markdown to html.
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
1. Once execution completes the tag and release should appear automatically on Github 
1. Open the Kokoro and execute the `google-cloud-intellij/gcp_ubuntu/release` job, specifying the newly created tag name as the build target (`v{$version}`). Kokoro will trigger a deployment of the release artifacts to Github and the Jetbrains plugin repository alpha channel.
1. After testing the release, upload the binary from the alpha channel to the stable channel to publish the release.
1. Go to your new release branch in Github, create a PR, and merge to master.
