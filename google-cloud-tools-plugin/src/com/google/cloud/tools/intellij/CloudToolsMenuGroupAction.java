/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * Creates the tools menu top-level item for Cloud Tools. All relevant shortcuts are sub-menu items
 * of this.
 */
public class CloudToolsMenuGroupAction extends DefaultActionGroup {

  public CloudToolsMenuGroupAction() {
    getTemplatePresentation().setIcon(GoogleCloudToolsIcons.CLOUD);
  }

}
