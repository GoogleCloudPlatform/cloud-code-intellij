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

package com.google.cloud.tools.intellij.appengine.java.ultimate.server.run;

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.appengine.java.ultimate.server.instance.AppEngineServerModel;
import com.google.cloud.tools.intellij.appengine.java.ultimate.server.integration.AppEngineServerIntegration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.javaee.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.run.configuration.J2EEConfigurationFactory;
import com.intellij.javaee.run.configuration.J2EEConfigurationType;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import java.util.Arrays;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author nik */
public class AppEngineServerConfigurationType extends J2EEConfigurationType {

  public static AppEngineServerConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(AppEngineServerConfigurationType.class);
  }

  @Override
  protected RunConfiguration createJ2EEConfigurationTemplate(
      ConfigurationFactory factory, Project project, boolean isLocal) {
    final AppEngineServerModel serverModel = new AppEngineServerModel();
    return J2EEConfigurationFactory.getInstance()
        .createJ2EERunConfiguration(
            factory, project, serverModel, getIntegration(), isLocal, new CloudSdkStartupPolicy());
  }

  @Override
  public String getDisplayName() {
    return AppEngineMessageBundle.getString("appengine.run.server.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return getDisplayName() + " run configuration";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    final ConfigurationFactory configurationFactory = super.getConfigurationFactories()[0];

    return new ConfigurationFactory[] {
      new ConfigurationFactory(this) {
        @Override
        public String getName() {
          return configurationFactory.getName();
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
          return configurationFactory.createTemplateConfiguration(project);
        }

        /**
         * If false, then this run configuration shows up under the "irrelevant" section. If it's
         * not an App Engine Standard project (doesn't have an appengine-web.xml) then the run
         * config should show up as "irrelevant".
         */
        @Override
        public boolean isApplicable(@NotNull Project project) {
          XmlFile webXml =
              AppEngineAssetProvider.getInstance()
                  .loadAppEngineStandardWebXml(
                      project, Arrays.asList(ModuleManager.getInstance(project).getModules()));

          return webXml != null;
        }
      }
    };
  }

  @Override
  public AppServerIntegration getIntegration() {
    return AppEngineServerIntegration.getInstance();
  }

  @NotNull
  @Override
  public String getId() {
    return "gcp-app-engine-local-run";
  }
}
