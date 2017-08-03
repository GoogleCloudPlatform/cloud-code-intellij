/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.server.integration;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.javaee.appServerIntegrations.ApplicationServerHelper;
import com.intellij.javaee.appServerIntegrations.ApplicationServerInfo;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.javaee.appServerIntegrations.CantFindApplicationServerJarsException;
import com.intellij.javaee.oss.server.JavaeePersistentData;
import java.io.File;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class AppEngineServerHelper implements ApplicationServerHelper {

  @Override
  public ApplicationServerInfo getApplicationServerInfo(
      ApplicationServerPersistentData persistentData)
      throws CantFindApplicationServerJarsException {
    return new ApplicationServerInfo(new File[]{},
        GctBundle.getString("appengine.run.server.name"));
  }

  @Override
  public ApplicationServerPersistentData createPersistentDataEmptyInstance() {
    return new JavaeePersistentData();
  }

  @Override
  public ApplicationServerPersistentDataEditor createConfigurable() {
    final CloudSdkPanel cloudSdkPanel = new CloudSdkPanel();

    return new ApplicationServerPersistentDataEditor<ApplicationServerPersistentData>() {
      @Override
      protected void resetEditorFrom(ApplicationServerPersistentData data) {
        cloudSdkPanel.reset();
      }

      @Override
      protected void applyEditorTo(ApplicationServerPersistentData data) {
        if (CloudSdkService.getInstance()
            .isValidCloudSdk(cloudSdkPanel.getCloudSdkDirectoryText())) {
          cloudSdkPanel.apply();
        }
      }

      @NotNull
      @Override
      protected JComponent createEditor() {
        return cloudSdkPanel.getComponent();
      }
    };
  }
}
