/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.action;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.GoogleCloudCoreMessageBundle;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * Top menu group for all plugin visible actions that are expressed with menu items. All K8s and
 * Google Cloud functionality is nested under this group. See plugin.xml for structure.
 */
public class CloudCodeTopMenuGroupAction extends DefaultActionGroup {
  public CloudCodeTopMenuGroupAction() {
    getTemplatePresentation()
        .setText(GoogleCloudCoreMessageBundle.message("cloud.code.top.group.menu.text"));
    getTemplatePresentation().setIcon(GoogleCloudCoreIcons.CLOUD);
  }
}
