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

package com.google.cloud.tools.intellij.resources;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.services.cloudresourcemanager.model.Project;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ResourceProjectModelItem}. */
@RunWith(JUnit4.class)
public final class ResourceProjectModelItemTest {

  private static final String PROJECT_NAME = "Project Name";
  private static final String PROJECT_ID = "project-id";

  private final ResourceProjectModelItem modelItem = new ResourceProjectModelItem(new Project());

  @Before
  public void setUpProject() {
    modelItem.getProject().setName(PROJECT_NAME);
    modelItem.getProject().setProjectId(PROJECT_ID);
  }

  @Test
  public void getLabelHtml_withEmptyFilter_doesReturnLabelText() {
    assertThat(modelItem.getLabelHtml()).isEqualTo("Project Name (project-id)");
  }

  @Test
  public void getLabelHtml_withMatchingFilter_doesReturnFormattedHtml() {
    modelItem.setFilter("project-id");

    assertThat(modelItem.getLabelHtml()).isEqualTo("<html>Project Name (<b>project-id</b>)</html>");
  }

  @Test
  public void getLabelHtml_withMatchingFilter_isCaseInsensitive() {
    modelItem.setFilter("PrOjEcT-iD");

    assertThat(modelItem.getLabelHtml()).isEqualTo("<html>Project Name (<b>project-id</b>)</html>");
  }

  @Test
  public void getLabelHtml_withMatchingFilter_andMultipleMatches_doesFormatFirstMatch() {
    modelItem.setFilter("project");

    assertThat(modelItem.getLabelHtml()).isEqualTo("<html><b>Project</b> Name (project-id)</html>");
  }

  @Test
  public void getLabelHtml_withNonMatchingFilter_doesReturnEmptyString() {
    modelItem.setFilter("does not match");

    assertThat(modelItem.getLabelHtml()).isEmpty();
  }

  @Test
  public void isVisible_withEmptyFilter_doesReturnTrue() {
    assertThat(modelItem.isVisible()).isTrue();
  }

  @Test
  public void isVisible_withMatchingFilter_doesReturnTrue() {
    modelItem.setFilter("project-id");

    assertThat(modelItem.isVisible()).isTrue();
  }

  @Test
  public void isVisible_withMatchingFilter_isCaseInsensitive() {
    modelItem.setFilter("PrOjEcT-iD");

    assertThat(modelItem.isVisible()).isTrue();
  }

  @Test
  public void isVisible_withNonMatchingFilter_doesReturnFalse() {
    modelItem.setFilter("does not match");

    assertThat(modelItem.isVisible()).isFalse();
  }
}
