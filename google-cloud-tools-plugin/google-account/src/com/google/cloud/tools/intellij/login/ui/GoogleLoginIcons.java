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

package com.google.cloud.tools.intellij.login.ui;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

/**
 * A utility class that loads the icons that are used by the Google Login plugin.
 * <p/>
 * Using this class to load icons is preferred over creating scaled image icons manually in code,
 * as this utility class will automatically select the appropriate icons based on the current
 * theme (IntelliJ classic vs. Darcula) and will automatically select high resolution icons
 * for retina displays.
 * <p/>
 * To ensure the appropriate icons are selected, they should be named according to the
 * following rules:
 * <p/>
 * <ul>
 *   <li>icon_name.png - The IntelliJ classic image icon to use.</li>
 *   <li>icon_name@2x.png - The IntelliJ classic image icon to use for retina displays.</li>
 *   <li>icon_name_dark.png - The Darcula image icon to use.</li>
 *   <li>icon_name@2x_dark.png - The Darcula image icon to use for retina displays.</li>
 * </ul>
 * <p/>
 * Note that if no alternative image icons are specified, the single provided image icon
 * will be used in all themes and resolutions.
 */
public final class GoogleLoginIcons {
  public static final Icon GOOGLE_LOGO = load("/icons/google_logo.png");
  public static final Icon GOOGLE_FAVICON = load("/icons/googleFavicon@2x.png");
  public static final Icon DEFAULT_USER_AVATAR = load("/icons/loginAvatar.png");

  private static Icon load(String path) {
    return IconLoader.getIcon(path, GoogleLoginIcons.class);
  }

  private GoogleLoginIcons() {
    // This utility class should not be instantiated.
  }
}
