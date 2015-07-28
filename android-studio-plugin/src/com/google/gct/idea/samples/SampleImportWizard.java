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

import com.android.tools.idea.wizard.dynamic.DynamicWizard;
import com.appspot.gsamplesindex.samplesindex.model.SampleCollection;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import icons.AndroidIcons;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Presents a wizard to the user to import hosted samples
 */
public class SampleImportWizard extends DynamicWizard {

  private SampleCollection mySampleList;

  public SampleImportWizard(@Nullable Project project, @NotNull SampleCollection samples) {
    super(project, null, GctBundle.message("sample.import.title"));
    mySampleList = samples;
    init();

  }
  @Override
  public void init() {
    if (!AndroidSdkUtils.isAndroidSdkAvailable()) {
      String title = "SDK problem";
      String msg =
        "<html>Your Android SDK is missing or out of date.<br>" +
        "You can configure your SDK via <b>Configure | Project Defaults | Project Structure | SDKs</b></html>";
      super.init();
      Messages.showErrorDialog(msg, title);
      return;
    }

    addPath(new SampleImportWizardPath(mySampleList, getDisposable()));
    super.init();
  }

  /**
   * @return optional wizard icon that will be shown on every page title.
   */
  @Nullable
  @Override
  public Icon getIcon() {
    return AndroidIcons.Wizards.NewProjectMascotGreen;
  }

  @Override
  public void performFinishingActions() {
    // our single path is performing the finishing actions
  }

  @NotNull
  protected String getProgressTitle() {
    return GctBundle.getString("sample.import.progress.title");
  }

  @Override
  public String getWizardActionDescription() {
    return GctBundle.message("sample.import.title");
  }
}
