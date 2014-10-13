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

import com.android.annotations.VisibleForTesting;
import com.android.tools.idea.templates.Parameter;
import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.wizard.*;
import com.android.tools.idea.wizard.ScopedStateStore.Key;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.UIUtil;
import icons.GoogleCloudToolsIcons;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.File;
import java.util.Set;

/**
 * Represents a step in the "create a new AppEngine module" wizard process.
 */
public class NewCloudModuleDynamicWizardStep extends DynamicWizardStepWithHeaderAndDescription {
  private static final JBColor CLOUD_HEADER_BACKGROUND_COLOR = new JBColor(0x254A89, 0x254A89);
  private static final JBColor CLOUD_HEADER_TEXT_COLOR = new JBColor(0xFFFFFF, 0xFFFFFF);
  private static final JBColor CLOUD_TITLE_TEXT_COLOR = new JBColor(0x254A89, 0xFFFFFF);
  private static final String HEADER_TITLE_FORMAT = "New %s";
  private static final String BODY_TITLE = "Configure your new AppEngine module";
  private static final String STEP_NAME = "New Cloud Module Step";

  private final Project myProject;
  private JLabel myHeaderTitleLabel;
  private JPanel myRootPane;
  private JTextField myPackageNameField;
  private JTextField myModuleNameField;
  private JComboBox myClientModuleCombo;
  private JEditorPane myModuleDescriptionText;

  public NewCloudModuleDynamicWizardStep(@NotNull Project project, @Nullable Disposable parentDisposable) {
    super(BODY_TITLE, null, null, parentDisposable);
    setBodyComponent(myRootPane);
    myProject = project;
  }

  @Override
  public void init() {
    super.init();
    initClientModuleCombo();
    getMessageLabel().setForeground(JBColor.red);
    register(NewCloudModuleDynamicWizardPath.KEY_MODULE_NAME, myModuleNameField);
    register(NewCloudModuleDynamicWizardPath.KEY_PACKAGE_NAME, myPackageNameField);
    register(NewCloudModuleDynamicWizardPath.KEY_CLIENT_MODULE_NAME, myClientModuleCombo);
  }

  private void initClientModuleCombo() {
    final Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      final AndroidFacet facet = AndroidFacet.getInstance(module);
      if (facet != null && ModuleRootManager.getInstance(module).getContentRoots().length > 0) {
        String moduleNameLabel = module.getName();
        if (facet.getManifest() != null) {
          final String packageName = facet.getManifest().getPackage().getValue();
          if (!Strings.isNullOrEmpty(packageName)) {
            moduleNameLabel += String.format(" (%s)", packageName);
          }
        }
        myClientModuleCombo.addItem(new ComboBoxItem(module.getName(), moduleNameLabel, 1, 1));
      }
    }
  }

  @Override
  public void onEnterStep() {
    super.onEnterStep();
    // Update the module title and description text each time we enter/re-enter the step.
    initHeaderTitleText();
    initModuleDescriptionText();
  }

  private void initHeaderTitleText() {
    final TemplateMetadata metadata = myState.get(NewCloudModuleDynamicWizardPath.KEY_TEMPLATE_METADATA);
    assert metadata != null;
    myHeaderTitleLabel.setText(String.format(HEADER_TITLE_FORMAT, metadata.getTitle()));
  }

  private void initModuleDescriptionText() {
    final TemplateMetadata metadata = myState.get(NewCloudModuleDynamicWizardPath.KEY_TEMPLATE_METADATA);
    assert metadata != null;
    final Parameter docUrlParam = metadata.getParameter(CloudModuleUtils.ATTR_DOC_URL);
    assert docUrlParam != null;
    myModuleDescriptionText.setContentType(UIUtil.HTML_MIME);
    myModuleDescriptionText.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
    myModuleDescriptionText.setFont(UIManager.getFont("Label.font"));
    myModuleDescriptionText.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          BrowserUtil.browse(e.getURL().toString());
        }
      }
    });
    myModuleDescriptionText.setText(String.format("<html><body>Check the <a href='%s'>\"%s\"</a> documentation for more " +
                                                  "information about the contents of this backend module, and for " +
                                                  "detailed instructions about connecting your Android app to this " +
                                                  "backend.</body></html>", docUrlParam.initial, metadata.getTitle()));
  }

  @Override
  public void deriveValues(Set<Key> modified) {
    super.deriveValues(modified);
    // TODO: should we auto-fill the package name when the user starts typing in a module name? if so, then how?
  }

  @Override
  public boolean validate() {
    final String moduleName = myState.get(NewCloudModuleDynamicWizardPath.KEY_MODULE_NAME);
    final String packageName = myState.get(NewCloudModuleDynamicWizardPath.KEY_PACKAGE_NAME);
    final String clientModuleName = myState.get(NewCloudModuleDynamicWizardPath.KEY_CLIENT_MODULE_NAME);
    assert moduleName != null && packageName != null;
    if (moduleName.isEmpty()) {
      setErrorHtml("Please enter a module name.");
      return false;
    }
    if (!isValidModuleName(moduleName)) {
      setErrorHtml("Please enter a valid module name.");
      return false;
    }
    if (ModuleManager.getInstance(myProject).findModuleByName(moduleName) != null ||
        new File(myProject.getBasePath(), moduleName).exists()) {
      setErrorHtml(String.format("Module %s already exists.", moduleName));
      return false;
    }
    if (packageName.isEmpty()) {
      setErrorHtml("Please enter a package name.");
      return false;
    }
    if (!PsiNameHelper.getInstance(myProject).isQualifiedName(packageName)) {
      setErrorHtml("Please enter a valid package name.");
      return false;
    }
    if (Strings.isNullOrEmpty(clientModuleName)) {
      setErrorHtml("Please select a client module.");
      return false;
    }
    setErrorHtml("");
    return true;
  }

  @VisibleForTesting
  static boolean isValidModuleName(@NotNull String moduleName) {
    if (!moduleName.replaceAll(WizardConstants.INVALID_FILENAME_CHARS, "").equals(moduleName)) {
      return false;
    }
    for (String s : Splitter.on('.').split(moduleName)) {
      if (WizardConstants.INVALID_WINDOWS_FILENAMES.contains(s.toLowerCase())) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  @Override
  public String getStepName() {
    return STEP_NAME;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myModuleNameField;
  }

  @Nullable
  @Override
  protected JBColor getTitleTextColor() {
    return CLOUD_TITLE_TEXT_COLOR;
  }

  @Nullable
  @Override
  protected JComponent getHeader() {
    // The third argument is the empty string intentionally... it will be set to its appropriate value in onEnterStep().
    return createHeader(CLOUD_HEADER_BACKGROUND_COLOR, GoogleCloudToolsIcons.CLOUD_72x64, "");
  }

  /**
   * Copied from {@link DynamicWizardStep} and slightly modified so that we can keep a reference to
   * the title JLabel (which needs to be updated each time the user enters the step).
   */
  private JPanel createHeader(JBColor headerColor, Icon icon, String title) {
    final JPanel panel = new JPanel();
    panel.setBackground(headerColor);
    panel.setBorder(new EmptyBorder(WizardConstants.STUDIO_WIZARD_INSETS));
    panel.setLayout(new GridLayoutManager(2, 2, new Insets(18, 0, 12, 0), 2, 2));
    panel.add(new ImageComponent(icon),
              new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                  GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(72, 64), null));
    final GridConstraints constraints = new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_HORIZONTAL,
                                                            GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                            GridConstraints.SIZEPOLICY_FIXED, null, null, null);
    myHeaderTitleLabel = new JLabel(title);
    myHeaderTitleLabel.setForeground(CLOUD_HEADER_TEXT_COLOR);
    myHeaderTitleLabel.setFont(myHeaderTitleLabel.getFont().deriveFont(24f));
    myHeaderTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    panel.add(myHeaderTitleLabel, constraints);
    constraints.setRow(1);
    constraints.setAnchor(GridConstraints.ANCHOR_NORTHWEST);
    final JLabel productLabel = new JLabel("Android Studio");
    productLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    productLabel.setForeground(CLOUD_HEADER_TEXT_COLOR);
    panel.add(productLabel, constraints);
    return panel;
  }
}