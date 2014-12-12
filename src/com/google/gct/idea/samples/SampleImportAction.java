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

import com.appspot.gsamplesindex.samplesindex.SamplesIndex;
import com.appspot.gsamplesindex.samplesindex.model.SampleCollection;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action that initiates the Sample Import Wizard, it will also download the samples list from the samples service and pass
 * it as a paramter to the Sample Wizard.
 */
public class SampleImportAction extends AnAction {

  private static final Logger LOG = Logger.getInstance(SampleImportAction.class);

  @Override
  public void actionPerformed(AnActionEvent e) {

    // TODO: perhaps this should go somewhere else
    final SamplesIndex samplesService;
    SamplesIndex.Builder myBuilder = new SamplesIndex.Builder(new NetHttpTransport(), new JacksonFactory(), null);
    samplesService = myBuilder.build();

    final AtomicReference<SampleCollection> sampleList = new AtomicReference<SampleCollection>(null);
    new Task.Modal(null, GctBundle.message("sample.import.title"), false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText(GctBundle.message("sample.index.downloading"));
        try {
          sampleList.set(samplesService.samples().listSamples().set("technology", "android").execute());
        }
        catch (IOException ex) {
          LOG.warn(GctBundle.message("sample.index.download.failed"));
          sampleList.set(null);
        }
      }
    }.queue();

    if (sampleList.get() == null || sampleList.get().size() == 0) {
      Messages.showErrorDialog(GctBundle.message("sample.index.download.failed"), GctBundle.message("sample.import.error.title"));
      return;
    }

    SampleImportWizard wizard = new SampleImportWizard(null, sampleList.get());
    wizard.show();
  }
}
