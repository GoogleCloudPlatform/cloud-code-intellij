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

package com.google.cloud.tools.intellij.appengine.migration;

import static com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.STAGED_ARTIFACT_NAME;
import static com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.common.collect.ImmutableList;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.RunManagerSettings;
import java.io.File;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link StagedArtifactNameConverterProvider}. */
@RunWith(JUnit4.class)
public final class StagedArtifactNameConverterProviderTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private ConversionContext mockConversionContext;
  @Mock private RunManagerSettings mockRunManagerSettings;

  @TestFile(name = "some.war")
  private File someWar;

  @TestFile(name = "some.jar")
  private File someJar;

  @TestFile(name = "some.txt")
  private File someTxt;

  private final Element configurationElement = new Element("configuration");
  private final Element deploymentElement = new Element("deployment");
  private final Element settingsElement = new Element("settings");
  private final StagedArtifactNameConverterProvider converterProvider =
      new StagedArtifactNameConverterProvider();

  @Before
  public void setUpElements() {
    configurationElement.addContent(deploymentElement);
    deploymentElement.addContent(settingsElement);

    // Defaults the artifact path to a valid WAR path.
    settingsElement.setAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE, someWar.toString());
  }

  @Before
  public void setUpRunManagerSettings() {
    // .thenReturn() cannot be used with type checking due to restricted wildcard on the interface.
    when(mockRunManagerSettings.getRunConfigurations())
        .thenAnswer(invocation -> ImmutableList.of(configurationElement));
  }

  @Test
  public void getConversionDescription_doesReturnDescription() {
    String expected =
        "Converts run configurations to set the new staged artifact name parameter to the default.";
    assertThat(converterProvider.getConversionDescription()).isEqualTo(expected);
  }

  @Test
  public void isConversionNeeded_withNullStagedArtifactName_doesReturnTrue() {
    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isTrue();
  }

  @Test
  public void isConversionNeeded_withMultipleConfigs_oneHasNullStagedArtifactName_doesReturnTrue() {
    Element otherConfig = createOtherConfig();
    when(mockRunManagerSettings.getRunConfigurations())
        .thenAnswer(invocation -> ImmutableList.of(otherConfig, configurationElement));

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isTrue();
  }

  @Test
  public void isConversionNeeded_withStagedArtifactName_doesReturnFalse() {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME, "target.war");

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withEmptyStagedArtifactName_doesReturnFalse() {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME, "");

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withNullArtifactPath_doesReturnFalse() {
    settingsElement.removeAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE);

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withNoSettingsElement_doesReturnFalse() {
    deploymentElement.removeContent(settingsElement);

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withNoDeploymentElement_doesReturnFalse() {
    configurationElement.removeContent(deploymentElement);

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withEmptyElementsList_doesReturnFalse() {
    when(mockRunManagerSettings.getRunConfigurations()).thenReturn(ImmutableList.of());

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void process_withNullStagedArtifactName_doesSetNameToTarget()
      throws CannotConvertException {
    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isEqualTo("target.war");
  }

  @Test
  public void process_withMultipleConfigs_oneHasNullStagedArtifactName_doesSetNullNameToTarget()
      throws CannotConvertException {
    Element otherConfig = createOtherConfig();
    when(mockRunManagerSettings.getRunConfigurations())
        .thenAnswer(invocation -> ImmutableList.of(otherConfig, configurationElement));

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isEqualTo("target.war");
    Element otherSettings = otherConfig.getChild("deployment").getChild("settings");
    assertThat(otherSettings.getAttributeValue(STAGED_ARTIFACT_NAME)).isEqualTo("some-name.war");
  }

  @Test
  public void process_withJarArtifact_doesSetNameToTargetJar() throws CannotConvertException {
    settingsElement.setAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE, someJar.toString());

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isEqualTo("target.jar");
  }

  @Test
  public void process_withUnknownArtifact_doesNotChangeName() throws CannotConvertException {
    settingsElement.setAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE, someTxt.toString());

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isNull();
  }

  @Test
  public void process_withStagedArtifactName_doesNotChangeName() throws CannotConvertException {
    String stagedArtifactName = "some-name.war";
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME, stagedArtifactName);

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME))
        .isEqualTo(stagedArtifactName);
  }

  @Test
  public void process_withEmptyStagedArtifactName_doesNotChangeName()
      throws CannotConvertException {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME, "");

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isEmpty();
  }

  @Test
  public void process_withNullArtifactPath_doesNotChangeName() throws CannotConvertException {
    settingsElement.removeAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE);

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isNull();
  }

  @Test
  public void process_withNoSettingsElement_doesNotChangeName() throws CannotConvertException {
    deploymentElement.removeContent(settingsElement);

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isNull();
  }

  @Test
  public void process_withNoDeploymentElement_doesNotChangeName() throws CannotConvertException {
    configurationElement.removeContent(deploymentElement);

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isNull();
  }

  @Test
  public void process_withEmptyElementsList_doesNotChangeName() throws CannotConvertException {
    when(mockRunManagerSettings.getRunConfigurations()).thenReturn(ImmutableList.of());

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME)).isNull();
  }

  private ConversionProcessor<RunManagerSettings> getConversionProcessor() {
    return converterProvider
        .createConverter(mockConversionContext)
        .createRunConfigurationsConverter();
  }

  private Element createOtherConfig() {
    Element otherConfig = new Element("configuration");
    Element otherDeployment = new Element("deployment");
    Element otherSettings = new Element("settings");
    otherConfig.addContent(otherDeployment);
    otherDeployment.addContent(otherSettings);
    otherSettings.setAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE, someWar.toString());
    otherSettings.setAttribute(STAGED_ARTIFACT_NAME, "some-name.war");
    return otherConfig;
  }
}
