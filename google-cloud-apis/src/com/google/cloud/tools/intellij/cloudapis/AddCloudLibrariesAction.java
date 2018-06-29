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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import java.util.stream.Stream;

/**
 * The action in the Google Cloud Tools menu group that opens the wizard to add client libraries to
 * the user's project and manage cloud APIs.
 */
public final class AddCloudLibrariesAction extends DumbAwareAction {

  public AddCloudLibrariesAction() {
    super(
        GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.active.text"),
        GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.description"),
        GoogleCloudCoreIcons.CLOUD);
  }

  @Override
  @SuppressWarnings("MissingCasesInEnumSwitch")
  public void update(AnActionEvent e) {
    Stream.of(CloudApiActionDecoratorExtension.EP_NAME.getExtensions())
        .forEach(decorator -> decorator.decorate(e));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    if (e.getProject() != null) {
      AddCloudLibrariesDialog librariesDialog = new AddCloudLibrariesDialog(e.getProject());
      librariesDialog.show();

      if (librariesDialog.isOK()) {
        CloudApiUiPresenter uiPresenter = CloudApiUiPresenter.getInstance();
        if (uiPresenter instanceof DefaultCloudApiUiPresenter) {
          ((DefaultCloudApiUiPresenter) uiPresenter)
              .notifyCloudLibrariesAddition(
                  librariesDialog.getSelectedLibraries(), librariesDialog.getSelectedModule());
        }
      }
    }
  }
}
