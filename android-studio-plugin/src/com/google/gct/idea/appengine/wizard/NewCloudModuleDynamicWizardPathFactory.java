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
package com.google.gct.idea.appengine.wizard;

import com.android.tools.idea.npw.NewModuleDynamicPath;
import com.android.tools.idea.npw.NewModuleDynamicPathFactory;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Factory used by the New Module wizard framework. Creates and returns a collection
 * containing our cloud module wizard path to be displayed.
 */
public class NewCloudModuleDynamicWizardPathFactory implements NewModuleDynamicPathFactory {
  @Override
  public Collection<NewModuleDynamicPath> createWizardPaths(@Nullable Project project, @NotNull Disposable disposable) {
    if (project == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of((NewModuleDynamicPath)new NewCloudModuleDynamicWizardPath());
  }
}
