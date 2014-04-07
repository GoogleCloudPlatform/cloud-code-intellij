/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.io.File;

public class AppEngineModuleWizard extends DialogWrapper {
  private JPanel myRootPanel;
  private JTextField myModuleNameField;
  private JTextField myPackageNameField;
  private JComboBox myTemplateBox;
  private JEditorPane myDescriptionPane;
  private String myModuleName;
  private String myPackageName;
  private File myTemplate;
  private final Project myProject;

  public AppEngineModuleWizard(Project project) {
    super(project, true);
    this.myProject = project;
    init();
    initValidation();
    setTitle("New App Engine Module");
    setOKButtonText("Generate");
  }

  public String getPackageName() {
    return myPackageName;
  }

  public String getModuleName() {
    return myModuleName;
  }

  public File getTemplate() { return myTemplate; }
  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return myModuleNameField;
  }

  @Override
  protected ValidationInfo doValidate() {
    String moduleName = myModuleNameField.getText().trim();
    String packageName = myPackageNameField.getText().trim();
    if(moduleName.length() == 0) {
      return new ValidationInfo("Module name is empty.", myModuleNameField);
    }
    if(ModuleManager.getInstance(myProject).findModuleByName(moduleName) != null ||
      new File(myProject.getBasePath(), moduleName).exists()) {
      return new ValidationInfo("Module " + moduleName + " already exists.", myModuleNameField);
    }
    if(!JavaPsiFacade.getInstance(myProject).getNameHelper().isQualifiedName(packageName)) {
      return new ValidationInfo("Package name is invalid.", myPackageNameField);
    }
    return null;
  }

  @Override
  protected void doOKAction() {
    myPackageName = myPackageNameField.getText().trim();
    myModuleName = myModuleNameField.getText().trim();
    myTemplate = ((AppEngineTemplates.TemplateInfo) myTemplateBox.getSelectedItem()).getFile();

    super.doOKAction();
  }

  protected void populateTemplates() {
    myTemplateBox.setModel(new DefaultComboBoxModel(AppEngineTemplates.getTemplates().toArray()));
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    populateTemplates();
    createDescriptionPane();
    return myRootPanel;
  }

  protected void createDescriptionPane() {
    myDescriptionPane.setContentType(UIUtil.HTML_MIME);
    myDescriptionPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    myDescriptionPane.setFont(UIManager.getFont("Label.font"));
    myDescriptionPane.setOpaque(false);
    myDescriptionPane.setEditable(false);
    myDescriptionPane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
    myDescriptionPane.setText("<html>" +
                              "<body>" +
                              "Add a Gradle based App Engine module to your Android project. Find more information about the templates " +
                              "<a href='https://github.com/GoogleCloudPlatform/gradle-appengine-templates'>here</a>." +
                              "</body>" +
                              "</html> ");

    myDescriptionPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          BrowserUtil.browse(e.getURL().toString());
        }
      }
    });
  }
}
