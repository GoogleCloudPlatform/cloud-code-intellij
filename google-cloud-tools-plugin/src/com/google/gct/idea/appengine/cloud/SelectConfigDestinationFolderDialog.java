/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import org.jetbrains.annotations.Nullable;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A widget for selecting a folder in which to generate ManagedVM source files.
 */
public class SelectConfigDestinationFolderDialog extends DialogWrapper {

  private JPanel rootPanel;
  private TextFieldWithBrowseButton destinationFolderChooser;

  public SelectConfigDestinationFolderDialog(@Nullable Project project) {
    super(project);
    setTitle(GctBundle.message("appengine.flex.config.destination.chooser.title"));

    init();
    destinationFolderChooser.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.choose.destination.folder.window.title"),
        null,
        project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }

  public File getDestinationFolder() {
    return new File(destinationFolderChooser.getText());
  }
}
