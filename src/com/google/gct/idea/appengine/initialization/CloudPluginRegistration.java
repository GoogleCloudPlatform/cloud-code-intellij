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
package com.google.gct.idea.appengine.initialization;

import com.google.gct.idea.appengine.deploy.AppEngineUpdateAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.Anchor;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Initializes the menus for deploy.
 */
public class CloudPluginRegistration implements ApplicationComponent {

  // We are reusing the flag for login.
  private final static String SHOW_DEPLOY = "show.google.login.button";

  public CloudPluginRegistration() {
  }

  @Override
  public void initComponent() {
    if (Boolean.getBoolean(SHOW_DEPLOY)) {
      ActionManager am = ActionManager.getInstance();

      AppEngineUpdateAction action = new AppEngineUpdateAction();
      action.getTemplatePresentation().setText("Deploy Module to App Engine...");

      am.registerAction("GoogleCloudTools.AppEngineUpdate", action);
      DefaultActionGroup buildMenu = (DefaultActionGroup)am.getAction("BuildMenu");

      DefaultActionGroup appEngineUpdateGroup = new DefaultActionGroup();
      appEngineUpdateGroup.addSeparator();
      appEngineUpdateGroup.add(action);
      buildMenu.add(appEngineUpdateGroup, new Constraints(Anchor.AFTER, "Compile"));
    }
  }

  @Override
  public void disposeComponent() {
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "CloudPluginRegistration";
  }
}
