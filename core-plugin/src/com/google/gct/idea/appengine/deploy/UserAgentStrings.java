/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gct.idea.appengine.deploy;

public class UserAgentStrings {
    // Note: If IDE is using gradle or maven under the hood to make the deploy calls,
    // it should be reflected in the name of the user-agent
    // e.g 'MAVEN_4_CLOUD_TOOLS_FOR_ANDROID_STUDIO'

    public static String TRACKING_KEY = "appengine.useragent";

    // Sources for appcfg update/deploy calls
    public static String CT4AS = "CLOUD_TOOLS_FOR_ANDROID_STUDIO";
}
