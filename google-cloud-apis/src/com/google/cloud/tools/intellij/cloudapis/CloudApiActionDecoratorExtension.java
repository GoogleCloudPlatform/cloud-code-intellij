/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * An extension point to dynamically change behaivor of "Add Cloud APIs" action, i.e. check for
 * eligibility of specific build systems or frameworks and disable/enable/modify text based on that.
 */
public interface CloudApiActionDecoratorExtension {
  ExtensionPointName<CloudApiActionDecoratorExtension> EP_NAME =
      new ExtensionPointName<>("com.google.gct.cloudapis.cloudApiActionDecorator");

  /**
   * Called by core Cloud API UI to allow extensions to "decorate" default behaivor of Cloud API
   * action, which by default is always enabled. Called by IDE platform on each project change.
   */
  void decorate(AnActionEvent e);
}
