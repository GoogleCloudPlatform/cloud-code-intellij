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
package com.google.gct.idea.samples;

import com.android.tools.idea.wizard.*;
import com.appspot.gsamplesindex.samplesindex.model.Sample;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static com.google.gct.idea.samples.SampleImportWizardPath.SAMPLE_NAME;
import static com.google.gct.idea.samples.SampleImportWizardPath.SAMPLE_DIR;
import static com.google.gct.idea.samples.SampleImportWizardPath.SAMPLE_KEY;
import static com.google.gct.idea.samples.SampleImportWizardPath.SAMPLE_URL;
import static com.android.tools.idea.wizard.ScopedStateStore.Key;

/**
 * SampleSetupStep is the second/final page in the Sample Import wizard that sets project/module name, location, and other project-global
 * parameters.
 */
public class SampleSetupStep extends DynamicWizardStepWithHeaderAndDescription {
  private JTextField myProjectNameField;
  private HyperlinkLabel myUrlField;
  private TextFieldWithBrowseButton myProjectLocationField;
  private JPanel myPanel;

  private static final String DEFAULT_SAMPLE_NAME = GctBundle.message("sample.default.name");

  public SampleSetupStep(Disposable parentDisposable) {
    super(GctBundle.message("sample.setup.title"), GctBundle.message("sample.setup.subtitle"), parentDisposable);
    setBodyComponent(myPanel);
  }

  @Override
  public void init() {
    super.init();
    myProjectLocationField.addBrowseFolderListener(GctBundle.message("select.project.location"), null, null,
                                                   FileChooserDescriptorFactory.createSingleFolderDescriptor());
    String sampleName = getUniqueName(DEFAULT_SAMPLE_NAME);
    myState.put(SAMPLE_NAME, sampleName);
    myState.put(SAMPLE_DIR, getFileLocation(sampleName).getAbsolutePath());
    register(SAMPLE_NAME, myProjectNameField);
    register(SAMPLE_DIR, myProjectLocationField);
    register(SAMPLE_URL, myUrlField, new ComponentBinding<String, HyperlinkLabel>() {
      @Override
      public void setValue(@Nullable String newValue, @NotNull HyperlinkLabel component) {
        component.setHyperlinkTarget(newValue);
        newValue = (newValue == null) ? "" : newValue;
        component.setHyperlinkText(newValue);
      }
    });
    registerValueDeriver(SAMPLE_NAME, new SampleNameValueDeriver());
    registerValueDeriver(SAMPLE_DIR, new SampleDirValueDeriver());
  }

  private static String getUniqueName(String projectName) {
    File file = getFileLocation(projectName);
    String name = projectName;
    int i = 0;
    while (file.exists()) {
      i++;
      name = projectName + i;
      file = getFileLocation(name);
    }
    return name;
  }

  private static File getFileLocation(String projectName){
    return new File(NewProjectWizardState.getProjectFileDirectory(), projectName.replaceAll("[^a-zA-Z0-9_\\-.]", ""));
  }

  @Override
  public boolean validate() {
    String applicationName = myState.get(SAMPLE_NAME);
    if (applicationName == null || applicationName.trim().isEmpty()) {
      setErrorHtml(GctBundle.message("application.name.not.set"));
      return false;
    }
    String sampleDir = myState.get(SAMPLE_DIR);
    WizardUtils.ValidationResult validationResult = WizardUtils.validateLocation(sampleDir);
    setErrorHtml(validationResult.isOk() ? "" : validationResult.getFormattedMessage());
    return !validationResult.isError();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myProjectNameField;
  }

  @NotNull
  @Override
  public String getStepName() {
    return GctBundle.message("sample.setup.title");
  }

  private static class SampleNameValueDeriver extends ValueDeriver<String> {
    @Nullable
    @Override
    public Set<Key<?>> getTriggerKeys() {
      Set<Key<?>> filterKeys = new HashSet<Key<?>>(1);
      filterKeys.add(SAMPLE_KEY);
      return filterKeys;
    }

    @Nullable
    @Override
    public String deriveValue(@NotNull ScopedStateStore state, Key changedKey, @Nullable String currentValue) {
      Sample sample = state.get(SAMPLE_KEY);
      if (sample == null) {
        return "";
      }
      String sampleName = sample.getTitle();
      return getUniqueName(StringUtil.isEmpty(sampleName) ? DEFAULT_SAMPLE_NAME : sampleName);
    }
  }

  private static class SampleDirValueDeriver extends ValueDeriver<String> {
    @Nullable
    @Override
    public Set<Key<?>> getTriggerKeys() {
      Set<Key<?>> filterKeys = new HashSet<Key<?>>(1);
      filterKeys.add(SAMPLE_NAME);
      return filterKeys;
    }

    @Nullable
    @Override
    public String deriveValue(@NotNull ScopedStateStore state, Key changedKey, @Nullable String currentValue) {
      String name = state.get(SAMPLE_NAME);
      if (name == null) {
        return "";
      }
      return getFileLocation(name).getAbsolutePath();
    }
  }

  @NotNull
  @Override
  protected WizardStepHeaderSettings getStepHeader() {
    return WizardStepHeaderSettings.createProductHeader(GctBundle.message("sample.import.title"));
  }
}
