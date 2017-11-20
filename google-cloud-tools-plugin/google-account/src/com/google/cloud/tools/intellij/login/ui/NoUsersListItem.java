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

import java.awt.Image;

/**
 * A place holder for when no user exist. This allows us to create a customized panel
 * when no users exist.
 */
public class NoUsersListItem extends UsersListItem {
  public static final NoUsersListItem INSTANCE = new NoUsersListItem();

  private NoUsersListItem() {
    super(null);
  }

  @Override
  public String getUserEmail() {
    return null;
  }

  @Override
  public boolean isActiveUser() {
    return false;
  }

  @Override
  public String getUserName() {
    return null;
  }

  @Override
  public Image getUserPicture() {
    return null;
  }
}
