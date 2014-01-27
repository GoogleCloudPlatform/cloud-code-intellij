// Currently, the appengine gradle plugin's appengine devappserver launch doesn't interact well with Intellij/AndroidStudio's
// Gradle integration.  As a temporary solution, please launch from the command line.
// If you would like more information on the gradle-appengine-plugin please refer to the github page
// https://github.com/GoogleCloudPlatform/gradle-appengine-plugin

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath 'com.google.appengine:gradle-appengine-plugin:1.8.7-SNAPSHOT'
    }
}

repositories {
    mavenCentral();
}

apply plugin: 'java'
apply plugin: 'war'
apply plugin: 'appengine'

dependencies {
  appengineSdk 'com.google.appengine:appengine-java-sdk:1.8.7'
  compile 'com.google.appengine:appengine-endpoints:1.8.7'
  compile 'javax.servlet:servlet-api:2.5'
}

appengine {
  downloadSdk = true
  httpPort = 8080
  endpoints {
    getClientLibsOnBuild = true
    getDiscoveryDocsOnBuild = true
  }
}
