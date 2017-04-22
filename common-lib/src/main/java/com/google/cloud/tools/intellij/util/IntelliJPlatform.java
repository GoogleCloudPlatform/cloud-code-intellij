/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.util;

import com.intellij.util.PlatformUtils;

/** Identifies the current platform based on platform prefix. */
public enum IntelliJPlatform {
  IDEA("IntelliJ IDEA Ultimate Edition", PlatformUtils.IDEA_PREFIX),
  IDEA_IC("IntelliJ IDEA Community Edition", PlatformUtils.IDEA_CE_PREFIX),
  RUBYMINE("RubyMine", PlatformUtils.RUBY_PREFIX),
  PYCHARM("PyCharm", PlatformUtils.PYCHARM_PREFIX),
  PYCHARM_PC("PyCharm Community Edition", PlatformUtils.PYCHARM_CE_PREFIX),
  PYCHARM_EDU("PyCharm Educational Edition", PlatformUtils.PYCHARM_EDU_PREFIX),
  PHPSTORM("PhpStorm", PlatformUtils.PHP_PREFIX),
  WEBSTORM("WebStorm", PlatformUtils.WEB_PREFIX),
  APPCODE("AppCode", PlatformUtils.APPCODE_PREFIX),
  CLION("CLion", PlatformUtils.CLION_PREFIX),
  DBE("0xDBE", PlatformUtils.DBE_PREFIX),
  ANDROID_STUDIO("Android Studio", "AndroidStudio");

  private final String name;
  private final String platformPrefix;

  IntelliJPlatform(String name, String platformPrefix) {
    this.name = name;
    this.platformPrefix = platformPrefix;
  }

  /** Given a prefix, return the corresponding platform. */
  public static IntelliJPlatform fromPrefix(String prefix) {
    for (IntelliJPlatform product : values()) {
      if (prefix.equals(product.getPlatformPrefix())) {
        return product;
      }
    }
    return IDEA;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getPlatformPrefix() {
    return platformPrefix;
  }
}
