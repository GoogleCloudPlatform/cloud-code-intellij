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
package com.google.gct.idea.cloudsave;

import com.android.tools.idea.templates.Parameter;
import com.android.tools.idea.wizard.ScopedDataBinder;
import com.android.tools.idea.wizard.WizardParameterFactory;
import com.google.gct.idea.elysium.ProjectSelector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;

/**
 * The Android Studio dynamic wizard allows custom UI to be created by registering this factory.
 */
public class CloudWizardParameterFactory implements WizardParameterFactory {
  private static final String GOOGLE_PROJECT_SELECTOR_TYPENAME = "GoogleProjectSelector";

  @Override
  public String[] getSupportedTypes() {
    return new String[]{GOOGLE_PROJECT_SELECTOR_TYPENAME};
  }

  @Override
  public JComponent createComponent(String type, Parameter parameter) {
    if (!GOOGLE_PROJECT_SELECTOR_TYPENAME.equals(type)) {
      throw new IllegalArgumentException("type");
    }
    return new ProjectSelector(true);
  }

  @Override
  public ScopedDataBinder.ComponentBinding<String, JComponent> createBinding(JComponent component, Parameter parameter) {
    return new ProjectSelectorWizardBinding();
  }

  private static class ProjectSelectorWizardBinding extends ScopedDataBinder.ComponentBinding<String, JComponent> {
    @Override
    public void setValue(@Nullable String newValue, @NotNull JComponent component) {
      ((ProjectSelector)component).setText(newValue);
    }

    @Override
    public String getValue(@NotNull JComponent component) {
      return ((ProjectSelector)component).getText();
    }

    @Override
    public Document getDocument(@NotNull JComponent component) {
      return ((ProjectSelector)component).getDocument();
    }
  }
}
