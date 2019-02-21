## Google Cloud Platform Support Satrix

In addition to IntelliJ IDEA, the plugin provides limited support for the following IDEs, version 2017.1 or later:
* PyCharm (Professional and Community Editions)
* WebStorm
* PhpStorm
* Rider
* CLion
* RubyMine
* GoLand
* AppCode

This table displays the features available in the IDEs:

| | IntelliJ IDEA - Ultimate | IntelliJ IDEA - Community | All other IntelliJ platform based IDEs |
|---|:---:|:---:|:---:|
| Create App Engine standard environment App | :heavy_check_mark: | :x: | :x: |
| Create App Engine flexible environment App | :heavy_check_mark: | :heavy_check_mark: | :x: |
| Run and Debug App Engine<br>standard environment App Locally | :heavy_check_mark: | :heavy_check_mark: \* | :x: |
| Deploy App Engine flexible environment App | :heavy_check_mark: | :heavy_check_mark: | :x: |
| Deploy App Engine standard environment App | :heavy_check_mark: | :heavy_check_mark: \** | :x: |
| Manage Cloud Client Libraries | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| Browse files in Cloud Storage | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| Use VCS for Source Control | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| Debug using Stackdriver | :heavy_check_mark: | :heavy_check_mark: | :x: |
| Auto-manage the Cloud SDK | :heavy_check_mark: | :heavy_check_mark: | :x: |

<p><b>*</b> You can follow the
  <a href="https://cloud.google.com/tools/intellij/docs/deploy-local#debugging_your_application_locally_on_community_edition">debugging your
    application locally on Community Edition</a> instructions to use the Maven
  or Gradle plugins for your local run.</p>

<p><b>**</b> You can deploy Maven-based projects using the IntelliJ IDEA
  Community Edition to the App Engine standard environment.</p>