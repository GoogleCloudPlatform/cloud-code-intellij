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

package com.google.cloud.tools.intellij.appengine.cloud.standard;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.util.GctBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link AppEngineStandardStagingPropertiesPanel}. */
@RunWith(JUnit4.class)
public class AppEngineStandardStagingPropertiesPanelTest {
  private AppEngineStandardStagingPropertiesPanel stagingPanel =
      new AppEngineStandardStagingPropertiesPanel();
  private AppEngineDeploymentConfiguration configuration = new AppEngineDeploymentConfiguration();

  @Test
  public void applyEditorTo_doesSetCompileEncoding() throws Exception {
    String encoding = "UTF-8";
    stagingPanel.getCompileEncodingTextField().setText(encoding);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getCompileEncoding()).isEqualTo(encoding);
  }

  @Test
  public void applyEditorTo_doesSetDeleteJsps() throws Exception {
    stagingPanel.getDeleteJspsCheckBox().setSelected(true);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getDeleteJsps()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetDisableJarJsps() throws Exception {
    stagingPanel.getDisableJarJspsCheckBox().setSelected(true);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getDisableJarJsps()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetDisableUpdateCheck() throws Exception {
    stagingPanel.getDisableUpdateCheckCheckBox().setSelected(true);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getDisableUpdateCheck()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetEnableJarClasses() throws Exception {
    stagingPanel.getEnableJarClassesCheckBox().setSelected(true);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getEnableJarClasses()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetEnableJarSplitting() throws Exception {
    stagingPanel.getEnableJarSplittingCheckBox().setSelected(true);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getEnableJarSplitting()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetEnableQuickstart() throws Exception {
    stagingPanel.getEnableQuickstartCheckBox().setSelected(true);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getEnableQuickstart()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetJarSplittingExcludes() throws Exception {
    String exclusions = "exclude.this";
    stagingPanel.getJarSplittingExcludesTextField().setText(exclusions);

    stagingPanel.applyEditorTo(configuration);

    assertThat(configuration.getJarSplittingExcludes()).isEqualTo(exclusions);
  }

  @Test
  public void resetEditorFrom_doesSetCompileEncoding() {
    String encoding = "UTF-8";
    configuration.setCompileEncoding(encoding);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getCompileEncodingTextField().getText()).isEqualTo(encoding);
  }

  @Test
  public void resetEditorFrom_doesSetDeleteJsps() {
    configuration.setDeleteJsps(true);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getDeleteJspsCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetDisableJarJsps() {
    configuration.setDisableJarJsps(true);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getDisableJarJspsCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetDisableUpdateCheck() {
    configuration.setDisableUpdateCheck(true);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getDisableUpdateCheckCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnableJarClasses() {
    configuration.setEnableJarClasses(true);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getEnableJarClassesCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnableJarSplitting() {
    configuration.setEnableJarSplitting(true);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getEnableJarSplittingCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnableQuickstart() {
    configuration.setEnableQuickstart(true);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getEnableQuickstartCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetJarSplittingExcludes() {
    String exclusions = "exclude.this";
    configuration.setJarSplittingExcludes(exclusions);

    stagingPanel.resetEditorFrom(configuration);

    assertThat(stagingPanel.getJarSplittingExcludesTextField().getText()).isEqualTo(exclusions);
  }

  @Test
  public void checkingEnableJarSplitting_enablesJarSplittingExcludes() {
    stagingPanel.getEnableJarSplittingCheckBox().setSelected(true);

    assertThat(stagingPanel.getJarSplittingExcludesTextField().isEnabled()).isTrue();
  }

  @Test
  public void uncheckingEnableJarSplitting_disablesJarSplittingExcludes() {
    stagingPanel.getEnableJarSplittingCheckBox().setSelected(false);

    assertThat(stagingPanel.getJarSplittingExcludesTextField().isEnabled()).isFalse();
  }

  @Test
  public void checkingEnableJarSplitting_jarSplittingExcludes_noEmptyText() {
    stagingPanel.getEnableJarSplittingCheckBox().setSelected(true);

    assertThat(stagingPanel.getJarSplittingExcludesTextField().getEmptyText().getText()).isEmpty();
  }

  @Test
  public void uncheckingEnableJarSplitting_jarSplittingExcludes_hasEmptyText() {
    stagingPanel.getEnableJarSplittingCheckBox().setSelected(false);

    assertThat(stagingPanel.getJarSplittingExcludesTextField().getEmptyText().getText())
        .isEqualTo(
            GctBundle.message("appengine.deployment.staging.jar.splitting.excludes.emptyText"));
  }
}
