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

package com.google.cloud.tools.intellij.stackdriver.facet;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Represents Stackdriver support for a project.
 *
 * <p>This class makes it possible to show "Google Stackdriver" as an option in the New Project
 * dialog, as well as in the Add Facet dialog.
 */
public class StackdriverFrameworkType extends FrameworkTypeEx {

  public static final String ID = "stackdriver-java";

  protected StackdriverFrameworkType() {
    super(ID);
  }

  static StackdriverFrameworkType getFrameworkType() {
    return EP_NAME.findExtension(StackdriverFrameworkType.class);
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleProvider createProvider() {
    return new StackdriverSupportProvider();
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return GctBundle.getString("stackdriver.name");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.STACKDRIVER;
  }
}
