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

import org.jetbrains.annotations.Nullable;

/**
 * Modal project and account selection dialog. Contains account drop-down with user list, table with
 * project list and simple filter. {@link ProjectSelector} calls {@link #showDialog(CloudProject)}.
 */
// TODO(ivanporty) implementation in the following PRs
public class ProjectSelectionDialog {

  /**
   * Creates and shows modal dialog to select project/account. Blocks EDT until choice is made.
   *
   * @param cloudProject Current project selection to populate dialog UI state.
   * @return New project selection or null if user cancels.
   */
  @Nullable
  CloudProject showDialog(@Nullable CloudProject cloudProject) {
    return cloudProject;
  }
}
