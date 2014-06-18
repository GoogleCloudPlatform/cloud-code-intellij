/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.wizard;

import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.wizard.AndroidStudioWizardStep;
import com.android.tools.idea.wizard.NewModuleWizardState;
import com.android.tools.idea.wizard.NewProjectWizardState;
import com.android.tools.idea.wizard.TemplateWizardStep;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiNameHelper;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard step for specifying the parameters of a backend module.
 */
public class BackendModuleWizardStep extends ModuleWizardStep implements AndroidStudioWizardStep {
  private static final String ATTR_DOC_URL = "docUrl";

  private final Project myProject;
  private final NewModuleWizardState myWizardState;
  private final TemplateWizardStep.UpdateListener myUpdateListener;
  private final Icon mySidePanelIcon;
  private JPanel myRootPane;
  private JTextField myPackageNameField;
  private JTextField myModuleNameField;
  private JComboBox myClientModuleCombo;
  private JBLabel myValidationStatus;
  private HyperlinkLabel myDocLabel;
  private JLabel myDocLabel2;
  private JPanel myDocPanel;
  private JLabel myDocLabel3;
  private boolean myUpdating;
  private boolean myPackageNameModified;

  public BackendModuleWizardStep(Project project,
                                 NewModuleWizardState wizardState,
                                 TemplateWizardStep.UpdateListener updateListener,
                                 Icon sidePanelIcon) {
    myProject = project;
    myWizardState = wizardState;
    myUpdateListener = updateListener;
    mySidePanelIcon = sidePanelIcon;
    setupListener(myModuleNameField, true);
    setupListener(myPackageNameField, false);
    myClientModuleCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        updateDataModel();
      }
    });

    List<Module> clientModules = new ArrayList<Module>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (AndroidFacet.getInstance(module) != null && ModuleRootManager.getInstance(module).getContentRoots().length > 0) {
        clientModules.add(module);
        if (wizardState.get(NewAppEngineModuleAction.ATTR_CLIENT_MODULE_NAME) == null) {
          wizardState.put(NewAppEngineModuleAction.ATTR_CLIENT_MODULE_NAME, module.getName());
        }
      }
    }
    myClientModuleCombo.setModel(new CollectionComboBoxModel(clientModules));
    myClientModuleCombo.setRenderer(new AndroidModuleListCellRenderer());

    myValidationStatus.setIcon(MessageType.ERROR.getDefaultIcon());

    // IntelliJ IDEA has a number of hyperlink components but none that support all 3 of: wrapping; hover cursor over a link; marking only
    // part of text as link. So we have to use multiple separate labels here. :(
    // Add some space above and below the doc and match HighlightableComponent.getTextOffset() (2 pixels left border on second label).
    myDocLabel.setBorder(IdeBorderFactory.createEmptyBorder(8, 0, 0, 0));
    myDocLabel2.setBorder(IdeBorderFactory.createEmptyBorder(0, 2, 0, 0));
    myDocLabel3.setBorder(IdeBorderFactory.createEmptyBorder(0, 2, 8, 0));
  }

  private void setupListener(JTextField field, final boolean updatePackageName) {
    field.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        if (myUpdating) {
          return;
        }
        myUpdating = true;
        try {
          synchronizePackageName(updatePackageName);
          updateDataModel();
          myUpdateListener.update();
        }
        finally {
          myUpdating = false;
        }
      }
    });
  }

  private void synchronizePackageName(boolean updatePackageName) {
    if (!myPackageNameModified) {
      if (updatePackageName) {
        String oldPackageName = myPackageNameField.getText();
        int lastDot = oldPackageName.lastIndexOf('.');
        String newPackageName = (lastDot >= 0 ? oldPackageName.substring(0, lastDot + 1) : "") + myModuleNameField.getText();
        myPackageNameField.setText(newPackageName);
      } else {
        myPackageNameModified = true;
      }
    }
  }

  @Override
  public JComponent getComponent() {
    return myRootPane;
  }

  public void loadDataFromTemplate() {
    if (myUpdating) {
      return;
    }
    myUpdating = true;
    try {
      Object moduleName = myWizardState.get(NewAppEngineModuleAction.ATTR_MODULE_NAME);
      if (moduleName == null) {
        moduleName = myWizardState.get(NewProjectWizardState.ATTR_MODULE_NAME);
      }
      if (moduleName != null) {
        myModuleNameField.setText(moduleName.toString());
      }
      Object packageName = myWizardState.get(TemplateMetadata.ATTR_PACKAGE_NAME);
      if (packageName != null) {
        myPackageNameField.setText(packageName.toString());
      }

      updateDocLabels();
    }
    finally {
      myUpdating = false;
    }
  }

  private void updateDocLabels() {
    String docUrl = (String)myWizardState.get(ATTR_DOC_URL);
    TemplateMetadata metadata = myWizardState.getTemplateMetadata();
    String title = metadata != null ? metadata.getTitle() : null;
    if (docUrl != null && title != null) {
      myDocPanel.setVisible(true);
      myDocLabel.setHyperlinkText("Check the ", "\"" + title + "\" documentation", "");
      myDocLabel.setHyperlinkTarget(docUrl);
    } else {
      myDocPanel.setVisible(false);
    }
  }

  @Override
  public void updateDataModel() {
    validate();
    myWizardState.put(NewAppEngineModuleAction.ATTR_MODULE_NAME, myModuleNameField.getText());
    myWizardState.put(TemplateMetadata.ATTR_PACKAGE_NAME, myPackageNameField.getText());

    Module clientModule = (Module) myClientModuleCombo.getSelectedItem();
    myWizardState.put(NewAppEngineModuleAction.ATTR_CLIENT_MODULE_NAME, clientModule == null ? "" : clientModule.getName());
  }

  @Override
  public boolean validate() {
    myValidationStatus.setVisible(true);
    String moduleName = myModuleNameField.getText().trim();
    String packageName = myPackageNameField.getText().trim();
    if (moduleName.length() == 0) {
      myValidationStatus.setText("Module name is empty.");
      return false;
    }
    if (ModuleManager.getInstance(myProject).findModuleByName(moduleName) != null ||
        new File(myProject.getBasePath(), moduleName).exists()) {
      myValidationStatus.setText("Module " + moduleName + " already exists.");
      return false;
    }
    if (!PsiNameHelper.getInstance(myProject).isQualifiedName(packageName)) {
      myValidationStatus.setText("Package name is invalid.");
      return false;
    }
    myValidationStatus.setVisible(false);
    return true;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myModuleNameField;
  }

  @Override
  public boolean isValid() {
    return validate();
  }

  @Override
  public Icon getIcon() {
    return mySidePanelIcon;
  }

  private static class AndroidModuleListCellRenderer extends ColoredListCellRenderer<Module> {
    @Override
    protected void customizeCellRenderer(JList list, Module value, int index, boolean selected, boolean hasFocus) {
      if (value == null) {
        return;
      }
      append(value.getName());
      AndroidFacet facet = AndroidFacet.getInstance(value);
      if (facet != null) {
        Manifest manifest = facet.getManifest();
        if (manifest != null) {
          String aPackage = manifest.getPackage().getValue();
          if (aPackage != null) {
            append(" (").append(aPackage).append(")");
          }
        }
      }
    }
  }
}
