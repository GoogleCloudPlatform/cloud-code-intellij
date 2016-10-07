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

package com.google.cloud.tools.intellij.appengine.module;

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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.EffectiveLanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import javax.swing.event.HyperlinkEvent;

public class JavaLanguageLevelSupportCheck implements ModuleComponent {

  private static final String COMPONENT_NAME = "App Engine Java Version Support Check";
  private static final String RUNTIME_TAG = "runtime";
  private static final String RUNTIME_TAG_JAVA_8 = "java8";
  private static final String UPDATE_HREF = "#update";
  private static final LanguageLevel HIGHEST_SUPPORTED_LANGUAGE_LEVEL = LanguageLevel.JDK_1_7;

  private Module thisModule;

  public JavaLanguageLevelSupportCheck(Module module) {
    this.thisModule = module;
  }

  @Override
  public void moduleAdded() {
    if (isModuleUsingUnsupportedLanguageLevel(thisModule)) {
      warnUser(thisModule);
    }
  }

  protected boolean isModuleUsingUnsupportedLanguageLevel(Module module) {
    // if it's not an app engine module, it's fine
    if (!hasAppEngineFacet(module)) {
      return false;
    }

    @Nullable
    XmlFile appengineWebXml = AppEngineAssetProvider.getInstance()
        .loadAppEngineStandardWebXml(module.getProject(), Arrays.asList(module));

    return isAppEngineStandard(appengineWebXml)
        && usesJava8OrGreater(module)
        && !declaresJava8Runtime(appengineWebXml);
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
    XmlTag rootTag;
    if (appengineWebXml == null || (rootTag = appengineWebXml.getRootTag()) == null) {
      return false;
    }
    String runtime = rootTag.getSubTagText(RUNTIME_TAG);
    return RUNTIME_TAG_JAVA_8.equals(runtime);
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

  private void warnUser(Module module) {
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
            new LanguageLevelLinkListener(module))
        .notify(module.getProject());
  }

  private static class LanguageLevelLinkListener implements NotificationListener {
    private Module invalidModule;

    public LanguageLevelLinkListener(Module invalidModule) {
      this.invalidModule = invalidModule;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      String href = event.getDescription();
      if (href.equals(UPDATE_HREF)) {
        // set the language level to the latest supported language level
        setModuleLanguageLevel(invalidModule, HIGHEST_SUPPORTED_LANGUAGE_LEVEL);
        notification.hideBalloon();
      }
    }
  }

  @NotNull
  @Override
  public String getComponentName() {
    return COMPONENT_NAME;
  }

  @Override
  public void projectOpened() {}

  @Override
  public void projectClosed() {}

  @Override
  public void initComponent() {}

  @Override
  public void disposeComponent() {}

}
