buildscript {
    repositories {
        maven { url 'http://repo1.maven.org/maven2' }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:0.5+'
    }
}

apply plugin: 'android-library'

repositories {
    mavenCentral();
}

dependencies {
    compile files('libs/gcm.jar')
    compile ('com.google.api-client:google-api-client-android:${clientLibVersion}') {
        exclude group: 'com.google.android.google-play-services'
    }
    compile 'com.google.http-client:google-http-client-jackson:1.15.0-rc'
}

android {
    compileSdkVersion ${buildApi}
    buildToolsVersion "${buildApi}"

    defaultConfig {
        minSdkVersion ${minApi}
        targetSdkVersion ${targetApi}
    }

    sourceSets {
      main {
        java.srcDirs = ['src/main/java', 'src/endpoint-src/java']
      }
    }
}
