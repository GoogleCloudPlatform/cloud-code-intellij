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

package com.google.cloud.tools.intellij.startup;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import javax.swing.event.HyperlinkEvent;

/**
 * A StartupActivity that warns the user if they are using an unsupported java version.
 */
public class UnsupportedJavaVersionCheck implements StartupActivity {

  private static final String RUNTIME_TAG = "runtime";
  private static final String RUNTIME_TAG_JAVA_8 = "java8";
  private static final String UPDATE_HREF = "#update";
  private static final LanguageLevel HIGHEST_SUPPORTED_LANGUAGE_LEVEL = LanguageLevel.JDK_1_7;

  @Override
  public void runActivity(@NotNull Project project) {
    if (shouldWarnUser(project)) {
      warnUser(project);
    }
  }

  private boolean shouldWarnUser(Project project) {
    return usesJava8OrGreater(project)
        && containsAnyAppEngineModulesWithoutJava8Support(project);
  }

  private boolean usesJava8OrGreater(Project project) {
    return PsiUtil.getLanguageLevel(project).compareTo(LanguageLevel.JDK_1_8) >= 0;
  }

  private boolean containsAnyAppEngineModulesWithoutJava8Support(Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (hasAppEngineFacet(module)) {
        @Nullable
        XmlFile appengineWebXml = AppEngineAssetProvider.getInstance()
            .loadAppEngineStandardWebXml(project, Arrays.asList(module));

        if (isAppEngineStandard(appengineWebXml) && !declaresJava8Runtime(appengineWebXml)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasAppEngineFacet(Module module) {
    return AppEngineFacet.getAppEngineFacetByModule(module) != null;
  }

  private boolean isAppEngineStandard(XmlFile appengineWebXml) {
    AppEngineEnvironment environment = AppEngineProjectService.getInstance()
        .getModuleAppEngineEnvironment(appengineWebXml);

    return environment == AppEngineEnvironment.APP_ENGINE_STANDARD;
  }

  private boolean declaresJava8Runtime(@NotNull XmlFile appengineWebXml) {
    XmlTag rootTag = appengineWebXml.getRootTag();
    if (rootTag == null) {
      return false;
    }
    String runtime = rootTag.getSubTagText(RUNTIME_TAG);
    return RUNTIME_TAG_JAVA_8.equals(runtime);
  }

  private void warnUser(@NotNull Project project) {
    NotificationGroup notification =
        new NotificationGroup(
            GctBundle.message("plugin.conflict.error.title"),
            NotificationDisplayType.BALLOON,
            true);

    String message =
        new StringBuilder()
            .append("<p>")
            .append(GctBundle.message("appengine.support.java.version.alert.detail",
                "<a href=\"" + UPDATE_HREF + "\">",
                "</a>"))
            .append("</p>")
            .toString();

    notification
        .createNotification(
            GctBundle.message("appengine.support.java.version.alert.title"),
            message,
            NotificationType.WARNING,
            new LanguageLevelLinkListener(project))
        .notify(project);
  }

  private static class LanguageLevelLinkListener implements NotificationListener {
    private Project project;

    public LanguageLevelLinkListener(Project project) {
      this.project = project;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      String href = event.getDescription();
      if (href.equals(UPDATE_HREF)) {
        // set the project language level to the highest supported level
        LanguageLevelProjectExtension.getInstance(project)
            .setLanguageLevel(HIGHEST_SUPPORTED_LANGUAGE_LEVEL);
        notification.hideBalloon();
      }
    }
  }
}
