<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${packageName}"
    android:versionCode="1"
    android:versionName="1.0">

    <uses-sdk android:minSdkVersion="${minApi}" <#if buildApi gte 4>android:targetSdkVersion="${targetApi}" </#if>/>

    <permission android:name="${packageName}.permission.C2D_MESSAGE" android:protectionLevel="signature"/>

    <uses-permission android:name="android.permission.INTERNET"/>

    <uses-permission android:name="${packageName}.permission.C2D_MESSAGE"/>
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
    <uses-permission android:name="android.permission.USE_CREDENTIALS"/>

    <application>
        <service android:name="${packageName}.GCMIntentService"/>
        <receiver android:name="com.google.android.gcm.GCMBroadcastReceiver"
                android:permission="com.google.android.c2dm.permission.SEND">
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
                <category android:name="${packageName}"/>
            </intent-filter>
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.REGISTRATION"/>
                <category android:name="${packageName}"/>
            </intent-filter>
        </receiver>
        <activity android:launchMode="singleTop" android:name=".RegisterActivity"/>
    </application>

</manifest>