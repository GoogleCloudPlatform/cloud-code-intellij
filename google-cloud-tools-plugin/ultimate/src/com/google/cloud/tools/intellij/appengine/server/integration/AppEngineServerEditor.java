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
import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * @author nik
 */
public class AppEngineServerEditor extends
    ApplicationServerPersistentDataEditor<ApplicationServerPersistentData> {

  private JPanel myMainPanel;
  // TODO(joaomartins): Replace with CloudSdkPanel when
  // https://youtrack.jetbrains.com/issue/IDEA-110316 gets fixed.
  private TextFieldWithBrowseButton mySdkHomeField;
  private JLabel warningMessage;

  public AppEngineServerEditor() {
    mySdkHomeField
        .addBrowseFolderListener("Google App Engine SDK", "Specify Google App Engine Java SDK home",
            null, FileChooserDescriptorFactory.createSingleFolderDescriptor());
    mySdkHomeField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        onSdkPathChanged();
      }
    });
  }

  private void onSdkPathChanged() {
    if (warningMessage.getText().isEmpty()) {
      warningMessage.setVisible(true);
      warningMessage.setForeground(JBColor.RED);
      warningMessage.setText(GctBundle.getString("appengine.cloudsdk.location.missing.message"));
    } else if (!CloudSdkUtil.containsCloudSdkExecutable(mySdkHomeField.getText())) {
      warningMessage.setVisible(true);
      warningMessage.setForeground(JBColor.RED);
      warningMessage.setText(GctBundle.getString("appengine.cloudsdk.location.invalid.message"));
    } else {
      warningMessage.setVisible(false);
    }
  }

  protected void resetEditorFrom(ApplicationServerPersistentData data) {
    CloudSdkService sdkService = CloudSdkService.getInstance();
    mySdkHomeField.setText(sdkService.getSdkHomePath() != null
        ? sdkService.getSdkHomePath().toString() : "" );
  }

  protected void applyEditorTo(ApplicationServerPersistentData data) {
    CloudSdkService.getInstance().setSdkHomePath(mySdkHomeField.getText());
  }

  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }
}
