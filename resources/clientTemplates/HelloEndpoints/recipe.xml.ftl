<?xml version="1.0"?>
<recipe>
    <merge from="AndroidManifest.xml.ftl" to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />
    <merge from="build.gradle.ftl" to="${escapeXmlAttribute(projectOut)}/build.gradle" />
</recipe>
