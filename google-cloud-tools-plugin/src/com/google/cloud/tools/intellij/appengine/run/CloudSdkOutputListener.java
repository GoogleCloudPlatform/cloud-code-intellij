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

package com.google.cloud.tools.intellij.appengine.run;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;

/**
 * Created by joaomartins on 7/25/16.
 */
public class CloudSdkOutputListener implements ProcessOutputLineListener {
  private ConsoleView consoleView;

  public CloudSdkOutputListener(Project project) {
    consoleView = new ConsoleViewImpl(project, false);
  }

  @Override
  public void onOutputLine(String line) {
    if (consoleView != null) {
      consoleView.print(line + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }
  }

  public ConsoleView getConsoleView() {
    return consoleView;
  }
}
