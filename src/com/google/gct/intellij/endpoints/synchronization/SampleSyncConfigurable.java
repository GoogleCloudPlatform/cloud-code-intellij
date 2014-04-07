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
package com.google.gct.intellij.endpoints.synchronization;


import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * The Sample Templates Selection in the Google Cloud Tools setting tab.
 */
public class SampleSyncConfigurable implements Configurable {
  private JPanel myPanel;
  private JRadioButton builtInSampleRadioButton;
  private JRadioButton repoRadioButton;

  @Nls
  @Override
  public String getDisplayName() {
    return "Google Cloud Tools";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if(SampleSyncConfiguration.usingBuiltInSamples()) {
      builtInSampleRadioButton.setSelected(true);
    } else {
      repoRadioButton.setSelected(true);
    }

    return (JComponent) myPanel;
  }

  @Override
  public boolean isModified() {
    return true;
  }

  @Override
  public void apply() throws ConfigurationException {
    // Save the user selected setting
    SampleSyncConfiguration.setUseBuiltInSamples(builtInSampleRadioButton.isSelected());
  }


  @Override
  public void reset() {
    if(SampleSyncConfiguration.usingBuiltInSamples()) {
      builtInSampleRadioButton.setSelected(true);
    } else {
      repoRadioButton.setSelected(true);
    }
  }

  @Override
  public void disposeUIResources() {
    // do nothing
  }
}
