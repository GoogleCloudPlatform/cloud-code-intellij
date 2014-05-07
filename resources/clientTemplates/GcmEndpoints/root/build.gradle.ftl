dependencies {
    // Make sure that you have installed "Extras -> Google Play services" component in Android SDK Manager
    compile 'com.google.android.gms:play-services:3.1.+'
    compile project(path: '${serverModulePath}', configuration: 'android-endpoints')
}