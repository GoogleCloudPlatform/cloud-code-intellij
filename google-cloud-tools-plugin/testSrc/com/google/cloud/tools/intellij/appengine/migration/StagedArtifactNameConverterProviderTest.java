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

import static com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.STAGED_ARTIFACT_NAME_LEGACY;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.common.collect.ImmutableList;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.RunManagerSettings;
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

  private static final String TYPE = "type";
  private static final String GCP_APP_ENGINE_DEPLOY = "gcp-app-engine-deploy";

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private ConversionContext mockConversionContext;
  @Mock private RunManagerSettings mockRunManagerSettings;

  private final Element configurationElement = new Element("configuration");
  private final Element deploymentElement = new Element("deployment");
  private final Element settingsElement = new Element("settings");
  private final StagedArtifactNameConverterProvider converterProvider =
      new StagedArtifactNameConverterProvider();

  @Before
  public void setUpElements() {
    configurationElement.setAttribute(TYPE, GCP_APP_ENGINE_DEPLOY);
    configurationElement.addContent(deploymentElement);
    deploymentElement.addContent(settingsElement);
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
        "Converts run configurations to set a legacy bit for preserving the old staged artifact "
            + "name.";
    assertThat(converterProvider.getConversionDescription()).isEqualTo(expected);
  }

  @Test
  public void isConversionNeeded_withNullLegacyBit_doesReturnTrue() {
    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isTrue();
  }

  @Test
  public void isConversionNeeded_withMultipleConfigs_oneHasNullLegacyBit_doesReturnTrue() {
    Element otherConfig = createOtherConfig();
    when(mockRunManagerSettings.getRunConfigurations())
        .thenAnswer(invocation -> ImmutableList.of(otherConfig, configurationElement));

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isTrue();
  }

  @Test
  public void isConversionNeeded_withDifferentConfigurationType_doesReturnFalse() {
    configurationElement.setAttribute(TYPE, "something-else");

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withFalseLegacyBit_doesReturnFalse() {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME_LEGACY, Boolean.toString(false));

    assertThat(getConversionProcessor().isConversionNeeded(mockRunManagerSettings)).isFalse();
  }

  @Test
  public void isConversionNeeded_withTrueLegacyBit_doesReturnFalse() {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME_LEGACY, Boolean.toString(true));

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
  public void process_withNullLegacyBit_doesSetLegacyBit() throws CannotConvertException {
    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME_LEGACY))
        .isEqualTo(Boolean.toString(true));
  }

  @Test
  public void process_withMultipleConfigs_oneHasNullLegacyBit_doesSetLegacyBit()
      throws CannotConvertException {
    Element otherConfig = createOtherConfig();
    when(mockRunManagerSettings.getRunConfigurations())
        .thenAnswer(invocation -> ImmutableList.of(otherConfig, configurationElement));

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME_LEGACY))
        .isEqualTo(Boolean.toString(true));

    // Ensures the other config remains unchanged.
    Element otherSettings = otherConfig.getChild("deployment").getChild("settings");
    assertThat(otherSettings.getAttributeValue(STAGED_ARTIFACT_NAME_LEGACY))
        .isEqualTo(Boolean.toString(false));
  }

  @Test
  public void process_withDifferentConfigurationType_doesNothing() throws CannotConvertException {
    configurationElement.setAttribute(TYPE, "something-else");

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttribute(STAGED_ARTIFACT_NAME_LEGACY)).isNull();
  }

  @Test
  public void process_withFalseLegacyBit_doesNotChangeBitValue() throws CannotConvertException {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME_LEGACY, Boolean.toString(false));

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME_LEGACY))
        .isEqualTo(Boolean.toString(false));
  }

  @Test
  public void process_withTrueLegacyBit_doesNotChangeBitValue() throws CannotConvertException {
    settingsElement.setAttribute(STAGED_ARTIFACT_NAME_LEGACY, Boolean.toString(true));

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttributeValue(STAGED_ARTIFACT_NAME_LEGACY))
        .isEqualTo(Boolean.toString(true));
  }

  @Test
  public void process_withNoSettingsElement_doesNothing() throws CannotConvertException {
    deploymentElement.removeContent(settingsElement);

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttribute(STAGED_ARTIFACT_NAME_LEGACY)).isNull();
  }

  @Test
  public void process_withNoDeploymentElement_doesNothing() throws CannotConvertException {
    configurationElement.removeContent(deploymentElement);

    getConversionProcessor().process(mockRunManagerSettings);

    assertThat(settingsElement.getAttribute(STAGED_ARTIFACT_NAME_LEGACY)).isNull();
  }

  @Test
  public void process_withEmptyElementsList_doesNothing() throws CannotConvertException {
    when(mockRunManagerSettings.getRunConfigurations()).thenReturn(ImmutableList.of());

    assertThat(settingsElement.getAttribute(STAGED_ARTIFACT_NAME_LEGACY)).isNull();
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

    otherConfig.setAttribute(TYPE, GCP_APP_ENGINE_DEPLOY);
    otherConfig.addContent(otherDeployment);
    otherDeployment.addContent(otherSettings);
    otherSettings.setAttribute(STAGED_ARTIFACT_NAME_LEGACY, Boolean.toString(false));
    return otherConfig;
  }
}
