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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.services.cloudresourcemanager.model.Project;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/** tests for {@link ProjectListTableModel} */
public class ProjectListTableModelTest {
  private ProjectListTableModel model;
  private Project testProject1, testProject2;

  @Before
  public void setUp() {
    model = new ProjectListTableModel();
    testProject1 = new Project();
    testProject1.setName("test-project");
    testProject1.setProjectId("test-project-ID");
    testProject2 = new Project();
    testProject2.setName("test-project-2");
    testProject2.setProjectId("test-project-2-ID");
  }

  @Test
  public void starts_Empty() {
    assertThat(model.getColumnCount()).isEqualTo(2);
    assertThat(model.getRowCount()).isEqualTo(0);
  }

  @Test
  public void setProjectList() {
    model.setProjectList(Arrays.asList(testProject1, testProject2));

    assertThat(model.getRowCount()).isEqualTo(2);
  }

  @Test
  public void returns_validProjectName() {
    model.setProjectList(Arrays.asList(testProject1, testProject2));

    assertThat(model.getProjectNameAtRow(0)).isEqualTo(testProject1.getName());
    assertThat(model.getProjectNameAtRow(1)).isEqualTo(testProject2.getName());
  }

  @Test
  public void returns_validProjectId() {
    model.setProjectList(Arrays.asList(testProject1, testProject2));

    assertThat(model.getProjectIdAtRow(0)).isEqualTo(testProject1.getProjectId());
    assertThat(model.getProjectIdAtRow(1)).isEqualTo(testProject2.getProjectId());
  }

  @Test
  public void setProjectList_clearsPreviousState() {
    model.setProjectList(Arrays.asList(testProject1, testProject2));
    model.setProjectList(Collections.singletonList(testProject2));

    assertThat(model.getRowCount()).isEqualTo(1);
    assertThat(model.getProjectNameAtRow(0)).isEqualTo(testProject2.getName());
  }
}
