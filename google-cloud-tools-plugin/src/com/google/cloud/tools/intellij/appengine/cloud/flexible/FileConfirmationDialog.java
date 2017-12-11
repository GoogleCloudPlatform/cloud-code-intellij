/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import java.nio.file.Path;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dialog warning for, e.g., overwriting existing app.yaml and Dockerfile configuration files when
 * user selects a destination folder to auto-generate them.
 */
public class FileConfirmationDialog extends DialogWrapper {

  public enum DialogType {
    CONFIRM_OVERWRITE,
    CONFIRM_CREATE_DIR,
    NOT_DIRECTORY_ERROR
  }

  private JPanel rootPanel;
  private JLabel warningLabel;
  private JTextField pathDisplay;

  private DialogType dialogType;

  /** Initialize the dialog of the correct type. */
  public FileConfirmationDialog(
      @Nullable Project project, DialogType dialogType, @NotNull Path targetPath) {
    super(project);

    this.dialogType = dialogType;

    switch (dialogType) {
      case CONFIRM_OVERWRITE:
        setTitle(GctBundle.message("appengine.flex.config.destination.overwrite.title"));
        warningLabel.setText(GctBundle.message("appengine.flex.config.destination.overwrite"));
        break;

      case CONFIRM_CREATE_DIR:
        setTitle(GctBundle.message("appengine.flex.config.destination.create.dir.title"));
        warningLabel.setText(GctBundle.message("appengine.flex.config.destination.create.dir"));
        break;

      case NOT_DIRECTORY_ERROR:
        setTitle(GctBundle.message("appengine.flex.config.destination.not.directory.title"));
        warningLabel.setText(GctBundle.message("appengine.flex.config.destination.not.directory"));
        break;

      default:
        throw new AssertionError();
    }
    pathDisplay.setText(targetPath.toString());

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    if (dialogType == DialogType.NOT_DIRECTORY_ERROR) {
      return new Action[] {getOKAction()};
    } else {
      return super.createActions();
    }
  }
}
