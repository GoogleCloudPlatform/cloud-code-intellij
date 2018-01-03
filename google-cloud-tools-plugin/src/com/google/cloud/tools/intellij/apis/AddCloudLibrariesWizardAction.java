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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.intellij.ide.wizard.AbstractWizard;
import com.intellij.ide.wizard.Step;
import com.intellij.ide.wizard.StepAdapter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.DialogManager;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nullable;

/**
 * The action in the Google Cloud Tools menu group that opens the wizard to add client libraries to
 * the user's project and manage cloud APIs.
 */
public final class AddCloudLibrariesWizardAction extends DumbAwareAction {

  private static final Logger logger = Logger.getInstance(AddCloudLibrariesWizardAction.class);

  public AddCloudLibrariesWizardAction() {
    super(
        GctBundle.message("cloud.libraries.menu.action.text"),
        GctBundle.message("cloud.libraries.menu.action.description"),
        GoogleCloudToolsIcons.CLOUD);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    if (e.getProject() != null) {
      AddCloudLibrariesWizard librariesDialog = new AddCloudLibrariesWizard(e.getProject());
      DialogManager.show(librariesDialog);

      if (librariesDialog.isOK()) {
        CloudLibraryDependencyWriter.addLibraries(
            librariesDialog.getSelectedLibraries(), librariesDialog.getSelectedModule());
      }
    }
  }

  /** The wizard for the "Add Cloud Libraries" menu action. */
  // TODO(eshaul) this does not need to be a wizard and can be converted to a simple dialog.
  private static final class AddCloudLibrariesWizard extends AbstractWizard<Step> {

    private final Project project;
    private final SelectClientLibrariesStep selectClientLibrariesStep;

    AddCloudLibrariesWizard(Project project) {
      super(GctBundle.message("cloud.libraries.dialog.title"), project);

      this.project = project;

      selectClientLibrariesStep = new SelectClientLibrariesStep(project);
      selectClientLibrariesStep.addModuleSelectionListener(event -> updateButtons());

      addStep(selectClientLibrariesStep);
      init();
    }

    /** Returns the selected {@link Module}. */
    Module getSelectedModule() {
      return selectClientLibrariesStep.getSelectedModule();
    }

    /** Returns the set of selected {@link CloudLibrary CloudLibraries}. */
    Set<CloudLibrary> getSelectedLibraries() {
      return selectClientLibrariesStep.getSelectedLibraries();
    }

    Set<CloudLibrary> getApisToEnable() {
      return selectClientLibrariesStep.getApisToEnable();
    }

    @Override
    protected boolean canGoNext() {
      return selectClientLibrariesStep.getSelectedModule() != null;
    }

    @Nullable
    @Override
    protected String getHelpID() {
      return null;
    }

    /**
     * Overrides {@link DialogWrapper#doOKAction()} to first check if there are any APIs to enable
     * on GCP.
     *
     * <p>If so, the {@link CloudApiManagementDialog} is opened allowing the user to select a cloud
     * project and confirm. If the user cancels, the user is returned to this parent dialog.
     * Otherwise, it is closed.
     */
    @Override
    protected void doOKAction() {
      Set<CloudLibrary> apisToEnable = getApisToEnable();

      if (!apisToEnable.isEmpty()) {
        CloudApiManagementDialog managementDialog = new CloudApiManagementDialog(project);
        DialogManager.show(managementDialog);

        if (managementDialog.isOK()) {
          ProgressManager.getInstance()
              .runProcessWithProgressSynchronously(
                  () ->
                      CloudApiManager.enableApis(apisToEnable, managementDialog.getCloudProject()),
                  GctBundle.message("cloud.apis.enable.progress.title"),
                  true /*canBeCanceled*/,
                  project);

          super.doOKAction();
        }
      } else {
        super.doOKAction();
      }
    }
  }

  /** The wizard step encapsulating the client library selection panel. */
  private static final class SelectClientLibrariesStep extends StepAdapter {

    private final GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

    SelectClientLibrariesStep(Project project) {
      List<CloudLibrary> libraries;
      try {
        libraries = CloudLibraries.getCloudLibraries();
      } catch (IOException e) {
        logger.error(e);
        libraries = ImmutableList.of();
      }

      this.cloudApiSelectorPanel = new GoogleCloudApiSelectorPanel(libraries, project);
    }

    /** Adds the given {@link ActionListener} to the panel's module combobox. */
    void addModuleSelectionListener(ActionListener listener) {
      cloudApiSelectorPanel.addModuleSelectionListener(listener);
    }

    /** Returns the selected {@link Module}. */
    Module getSelectedModule() {
      return cloudApiSelectorPanel.getSelectedModule();
    }

    /** Returns the set of selected {@link CloudLibrary CloudLibraries}. */
    Set<CloudLibrary> getSelectedLibraries() {
      return cloudApiSelectorPanel.getSelectedLibraries();
    }

    Set<CloudLibrary> getApisToEnable() {
      return cloudApiSelectorPanel.getApisToEnable();
    }

    @Override
    public JComponent getComponent() {
      return cloudApiSelectorPanel.getPanel();
    }
  }
}
