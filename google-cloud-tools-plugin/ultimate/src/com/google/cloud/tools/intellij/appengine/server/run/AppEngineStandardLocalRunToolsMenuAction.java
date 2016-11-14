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

package com.google.cloud.tools.intellij.appengine.server.run;

import com.google.cloud.tools.intellij.CloudToolsRunConfigurationAction;
import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener.UrlOpeningListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Creates a shortcut to the App Engine standard local run configuration in the tools menu.
 */
public class AppEngineStandardLocalRunToolsMenuAction extends CloudToolsRunConfigurationAction {

  private static final String APP_ENGINE_STANDARD_DOCS_LINK
      = "https://cloud.google.com/appengine/docs/java/";

  public AppEngineStandardLocalRunToolsMenuAction() {
    super(AppEngineServerConfigurationType.getInstance(),
        GctBundle.message("appengine.tools.menu.run.server.text"),
        GctBundle.message("appengine.tools.menu.run.server.description"),
        GoogleCloudToolsIcons.APP_ENGINE);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    if (event.getProject() != null && isAppEngineStandardProjectCheck(event.getProject())) {
      super.actionPerformed(event);
    }
  }

  private boolean isAppEngineStandardProjectCheck(@NotNull Project project) {
    XmlFile webXml = AppEngineAssetProvider.getInstance()
        .loadAppEngineStandardWebXml(project,
            Arrays.asList(ModuleManager.getInstance(project).getModules()));

    boolean isAppEngineStandardProject = webXml != null;

    if (!isAppEngineStandardProject) {
      NotificationGroup notification =
          new NotificationGroup(
              GctBundle.message("appengine.tools.menu.run.server.error.title"),
              NotificationDisplayType.BALLOON,
              true);

      String errorMessage = new StringBuilder()
          .append(GctBundle.message("appengine.tools.menu.run.server.error.message"))
          .append("<br />")
          .append("<br />")
          .append(
              GctBundle.message("appengine.tools.menu.run.server.error.help",
                  "<a href=\"" + APP_ENGINE_STANDARD_DOCS_LINK + "\">",
                  "</a>"))
          .toString();

      notification.createNotification(
          GctBundle.message("appengine.tools.menu.run.server.error.title"),
          errorMessage,
          NotificationType.ERROR,
          new UrlOpeningListener(false /*expire*/)).notify(project);
    }

    return isAppEngineStandardProject;
  }

}
