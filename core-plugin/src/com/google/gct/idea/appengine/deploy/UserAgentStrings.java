package com.google.gct.idea.appengine.deploy;

/**
 * Holds list of appcfg user-agent options that are very specific names of the tool calling appcfg.
 * For example, if the IDE is using gradle or maven under the hood to make the deploy calls,
 * it should be reflected in the name of the user-agent e.g 'MAVEN_VIA_ANDROID_STUDIO'
 */
public class UserAgentStrings {
    public static String USER_AGENT_KEY = "appengine.useragent";

    private UserAgentStrings () {
    }

    // Sources for appcfg update/deploy calls
    public static String ANDROID_STUDIO = "ANDROID_STUDIO";
}
