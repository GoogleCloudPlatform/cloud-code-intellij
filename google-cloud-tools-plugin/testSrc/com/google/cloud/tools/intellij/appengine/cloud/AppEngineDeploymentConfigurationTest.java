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

package com.google.cloud.tools.intellij.appengine.cloud;

import static com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND;
import static com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED;
import static com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT;
import static com.google.cloud.tools.intellij.testing.TestUtils.expectThrows;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestDirectory;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.ImmutableSet;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineDeploymentConfiguration}. */
@RunWith(JUnit4.class)
public final class AppEngineDeploymentConfigurationTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private RemoteServer<AppEngineServerConfiguration> mockRemoteServer;
  @Mock private AppEngineDeployable mockAppEngineDeployable;
  @Mock private UserSpecifiedPathDeploymentSource mockUserSpecifiedPathDeploymentSource;
  @Mock private DeploymentSource mockOtherDeploymentSource;
  @Mock @TestService private CloudSdkService mockCloudSdkService;
  @Mock @TestService private AppEngineProjectService mockAppEngineProjectService;

  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID)
  private Module module;

  @TestFile(name = "java.yaml", contents = "runtime: java\nservice: javaService")
  private File javaYaml;

  @TestFile(name = "custom.yaml", contents = "runtime: custom\nservice: customService")
  private File customYaml;

  @TestFile(
    name = "Dockerfile",
    contents = "FROM gcr.io/google_appengine/jetty\nADD target.war $JETTY_BASE/webapps/root.war"
  )
  private File dockerfile;

  @TestFile(name = "some.war")
  private File someWar;

  @TestFile(name = "some.jar")
  private File someJar;

  @TestFile(name = "some-other-file.txt")
  private File someOtherFile;

  @TestDirectory(name = "emptyDirectory")
  private File emptyDirectory;

  private AppEngineDeploymentConfiguration configuration;
  private Project project;

  @Before
  public void setUp() throws Exception {
    FacetManager.getInstance(module)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath(javaYaml.getPath());

    when(mockAppEngineDeployable.isValid()).thenReturn(true);
    configuration = new AppEngineDeploymentConfiguration();
    project = testFixture.getProject();
  }

  @Test
  public void checkConfiguration_withValidFlexConfig_doesNotThrow() throws Exception {
    setUpValidFlexConfiguration();
    configuration.checkConfiguration(mockRemoteServer, mockAppEngineDeployable, project);
  }

  @Test
  public void checkConfiguration_withValidFlexConfig_andUserSpecifiedWar_doesNotThrow()
      throws Exception {
    setUpValidFlexConfigurationWithUserSpecifiedSource();
    configuration.checkConfiguration(
        mockRemoteServer, mockUserSpecifiedPathDeploymentSource, project);
  }

  @Test
  public void checkConfiguration_withValidFlexConfig_andUserSpecifiedJar_doesNotThrow()
      throws Exception {
    setUpValidFlexConfigurationWithUserSpecifiedSource();
    configuration.setUserSpecifiedArtifactPath(someJar.getPath());
    configuration.checkConfiguration(
        mockRemoteServer, mockUserSpecifiedPathDeploymentSource, project);
  }

  @Test
  public void checkConfiguration_withValidFlexConfig_andNoRuntimeFromAppYaml_doesNotThrow()
      throws Exception {
    setUpValidFlexConfiguration();
    when(mockAppEngineProjectService.getFlexibleRuntimeFromAppYaml(customYaml.getPath()))
        .thenReturn(Optional.empty());
    configuration.checkConfiguration(mockRemoteServer, mockAppEngineDeployable, project);
  }

  @Test
  public void checkConfiguration_withValidCustomFlexConfig_doesNotThrow() throws Exception {
    setUpValidCustomFlexConfiguration();
    configuration.checkConfiguration(mockRemoteServer, mockAppEngineDeployable, project);
  }

  @Test
  public void checkConfiguration_withValidStandardConfig_doesNotThrow() throws Exception {
    setUpValidStandardConfiguration();
    configuration.checkConfiguration(mockRemoteServer, mockAppEngineDeployable, project);
  }

  @Test
  public void checkConfiguration_withOtherDeploymentSource_throwsException() {
    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockOtherDeploymentSource, project));
    assertThat(error).hasMessage("Invalid deployment source.");
  }

  @Test
  public void checkConfiguration_withCloudSdkNotFound_throwsException() {
    setUpValidFlexConfiguration();
    when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of(CLOUD_SDK_NOT_FOUND));

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error)
        .hasMessage("Server is misconfigured: No Cloud SDK was found in the specified directory.");
  }

  @Test
  public void checkConfiguration_withOutdatedCloudSdkVersion_throwsException() {
    setUpValidFlexConfiguration();
    when(mockCloudSdkService.validateCloudSdk())
        .thenReturn(ImmutableSet.of(CLOUD_SDK_VERSION_NOT_SUPPORTED));

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error.getMessage())
        .contains("Server is misconfigured: The Cloud SDK is out of date.");
  }

  @Test
  public void checkConfiguration_withNoAppEngineComponent_throwsException() {
    setUpValidFlexConfiguration();
    when(mockCloudSdkService.validateCloudSdk())
        .thenReturn(ImmutableSet.of(NO_APP_ENGINE_COMPONENT));

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error.getMessage())
        .contains(
            "Server is misconfigured: The Cloud SDK does not contain the app-engine-java "
                + "component.");
  }

  @Test
  public void checkConfiguration_withInvalidDeployable_throwsException() {
    setUpValidFlexConfiguration();
    when(mockAppEngineDeployable.isValid()).thenReturn(false);

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("Select a valid deployment source.");
  }

  @Test
  public void checkConfiguration_withBlankCloudProject_throwsException() {
    setUpValidFlexConfiguration();
    configuration.setCloudProjectName("");

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("Please select a project.");
  }

  @Test
  public void checkConfiguration_withUserSpecifiedSource_andNoArtifactPath_throwsException() {
    setUpValidFlexConfiguration();
    when(mockUserSpecifiedPathDeploymentSource.isValid()).thenReturn(true);
    when(mockUserSpecifiedPathDeploymentSource.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);
    configuration.setUserSpecifiedArtifactPath("");

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockUserSpecifiedPathDeploymentSource, project));
    assertThat(error).hasMessage("Browse to a JAR or WAR file.");
  }

  @Test
  public void checkConfiguration_withUserSpecifiedSource_andNotAJarOrWar_throwsException() {
    setUpValidFlexConfigurationWithUserSpecifiedSource();
    configuration.setUserSpecifiedArtifactPath(someOtherFile.getPath());

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockUserSpecifiedPathDeploymentSource, project));
    assertThat(error).hasMessage("Browse to a JAR or WAR file.");
  }

  @Test
  public void checkConfiguration_withUserSpecifiedSource_andDirectory_throwsException() {
    setUpValidFlexConfigurationWithUserSpecifiedSource();
    configuration.setUserSpecifiedArtifactPath(someWar.getParent());

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockUserSpecifiedPathDeploymentSource, project));
    assertThat(error).hasMessage("Browse to a JAR or WAR file.");
  }

  @Test
  public void checkConfiguration_withBlankModuleName_throwsException() throws Exception {
    setUpValidFlexConfiguration();
    configuration.setModuleName("");

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("Select a module with the App Engine flexible facet.");
  }

  @Test
  public void checkConfiguration_withInvalidModuleName_throwsException() throws Exception {
    setUpValidFlexConfiguration();
    configuration.setModuleName("some invalid module");

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("Select a module with the App Engine flexible facet.");
  }

  @Test
  public void checkConfiguration_withNoAppYaml_throwsException() throws Exception {
    setUpValidFlexConfiguration();

    FacetManager.getInstance(module)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath(null);

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("Browse to an app.yaml file.");
  }

  @Test
  public void checkConfiguration_withInvalidAppYaml_throwsException() throws Exception {
    setUpValidFlexConfiguration();

    FacetManager.getInstance(module)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath("/some/invalid/file");

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error)
        .hasMessage(
            "The specified app.yaml configuration file does not exist or is not a valid file.");
  }

  @Test
  public void checkConfiguration_withMalformedAppYaml_throwsException() throws Exception {
    setUpValidFlexConfiguration();

    when(mockAppEngineProjectService.getFlexibleRuntimeFromAppYaml(javaYaml.getPath()))
        .thenThrow(new MalformedYamlFileException(null));

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("The selected app.yaml file is malformed.");
  }

  @Test
  public void checkConfiguration_withNoDockerDirectory_throwsException() throws Exception {
    setUpValidCustomFlexConfiguration();

    FacetManager.getInstance(module)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setDockerDirectory(null);

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("Browse to a Docker directory.");
  }

  @Test
  public void checkConfiguration_withNoDockerfile_throwsException() throws Exception {
    setUpValidCustomFlexConfiguration();

    FacetManager.getInstance(module)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setDockerDirectory(emptyDirectory.getPath());

    RuntimeConfigurationError error =
        expectThrows(
            RuntimeConfigurationError.class,
            () ->
                configuration.checkConfiguration(
                    mockRemoteServer, mockAppEngineDeployable, project));
    assertThat(error).hasMessage("There is no Dockerfile in specified directory.");
  }

  @Test
  public void checkConfiguration_withNoStagedArtifactName_doesNotThrow() throws Exception {
    setUpValidCustomFlexConfiguration();

    configuration.setStagedArtifactName(null);

    configuration.checkConfiguration(mockRemoteServer, mockAppEngineDeployable, project);
  }

  @Test
  public void checkConfiguration_withNullEnvironment_doesNotThrow() throws Exception {
    setUpValidFlexConfiguration();
    when(mockAppEngineDeployable.getEnvironment()).thenReturn(null);

    configuration.checkConfiguration(mockRemoteServer, mockAppEngineDeployable, project);
  }

  @Test
  public void equals_withEqualConfigs_doesReturnTrue() {
    AppEngineDeploymentConfiguration configA = createPopulatedConfig();
    AppEngineDeploymentConfiguration configB = createPopulatedConfig();

    assertThat(configA.equals(configB)).isTrue();
  }

  @Test
  public void equals_withNewConfigs_doesReturnTrue() {
    AppEngineDeploymentConfiguration configA = new AppEngineDeploymentConfiguration();
    AppEngineDeploymentConfiguration configB = new AppEngineDeploymentConfiguration();

    assertThat(configA.equals(configB)).isTrue();
  }

  @Test
  public void equals_withSameConfig_doesReturnTrue() {
    AppEngineDeploymentConfiguration configA = createPopulatedConfig();
    AppEngineDeploymentConfiguration configB = configA;

    assertThat(configA.equals(configB)).isTrue();
  }

  @Test
  public void equals_withOtherObject_doesReturnFalse() {
    AppEngineDeploymentConfiguration configA = new AppEngineDeploymentConfiguration();
    CloudDeploymentNameConfiguration configB = new CloudDeploymentNameConfiguration();

    assertThat(configA.equals(configB)).isFalse();
  }

  @Test
  public void equals_withDifferentConfigs_doesReturnFalse() {
    AppEngineDeploymentConfiguration configA = createPopulatedConfig();
    AppEngineDeploymentConfiguration configB = new AppEngineDeploymentConfiguration();

    assertThat(configA.equals(configB)).isFalse();
  }

  @Test
  public void hashCode_withEqualConfigs_doesReturnSameHashCode() {
    int hashCodeA = createPopulatedConfig().hashCode();
    int hashCodeB = createPopulatedConfig().hashCode();

    assertThat(hashCodeA).isEqualTo(hashCodeB);
  }

  @Test
  public void hashCode_withDifferentConfigs_doesReturnDifferentHashCodes() {
    int hashCodeA = createPopulatedConfig().hashCode();
    int hashCodeB = new AppEngineDeploymentConfiguration().hashCode();

    assertThat(hashCodeA).isNotEqualTo(hashCodeB);
  }

  /** Sets up the {@code configuration} to be valid for a deployment to a flex environment. */
  private void setUpValidFlexConfiguration() {
    when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());
    when(mockAppEngineDeployable.isValid()).thenReturn(true);
    when(mockAppEngineDeployable.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    configuration.setCloudProjectName("some-project-name");
    configuration.setModuleName(module.getName());

    try {
      when(mockAppEngineProjectService.getFlexibleRuntimeFromAppYaml(javaYaml.getPath()))
          .thenReturn(Optional.of(FlexibleRuntime.JAVA));
    } catch (MalformedYamlFileException e) {
      throw new AssertionError("This should not happen; Mockito must be broken.", e);
    }
  }

  /**
   * Sets up the {@code configuration} to be valid for a deployment to a flex environment with a
   * {@link UserSpecifiedPathDeploymentSource}.
   */
  private void setUpValidFlexConfigurationWithUserSpecifiedSource() {
    when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());
    when(mockUserSpecifiedPathDeploymentSource.isValid()).thenReturn(true);
    when(mockUserSpecifiedPathDeploymentSource.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    configuration.setCloudProjectName("some-project-name");
    configuration.setUserSpecifiedArtifactPath(someWar.getPath());
    configuration.setModuleName(module.getName());

    try {
      when(mockAppEngineProjectService.getFlexibleRuntimeFromAppYaml(javaYaml.getPath()))
          .thenReturn(Optional.of(FlexibleRuntime.JAVA));
    } catch (MalformedYamlFileException e) {
      throw new AssertionError("This should not happen; Mockito must be broken.", e);
    }
  }

  /** Sets up the {@code configuration} to be a valid custom deployment to a flex environment. */
  private void setUpValidCustomFlexConfiguration() {
    when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());
    when(mockAppEngineDeployable.isValid()).thenReturn(true);
    when(mockAppEngineDeployable.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    AppEngineFlexibleFacetConfiguration facetConfig =
        FacetManager.getInstance(module)
            .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
            .getConfiguration();
    facetConfig.setAppYamlPath(customYaml.getPath());
    facetConfig.setDockerDirectory(dockerfile.getParent());

    configuration.setCloudProjectName("some-project-name");
    configuration.setModuleName(module.getName());
    configuration.setStagedArtifactName("target.war");

    try {
      when(mockAppEngineProjectService.getFlexibleRuntimeFromAppYaml(customYaml.getPath()))
          .thenReturn(Optional.of(FlexibleRuntime.CUSTOM));
    } catch (MalformedYamlFileException e) {
      throw new AssertionError("This should not happen; Mockito must be broken.", e);
    }
  }

  /** Sets up the {@code configuration} to be valid for a deployment to a standard environment. */
  private void setUpValidStandardConfiguration() {
    when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());
    when(mockAppEngineDeployable.isValid()).thenReturn(true);
    when(mockAppEngineDeployable.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);
    configuration.setCloudProjectName("some-project-name");
  }

  /**
   * Returns a fully populated {@link AppEngineDeploymentConfiguration} with non-default values for
   * every field.
   */
  private static AppEngineDeploymentConfiguration createPopulatedConfig() {
    AppEngineDeploymentConfiguration configuration = new AppEngineDeploymentConfiguration();
    configuration.setCloudProjectName("cloud-project-name");
    configuration.setGoogleUsername("google-username");
    configuration.setEnvironment(AppEngineEnvironment.APP_ENGINE_FLEX);
    configuration.setUserSpecifiedArtifactPath("user-specified-artifact-path");
    configuration.setPromote(true);
    configuration.setStopPreviousVersion(true);
    configuration.setVersion("version");
    configuration.setDeployAllConfigs(true);
    configuration.setModuleName("module-name");
    configuration.setStagedArtifactName("staged-artifact-name");
    configuration.setStagedArtifactNameLegacy(true);
    configuration.setDefaultDeploymentName(true);
    configuration.setDeploymentName("deployment-name");
    return configuration;
  }
}
