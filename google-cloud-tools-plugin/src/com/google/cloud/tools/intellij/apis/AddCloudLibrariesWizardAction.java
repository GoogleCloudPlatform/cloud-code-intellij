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
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.DialogManager;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.TableModelListener;
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
      AddCloudLibrariesWizard dialog = new AddCloudLibrariesWizard(e.getProject());
      DialogManager.show(dialog);
      if (dialog.isOK()) {
        CloudLibraryDependencyWriter.addLibraries(
            dialog.getSelectedLibraries(), dialog.getSelectedModule());
      }
    }
  }

  /** The wizard for the "Add Cloud Libraries" menu action. */
  private static final class AddCloudLibrariesWizard extends AbstractWizard<Step> {

    private List<CloudLibrary> libraries;

    private final SelectClientLibrariesStep selectClientLibrariesStep;
    private final ManageCloudApisStep manageCloudApisStep;

    AddCloudLibrariesWizard(Project project) {
      super(GctBundle.message("cloud.libraries.dialog.title"), project);

      try {
        libraries = CloudLibraries.getCloudLibraries();
      } catch (IOException e) {
        logger.error(e);
        libraries = ImmutableList.of();
      }

      selectClientLibrariesStep = new SelectClientLibrariesStep(libraries, project);
      selectClientLibrariesStep.addModuleSelectionListener(event -> updateButtons());
      selectClientLibrariesStep.addLibrarySelectionListener(event -> updateButtons());
      manageCloudApisStep = new ManageCloudApisStep();

      addStep(selectClientLibrariesStep);
      addStep(manageCloudApisStep);
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

    @Override
    protected boolean canGoNext() {
      return selectClientLibrariesStep.getSelectedModule() != null
          && !getSelectedLibraries().isEmpty();
    }

    @Override
    protected void updateStep() {
      super.updateStep();

      if (getCurrentStepObject() instanceof ManageCloudApisStep) {
        manageCloudApisStep.init(libraries, getSelectedLibraries());
      }
    }

    @Nullable
    @Override
    protected String getHelpID() {
      return null;
    }
  }

  /** The wizard step encapsulating the client library selection panel. */
  private static final class SelectClientLibrariesStep extends StepAdapter {

    private final GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

    SelectClientLibrariesStep(List<CloudLibrary> libraries, Project project) {
      this.cloudApiSelectorPanel = new GoogleCloudApiSelectorPanel(libraries, project);
    }

    /** Adds the given {@link ActionListener} to the panel's module combobox. */
    void addModuleSelectionListener(ActionListener listener) {
      cloudApiSelectorPanel.addModuleSelectionListener(listener);
    }

    /** Adds the given {@link TableModelListener} to the panel's library table. */
    void addLibrarySelectionListener(TableModelListener listener) {
      cloudApiSelectorPanel.addLibrarySelectionListener(listener);
    }

    /** Returns the selected {@link Module}. */
    Module getSelectedModule() {
      return cloudApiSelectorPanel.getSelectedModule();
    }

    /** Returns the set of selected {@link CloudLibrary CloudLibraries}. */
    Set<CloudLibrary> getSelectedLibraries() {
      return cloudApiSelectorPanel.getSelectedLibraries();
    }

    @Override
    public JComponent getComponent() {
      return cloudApiSelectorPanel.getPanel();
    }
  }

  /** The wizard step encapsulating the cloud API management panel. */
  private static final class ManageCloudApisStep extends StepAdapter {

    private final GoogleCloudApiManagementPanel cloudApiManagementPanel;

    ManageCloudApisStep() {
      cloudApiManagementPanel = new GoogleCloudApiManagementPanel();
    }

    void init(List<CloudLibrary> allLibraries, Set<CloudLibrary> selectedLibraries) {
      cloudApiManagementPanel.init(allLibraries, selectedLibraries);
    }

    @Override
    public JComponent getComponent() {
      return cloudApiManagementPanel.getContentPanel();
    }
  }
}
