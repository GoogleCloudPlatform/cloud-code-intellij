/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.project;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.auto.value.AutoValue;
import org.jetbrains.annotations.Nullable;

/** GCP project and account. */
@AutoValue
public abstract class CloudProject {
  public static CloudProject create(String projectName, String projectId, String googleUsername) {
    return new AutoValue_CloudProject(projectName, projectId, null /* unset */, googleUsername);
  }

  public static CloudProject create(
      String projectName, String projectId, Long projectNumber, String googleUsername) {
    return new AutoValue_CloudProject(projectName, projectId, projectNumber, googleUsername);
  }

  /** See {@link Project#getName()}. */
  public abstract String projectName();

  /** See {@link Project#getProjectId()}. */
  public abstract String projectId();

  /** See {@link Project#getProjectNumber()}. */
  @Nullable
  public abstract Long projectNumber();

  public abstract String googleUsername();
}
