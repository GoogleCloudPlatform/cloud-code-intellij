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

import static com.google.cloud.tools.intellij.testing.TestUtils.expectThrows;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.ImmutableList;
import java.awt.Image;
import java.io.IOException;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

@RunWith(JUnit4.class)
public final class GoogleUserModelItemTest {

  private static final Project PROJECT_A = new Project();
  private static final Project PROJECT_B = new Project();

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @TestService @Mock private GoogleApiClientFactory mockGoogleApiClientFactory;

  @Mock private CredentialedUser mockUser;
  @Mock private Image mockImage;
  @Mock private CloudResourceManager mockCloudResourceManager;
  @Mock private CloudResourceManager.Projects mockProjects;
  @Mock private CloudResourceManager.Projects.List mockList;

  private GoogleUserModelItem modelItem;

  @BeforeClass
  public static void setUpProjects() {
    PROJECT_A.setName("Project A");
    PROJECT_A.setProjectId("project-a");
    PROJECT_B.setName("Project B");
    PROJECT_B.setProjectId("project-b");
  }

  @Before
  public void setUpModelItem() {
    when(mockGoogleApiClientFactory.getCloudResourceManagerClient(any()))
        .thenReturn(mockCloudResourceManager);
    modelItem =
        new GoogleUserModelItem(mockUser, new DefaultTreeModel(new DefaultMutableTreeNode()));
  }

  @Test
  public void newInstance_doesAddLoadingModelItem() {
    assertThat(modelItem.getChildCount()).isEqualTo(1);
    assertThat(modelItem.getChildAt(0)).isInstanceOf(ResourceLoadingModelItem.class);
  }

  @Test
  public void getCredentialedUser_doesReturnUser() {
    assertThat(modelItem.getCredentialedUser()).isEqualTo(mockUser);
  }

  @Test
  public void getEmail_doesReturnUserEmail() {
    String email = "foo@example.com";
    when(mockUser.getEmail()).thenReturn(email);

    assertThat(modelItem.getEmail()).isEqualTo(email);
  }

  @Test
  public void getName_doesReturnUserName() {
    String name = "Jane Smith";
    when(mockUser.getName()).thenReturn(name);

    assertThat(modelItem.getName()).isEqualTo(name);
  }

  @Test
  public void getImage_doesReturnUserImage() {
    when(mockUser.getPicture()).thenReturn(mockImage);

    assertThat(modelItem.getImage()).isEqualTo(mockImage);
  }

  @Test
  public void isSynchronizing_doesReturnFalse() {
    assertThat(modelItem.isSynchronizing()).isFalse();
  }

  @Test
  public void getChildCount_withEmptyFilter_doesReturnAllChildren() {
    mockListProjectsResponse(ImmutableList.of(PROJECT_A, PROJECT_B));

    modelItem.synchronize();
    modelItem.setFilter("");

    // 2 projects + 1 new project item = 3 children
    assertThat(modelItem.getChildCount()).isEqualTo(3);
  }

  @Test
  public void getChildCount_withMatchingFilter_doesReturnFilteredChildren() {
    mockListProjectsResponse(ImmutableList.of(PROJECT_A, PROJECT_B));

    modelItem.synchronize();
    modelItem.setFilter("project a");

    // 2 projects - 1 not matching project + 1 new project item = 2 children
    assertThat(modelItem.getChildCount()).isEqualTo(2);
  }

  @Test
  public void getChildCount_withNotMatchingFilter_doesReturnFilteredChildren() {
    mockListProjectsResponse(ImmutableList.of(PROJECT_A, PROJECT_B));

    modelItem.synchronize();
    modelItem.setFilter("does not match");

    // 2 projects - 2 not matching projects + 1 new project item = 1 child
    assertThat(modelItem.getChildCount()).isEqualTo(1);
  }

  @Test
  public void getChildAt_withEmptyFilter_doesReturnAllChildren() {
    mockListProjectsResponse(ImmutableList.of(PROJECT_A, PROJECT_B));

    modelItem.synchronize();
    modelItem.setFilter("");

    // 2 projects + 1 new project item = 3 children
    TreeNode projectA = modelItem.getChildAt(0);
    TreeNode projectB = modelItem.getChildAt(1);
    TreeNode newProject = modelItem.getChildAt(2);
    expectThrows(ArrayIndexOutOfBoundsException.class, () -> modelItem.getChildAt(3));

    assertThat(projectA).isInstanceOf(ResourceProjectModelItem.class);
    assertThat(((ResourceProjectModelItem) projectA).getProject()).isEqualTo(PROJECT_A);
    assertThat(projectB).isInstanceOf(ResourceProjectModelItem.class);
    assertThat(((ResourceProjectModelItem) projectB).getProject()).isEqualTo(PROJECT_B);
    assertThat(newProject).isInstanceOf(ResourceNewProjectModelItem.class);
  }

  @Test
  public void getChildAt_withMatchingFilter_doesReturnFilteredChildren() {
    mockListProjectsResponse(ImmutableList.of(PROJECT_A, PROJECT_B));

    modelItem.synchronize();
    modelItem.setFilter("project b");

    // 2 projects - 1 not matching project + 1 new project item = 2 children
    TreeNode projectB = modelItem.getChildAt(0);
    TreeNode newProject = modelItem.getChildAt(1);
    expectThrows(ArrayIndexOutOfBoundsException.class, () -> modelItem.getChildAt(2));

    assertThat(projectB).isInstanceOf(ResourceProjectModelItem.class);
    assertThat(((ResourceProjectModelItem) projectB).getProject()).isEqualTo(PROJECT_B);
    assertThat(newProject).isInstanceOf(ResourceNewProjectModelItem.class);
  }

  @Test
  public void getChildAt_withNotMatchingFilter_doesReturnFilteredChildren() {
    mockListProjectsResponse(ImmutableList.of(PROJECT_A, PROJECT_B));

    modelItem.synchronize();
    modelItem.setFilter("does not match");

    // 2 projects - 2 not matching projects + 1 new project item = 1 child
    TreeNode newProject = modelItem.getChildAt(0);
    expectThrows(ArrayIndexOutOfBoundsException.class, () -> modelItem.getChildAt(1));

    assertThat(newProject).isInstanceOf(ResourceNewProjectModelItem.class);
  }

  /**
   * Mocks the {@link ListProjectsResponse} returned by the {@link #mockCloudResourceManager}.
   *
   * @param projects the list of {@link Project Projects} to return as part of the {@link
   *     ListProjectsResponse}
   */
  private void mockListProjectsResponse(List<Project> projects) {
    when(mockCloudResourceManager.projects()).thenReturn(mockProjects);

    try {
      when(mockProjects.list()).thenReturn(mockList);
      when(mockList.setPageSize(any())).thenReturn(mockList);
      when(mockList.setPageToken(any())).thenReturn(mockList);

      ListProjectsResponse response = new ListProjectsResponse();
      response.setProjects(projects);
      response.setNextPageToken("");
      when(mockList.execute()).thenReturn(response);
    } catch (IOException e) {
      // Should not happen when setting up these mocks.
      throw new AssertionError(e);
    }
  }
}
