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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.project.ProjectLoader.ProjectLoaderResultCallback;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.util.GctBundle;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

/** Tests for {@link ProjectLoader} */
public class ProjectLoaderTest {
  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private ProjectLoader projectLoader;
  @Mock private ProjectLoaderResultCallback mockResultCallback;
  @TestService @Mock private GoogleApiClientFactory mockGoogleApiClientFactory;
  @Mock private CloudResourceManager mockCloudResourceManager;
  @Mock private CloudResourceManager.Projects mockProjects;
  @Mock private CloudResourceManager.Projects.List mockList;
  @Mock private CredentialedUser mockUser;

  private Project testProject1, testProject2;

  @Before
  public void setUp() {
    projectLoader = new ProjectLoader();
    when(mockGoogleApiClientFactory.getCloudResourceManagerClient(any()))
        .thenReturn(mockCloudResourceManager);
    testProject1 = new Project();
    testProject1.setName("test-project");
    testProject1.setProjectId("test-project-ID");
    testProject2 = new Project();
    testProject2.setName("test-project-2");
    testProject2.setProjectId("test-project-2-ID");
  }

  @Test
  public void loadUserProjects_invokesCallback() {
    List<Project> projects = Arrays.asList(testProject1, testProject2);
    mockListProjectsResponse(projects);

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback).projectListReady(projects);
  }

  @Test
  public void nullProjectList_invokes_callback_withEmptyList() {
    mockListProjectsResponse(null);

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback).projectListReady(Collections.emptyList());
  }

  @Test
  public void loadUserProjects_sortsProjects_byName() {
    Project aProject = new Project();
    aProject.setName("a project");
    aProject.setProjectId("The ID");
    mockListProjectsResponse(Arrays.asList(testProject1, testProject2, aProject));

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback)
        .projectListReady(Arrays.asList(aProject, testProject1, testProject2));
  }

  @Test
  public void deletedProjects_filteredFromResult() {
    List<Project> projects = Arrays.asList(testProject1, testProject2);
    testProject1.setLifecycleState("DELETE_REQUESTED");
    mockListProjectsResponse(projects);

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    List<Project> expectedList = Collections.singletonList(testProject2);
    verify(mockResultCallback).projectListReady(expectedList);
  }

  @Test
  public void projects_withEmptyId_filteredFromResult() {
    List<Project> projects = Arrays.asList(testProject1, testProject2);
    testProject1.setProjectId(null);
    testProject2.setProjectId("");
    mockListProjectsResponse(projects);

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback).projectListReady(Collections.emptyList());
  }

  @Test
  public void ioError_resultsIn_valid_errorMessage() throws IOException {
    mockListProjectsResponse(null);
    when(mockList.execute()).thenThrow(new IOException("IO issue"));

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback)
        .onError(GctBundle.getString("project.selector.loader.couldnotconnect"));
  }

  @Test
  public void runtimeException_resultsIn_exceptionMessage() throws IOException {
    // remove IDEA logging which invokes AssertionError.
    projectLoader = spy(new ProjectLoader());
    doNothing().when(projectLoader).logError(any(), any());
    mockListProjectsResponse(null);
    String errorMessage = "Internal error";
    when(mockList.execute()).thenThrow(new RuntimeException(errorMessage));

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback).onError(ArgumentMatchers.contains(errorMessage));
  }

  @Test
  public void multiPage_projectList_merged_correctly() throws IOException {
    mockTwoPageProjectsRespose("");

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    verify(mockResultCallback).projectListReady(Arrays.asList(testProject1, testProject2));
  }

  @Test
  public void multiPage_projectList_uses_valid_Tokens() throws IOException {
    ArgumentCaptor<String> pageTokenCaptor = mockTwoPageProjectsRespose("token");

    projectLoader.loadUserProjectsInBackground(mockUser, mockResultCallback);

    // check page token is called only for tokens returned in responses.
    assertThat(pageTokenCaptor.getAllValues().size()).isEqualTo(1);
    assertThat(pageTokenCaptor.getAllValues().get(0)).isEqualTo("token");
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

  /**
   * Mocks {@link ListProjectsResponse} to contain two pages, one test project per each, using the
   * specified token.
   *
   * @param token token to identify second page.
   * @return Argument captor to check which tokens are requested.
   */
  private ArgumentCaptor<String> mockTwoPageProjectsRespose(String token) throws IOException {
    mockListProjectsResponse(null);
    // emulate page with size 1, one token returned, then completed.
    ListProjectsResponse firstResponse = new ListProjectsResponse();
    firstResponse.setProjects(Collections.singletonList(testProject1));
    firstResponse.setNextPageToken(token);
    ListProjectsResponse secondResponse = new ListProjectsResponse();
    secondResponse.setProjects(Collections.singletonList(testProject2));
    secondResponse.setNextPageToken("");

    ArgumentCaptor<String> pageTokenCaptor = ArgumentCaptor.forClass(String.class);
    when(mockList.setPageToken(pageTokenCaptor.capture())).thenReturn(mockList);
    when(mockList.execute()).thenReturn(firstResponse, secondResponse);

    return pageTokenCaptor;
  }
}
