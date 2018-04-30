/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.resources;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** This model item represents a single GCP project. */
final class ResourceProjectModelItem extends ProjectModelItem {

  @NotNull private final Project project;
  private String filter;
  private boolean isVisible = true;

  ResourceProjectModelItem(@NotNull Project project) {
    this.project = project;
  }

  /**
   * Returns the HTML text to show in a label for this project model item.
   *
   * <p>If the {@link #filter} is empty, then the text returned is not formatted HTML (i.e. there
   * are no HTML tags). If the {@link #filter} is non-empty, it attempts to bold the first section
   * of the text that matches the filter, otherwise it returns an empty string.
   *
   * <p>You should call {@link #isVisible()} first to determine if this model item should be visible
   * before setting the label's text.
   */
  @NotNull
  String getLabelHtml() {
    String text = getLabelText();
    if (Strings.isNullOrEmpty(filter)) {
      return text;
    }

    // Converts to lowercase to make the filter case-insensitive.
    int beginning = text.toLowerCase().indexOf(filter.toLowerCase());
    if (beginning == -1) {
      return "";
    }

    int end = beginning + filter.length();
    String beforeText = text.substring(0, beginning);
    String matchedText = text.substring(beginning, end);
    String afterText = text.substring(end);
    return String.format("<html>%s<b>%s</b>%s</html>", beforeText, matchedText, afterText);
  }

  /** Returns the {@link Project} that this model item represents. */
  @NotNull
  Project getProject() {
    return project;
  }

  /**
   * Returns {@code true} if this project should be visible, {@code false} otherwise.
   *
   * <p>This is determined by the given {@link #filter} (settable via {@link #setFilter(String)}).
   * If the label's text contains the filter or if the filter is empty, the project should be
   * visible.
   */
  boolean isVisible() {
    return isVisible;
  }

  /**
   * Sets the filter to apply to this project model item.
   *
   * @param filter the filter to apply to the label text. If the given filter is contained in the
   *     label's text, the project should be visible and the matched section is bolded in {@link
   *     #getLabelHtml()}. Otherwise, the project should be hidden.
   */
  void setFilter(@Nullable String filter) {
    this.filter = filter;

    // Converts to lowercase to make the filter case-insensitive.
    isVisible =
        Strings.isNullOrEmpty(filter)
            || getLabelText().toLowerCase().contains(filter.toLowerCase());
  }

  @NotNull
  private String getLabelText() {
    return String.format("%s (%s)", project.getName(), project.getProjectId());
  }
}
