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
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineStandardRuntime;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.EffectiveLanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.HyperlinkEvent;

/**
 * A StartupActivity that warns the user if they are using a Java language level that corresponds
 * to an unsupported jdk version on App Engine standard.
 */
public class AppEngineStandardUnsupportedJavaVersionCheck implements StartupActivity {

  private static final String UPDATE_HREF = "#update";
  private static final LanguageLevel HIGHEST_SUPPORTED_LANGUAGE_LEVEL = LanguageLevel.JDK_1_7;

  @Override
  public void runActivity(@NotNull Project project) {
    List<Module> invalidModules = findAppEngineModulesUsingUnsupportedLanguageLevel(project);
    if (!invalidModules.isEmpty()) {
      warnUser(project, invalidModules);
    }
  }

  private List<Module> findAppEngineModulesUsingUnsupportedLanguageLevel(Project project) {
    Module[] projectModules = ModuleManager.getInstance(project).getModules();
    List<Module> invalidModules = new ArrayList<>();

    for (Module module : projectModules) {
      // only check app engine modules
      if (hasAppEngineFacet(module)) {

        @Nullable
        XmlFile appengineWebXml = AppEngineAssetProvider.getInstance()
            .loadAppEngineStandardWebXml(project, Arrays.asList(module));

        if (isAppEngineStandard(appengineWebXml)
            && usesJava8OrGreater(module)
            && !declaresJava8Runtime(appengineWebXml)) {
          invalidModules.add(module);
        }
      }
    }
    return invalidModules;
  }

  private boolean usesJava8OrGreater(Module module) {
    LanguageLevel languageLevel = EffectiveLanguageLevelUtil.getEffectiveLanguageLevel(module);
    return languageLevel.compareTo(LanguageLevel.JDK_1_8) >= 0;
  }

  private boolean hasAppEngineFacet(Module module) {
    return AppEngineFacet.getAppEngineFacetByModule(module) != null;
  }

  private boolean isAppEngineStandard(@Nullable XmlFile appengineWebXml) {
    AppEngineEnvironment environment = AppEngineProjectService.getInstance()
        .getModuleAppEngineEnvironment(appengineWebXml);

    return environment == AppEngineEnvironment.APP_ENGINE_STANDARD;
  }

  private boolean declaresJava8Runtime(@Nullable XmlFile appengineWebXml) {
    AppEngineStandardRuntime runtime = AppEngineProjectService.getInstance()
        .getAppEngineStandardDeclaredRuntime(appengineWebXml);
    return runtime != null && runtime.isJava8();
  }

  private static void setModuleLanguageLevel(Module module, LanguageLevel languageLevel) {
    final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module)
        .getModifiableModel();
    rootModel.getModuleExtension(LanguageLevelModuleExtension.class)
        .setLanguageLevel(languageLevel);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        rootModel.commit();
      }
    });
  }

  private void warnUser(Project project, List<Module> invalidModules) {
    String message =
        new StringBuilder()
            .append("<p>")
            .append(GctBundle.message("appengine.support.java.version.alert.detail",
                "<a href=\"" + UPDATE_HREF + "\">",
                "</a>"))
            .append("</p>")
            .toString();

    NotificationGroup notification =
        new NotificationGroup(
            GctBundle.message("appengine.support.java.version.alert.title"),
            NotificationDisplayType.BALLOON,
            true);

    notification
        .createNotification(
            GctBundle.message("appengine.support.java.version.alert.title"),
            message,
            NotificationType.WARNING,
            new LanguageLevelLinkListener(invalidModules))
        .notify(project);
  }

  private static class LanguageLevelLinkListener implements NotificationListener {
    private List<Module> invalidModules;

    public LanguageLevelLinkListener(List<Module> invalidModules) {
      this.invalidModules = invalidModules;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      String href = event.getDescription();
      if (href.equals(UPDATE_HREF)) {

        // set the language level for all unsupported modules to the latest supported language level
        for (Module module : invalidModules) {
          setModuleLanguageLevel(module, HIGHEST_SUPPORTED_LANGUAGE_LEVEL);
        }
        notification.hideBalloon();
      }
    }
  }
}
