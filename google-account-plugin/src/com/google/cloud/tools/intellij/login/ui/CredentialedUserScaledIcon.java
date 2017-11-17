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

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.intellij.util.ui.JBUI;
import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Provides scaled icon for {@link com.google.cloud.tools.intellij.login.CredentialedUser}. */
public class CredentialedUserScaledIcon {
  public static Icon getScaledUserIcon(int size, CredentialedUser user) {
    Icon icon = GoogleLoginIcons.DEFAULT_USER_AVATAR;
    if (user != null) {
      Image userImage = user.getPicture();
      if (userImage != null) {
        int targetIconSize = JBUI.scale(size);
        Image scaledUserImage = userImage.getScaledInstance(targetIconSize, targetIconSize, Image.SCALE_SMOOTH);
        icon = new ImageIcon(scaledUserImage);
      }
    }

    return icon;
  }
}
