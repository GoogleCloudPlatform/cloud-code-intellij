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

package com.google.cloud.tools.intellij.cloudapis.maven;

import com.google.cloud.tools.intellij.cloudapis.CloudApiActionDecoratorExtension;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class MavenCloudApisActionDecoratorExtension implements CloudApiActionDecoratorExtension {

  @Override
  public boolean decorate(AnActionEvent e) {
    // check if we have any Maven modules available in the project and then update wording.
    if (MavenUtils.hasAnyMavenModules(e.getProject())) {
      e.getPresentation()
          .setText(
              MavenCloudApisMessageBundle.message("maven.cloud.libraries.menu.action.active.text"));
      e.getPresentation()
          .setDescription(
              MavenCloudApisMessageBundle.message("maven.cloud.libraries.menu.action.description"));
    }

    // no need to interrupt decoration since action might still be disabled by incompatible
    // libraries or language level
    return false;
  }
}
