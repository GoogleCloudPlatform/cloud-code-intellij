/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.facet;

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HyperlinkLabel;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Stackdriver facet's user interface.
 */
public class StackdriverPanel extends FacetEditorTab {

  private JPanel stackdriverPanel;
  private JCheckBox generateSourceContext;
  private JCheckBox ignoreErrors;
  private FacetEditorContext editorContext;
  private HyperlinkLabel stackdriverInfo;
  private StackdriverFacetConfiguration configuration;

  /**
   * Used from the New Project/Module dialog, through {@link StackdriverSupportProvider}, where a
   * {@link FacetEditorContext} isn't available, but also doesn't use
   * {@link StackdriverPanel#apply()} to persist the configuration.
   */
  public StackdriverPanel() {
    this(null /* editorContext */);
    generateSourceContext.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ignoreErrors.setEnabled(((JCheckBox)e.getSource()).isSelected());
      }
    });
  }

  /**
   * Used from the Facets -> Add dialog.
   *
   * @param editorContext may contain the Stackdriver facet in the current context
   */
  public StackdriverPanel(FacetEditorContext editorContext) {
    this.editorContext = editorContext;
    if (editorContext != null) {
      configuration = (StackdriverFacetConfiguration) editorContext.getFacet().getConfiguration();
    }
    stackdriverInfo.setHyperlinkText("Google Stackdriver documentation");
    stackdriverInfo.setHyperlinkTarget("https://cloud.google.com/stackdriver/");
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return stackdriverPanel;
  }

  @Override
  public boolean isModified() {
    return isGenerateSourceContextSelected() != configuration.getState().isGenerateSourceContext()
        || isIgnoreErrorsSelected() != configuration.getState().isIgnoreErrors();
  }

  @Override
  public void apply() throws ConfigurationException {
    configuration.getState().setGenerateSourceContext(isGenerateSourceContextSelected());
    configuration.getState().setIgnoreErrors(isIgnoreErrorsSelected());
  }

  @Override
  public void reset() {
    if (editorContext.getFacet().getConfiguration() instanceof StackdriverFacetConfiguration) {
      generateSourceContext.setSelected(configuration.getState().isGenerateSourceContext());
      ignoreErrors.setEnabled(isGenerateSourceContextSelected());
      ignoreErrors.setSelected(configuration.getState().isIgnoreErrors());
      return;
    }
    // If not a StackdriverFacetConfiguration, use defaults, though this should never happen.
    generateSourceContext.setSelected(true);
    ignoreErrors.setSelected(true);
  }

  @Override
  public void disposeUIResources() {
    // Do nothing.
  }

  @Nls
  @Override
  public String getDisplayName() {
    return GctBundle.getString("stackdriver.name");
  }

  public boolean isGenerateSourceContextSelected() {
    return generateSourceContext.isSelected();
  }

  public boolean isIgnoreErrorsSelected() {
    return ignoreErrors.isSelected();
  }
}
