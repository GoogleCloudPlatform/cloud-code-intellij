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

import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * @author nik
 */
public class AppEngineServerEditor extends
    ApplicationServerPersistentDataEditor<AppEngineServerData> {

  private JPanel myMainPanel;
  private TextFieldWithBrowseButton mySdkHomeField;

  public AppEngineServerEditor() {
    mySdkHomeField
        .addBrowseFolderListener("Google App Engine SDK", "Specify Google App Engine Java SDK home",
            null, FileChooserDescriptorFactory.createSingleFolderDescriptor());
  }

  protected void resetEditorFrom(AppEngineServerData data) {
    mySdkHomeField.setText(FileUtil.toSystemDependentName(data.getSdkPath()));
  }

  protected void applyEditorTo(AppEngineServerData data) {
    data.setSdkPath(FileUtil.toSystemIndependentName(mySdkHomeField.getText()));
  }

  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
