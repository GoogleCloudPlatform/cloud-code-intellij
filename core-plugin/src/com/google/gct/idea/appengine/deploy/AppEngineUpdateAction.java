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
package com.google.gct.idea.appengine.deploy;

import com.google.gct.idea.stats.UsageTracker;
import com.google.gct.idea.util.GctTracking;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;

/**
 * Handles the menu action to deploy to AppEngine.
 */
public class AppEngineUpdateAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    final Module selectedModule = LangDataKeys.MODULE.getData(e.getDataContext());

    UsageTracker.getInstance().trackEvent(GctTracking.CATEGORY, GctTracking.DEPLOY, "show.dialog", null);
    AppEngineUpdateDialog.show(e.getProject(), selectedModule);
  }
}
