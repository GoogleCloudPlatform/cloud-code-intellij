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
  IDEA("IntelliJ IDEA", PlatformUtils.IDEA_PREFIX, LandingPages.INTELLIJ),
  IDEA_IC("IntelliJ IDEA Community Edition", PlatformUtils.IDEA_CE_PREFIX, LandingPages.INTELLIJ),
  RUBYMINE("RubyMine", PlatformUtils.RUBY_PREFIX, LandingPages.INTELLIJ),
  PYCHARM("PyCharm", PlatformUtils.PYCHARM_PREFIX, LandingPages.INTELLIJ),
  PYCHARM_PC("PyCharm Community Edition", PlatformUtils.PYCHARM_CE_PREFIX, LandingPages.INTELLIJ),
  PYCHARM_EDU("PyCharm Educational Edition", PlatformUtils.PYCHARM_EDU_PREFIX, LandingPages.INTELLIJ),
  PHPSTORM("PhpStorm", PlatformUtils.PHP_PREFIX, LandingPages.INTELLIJ),
  WEBSTORM("WebStorm", PlatformUtils.WEB_PREFIX, LandingPages.INTELLIJ),
  APPCODE("AppCode", PlatformUtils.APPCODE_PREFIX, LandingPages.INTELLIJ),
  CLION("CLion", PlatformUtils.CLION_PREFIX, LandingPages.INTELLIJ),
  DBE("0xDBE", PlatformUtils.DBE_PREFIX, LandingPages.INTELLIJ),
  ANDROID_STUDIO("Android Studio", "AndroidStudio", LandingPages.ANDROID_STUDIO);

  private final String myName;
  private final String myPlatformPrefix;
  private final LandingPages landingPages;

  public String getName() {
    return myName;
  }

  public String getPlatformPrefix() {
    return myPlatformPrefix;
  }

  IntelliJPlatform(String name, String platformPrefix, LandingPages landingPages) {
    myName = name;
    myPlatformPrefix = platformPrefix;
    this.landingPages = landingPages;
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
    return landingPages.getSuccessPage();
  }

  @NotNull
  public String getFailureLandingPage() {
    return landingPages.getFailurePage();
  }
}
