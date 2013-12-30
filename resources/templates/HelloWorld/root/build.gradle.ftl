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
  compile 'javax.inject:javax.inject:1'
  compile 'javax.jdo:jdo-api:3.0.1'
  testCompile 'junit:junit:4.10'
  testCompile 'org.mockito:mockito-all:1.9.0'
  testCompile 'com.google.appengine:appengine-testing:1.8.7'
  testCompile 'com.google.appengine:appengine-api-stubs:1.8.7'
}

appengine {
  downloadSdk = true
  endpoints {
    getClientLibsOnBuild = true
  }
}
