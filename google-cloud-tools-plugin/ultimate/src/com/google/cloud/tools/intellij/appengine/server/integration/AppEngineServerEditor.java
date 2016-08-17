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

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;

import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * @author nik
 */
public class AppEngineServerEditor extends
    ApplicationServerPersistentDataEditor<ApplicationServerPersistentData> {

  private JPanel myMainPanel;
  // TODO(joaomartins): Replace with CloudSdkPanel when
  // https://youtrack.jetbrains.com/issue/IDEA-110316 gets fixed.
  private TextFieldWithBrowseButton mySdkHomeField;

  public AppEngineServerEditor() {
    mySdkHomeField
        .addBrowseFolderListener("Google App Engine SDK", "Specify Google App Engine Java SDK home",
            null, FileChooserDescriptorFactory.createSingleFolderDescriptor());
  }

  protected void resetEditorFrom(ApplicationServerPersistentData data) {
    mySdkHomeField.setText(CloudSdkService.getInstance().getSdkHomePath().toString());
  }

  protected void applyEditorTo(ApplicationServerPersistentData data) {
    CloudSdkService.getInstance().setSdkHomePath(mySdkHomeField.getText());
  }

  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
