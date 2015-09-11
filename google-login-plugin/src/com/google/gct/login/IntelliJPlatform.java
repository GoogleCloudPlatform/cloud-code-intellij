/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.login;

import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the current platform based on platform prefix.
 */
public enum IntelliJPlatform {

  IDEA("IntelliJ IDEA", null, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  IDEA_IC("IntelliJ IDEA Community Edition", PlatformUtils.IDEA_CE_PREFIX, LandingPage.AUTH_SUCCESS,
      LandingPage.AUTH_FAILURE),
  RUBYMINE("RubyMine", PlatformUtils.RUBY_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  PYCHARM("PyCharm", PlatformUtils.PYCHARM_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  PYCHARM_PC("PyCharm Community Edition", PlatformUtils.PYCHARM_CE_PREFIX, LandingPage.AUTH_SUCCESS,
      LandingPage.AUTH_FAILURE),
  PYCHARM_EDU("PyCharm Educational Edition", PlatformUtils.PYCHARM_EDU_PREFIX, LandingPage.AUTH_SUCCESS,
      LandingPage.AUTH_FAILURE),
  PHPSTORM("PhpStorm", PlatformUtils.PHP_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  WEBSTORM("WebStorm", PlatformUtils.WEB_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  APPCODE("AppCode", PlatformUtils.APPCODE_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  CLION("CLion", PlatformUtils.CLION_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  DBE("0xDBE", PlatformUtils.DBE_PREFIX, LandingPage.AUTH_SUCCESS, LandingPage.AUTH_FAILURE),
  ANDROID_STUDIO("Android Studio", "AndroidStudio", LandingPage.AUTH_SUCCESS,
      LandingPage.AUTH_FAILURE); //there is no constant in PlatformUtils for AS.

  private interface LandingPage {
    String AUTH_SUCCESS = "https://developers.google.com/cloud/mobile/auth_success";
    String AUTH_FAILURE = "https://developers.google.com/cloud/mobile/auth_failure";
  }

  private final String myName;
  private final String myPlatformPrefix;
  private final String mySuccessfulLandingPage;
  private final String myFailureLandingPage;

  public String getName() {
    return myName;
  }

  public String getPlatformPrefix() {
    return myPlatformPrefix;
  }

  IntelliJPlatform(String name, String platformPrefix, String successfulLandingPage, String failureLandingPage) {
    myName = name;
    myPlatformPrefix = platformPrefix;
    mySuccessfulLandingPage = successfulLandingPage;
    myFailureLandingPage = failureLandingPage;
  }

  public static IntelliJPlatform fromPrefix(String prefix) {
    for (IntelliJPlatform product : values()) {
      if (prefix.equals(product.getPlatformPrefix())) {
        return product;
      }
    }
    return IDEA;
  }

  @NotNull
  public String getSuccessfulLandingPage() {
    return mySuccessfulLandingPage;
  }

  @NotNull
  public String getFailureLandingPage() {
    return myFailureLandingPage;
  }
}
