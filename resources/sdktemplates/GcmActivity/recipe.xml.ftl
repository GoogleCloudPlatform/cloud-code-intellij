<?xml version="1.0"?>
<recipe>

    <#if isGradle == "true">
        <instantiate from="build.gradle.ftl"
                to="${projectOut}/build.gradle" />
        <merge from="settings.gradle.ftl"
                to="${topOut}/settings.gradle" />
    </#if>

    <merge from="AndroidManifest.xml.ftl"
            to="${manifestOut}/AndroidManifest.xml" />

    <instantiate from="res/layout/activity_register.xml.ftl"
            to="${resOut}/layout/activity_register.xml" />


    <instantiate from="src/app_package/RegisterActivity.java.ftl"
            to="${srcOut}/RegisterActivity.java" />

    <instantiate from="src/app_package/CloudEndpointUtils.java.ftl"
            to="${srcOut}/CloudEndpointUtils.java" />

    <instantiate from="src/app_package/GCMIntentService.java.ftl"
            to="${srcOut}/GCMIntentService.java" />

</recipe>
