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

import static com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ENVIRONMENT_ATTRIBUTE;
import static com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.STAGED_ARTIFACT_NAME_LEGACY;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Strings;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ConverterProvider;
import com.intellij.conversion.ProjectConverter;
import com.intellij.conversion.RunManagerSettings;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ConverterProvider} for converting the staged artifact name in App Engine run
 * configurations.
 *
 * <p>With the introduction of this field, existing run configurations may rely on the previous
 * behavior for staging artifacts for custom runtime deployments. This behavior was to always rename
 * the built artifact to "target.jar" or "target.war", depending on the type of artifact that was
 * built.
 *
 * <p>This conversion flags all deployment configurations with a legacy bit that will be checked
 * upon deployment. If set, the staged artifact name will be explicitly set to "target.jar" or
 * "target.war" so the functionality is preserved.
 */
public final class StagedArtifactNameConverterProvider extends ConverterProvider {

  public StagedArtifactNameConverterProvider() {
    super("gcp-staged-artifact-name-converter");
  }

  @NotNull
  @Override
  public String getConversionDescription() {
    return GctBundle.message("appengine.staged.artifact.name.converter.description");
  }

  @NotNull
  @Override
  public ProjectConverter createConverter(@NotNull ConversionContext context) {
    return new ProjectConverter() {
      @Override
      public ConversionProcessor<RunManagerSettings> createRunConfigurationsConverter() {
        return new StagedArtifactNameConversionProcessor(context);
      }
    };
  }

  private static final class StagedArtifactNameConversionProcessor
      extends ConversionProcessor<RunManagerSettings> {

    static final Function<Element, Stream<Element>> TO_DEPLOYMENT_SETTINGS =
        element -> {
          Element deployment = element.getChild("deployment");
          if (deployment != null) {
            Element settings = deployment.getChild("settings");
            if (settings != null) {
              return Stream.of(settings);
            }
          }
          return Stream.empty();
        };

    static final Predicate<Element> IS_GCP_APP_ENGINE_DEPLOY =
        element -> "gcp-app-engine-deploy".equals(element.getAttributeValue("type"));

    static final Predicate<Element> IS_LEGACY_BIT_NONEXISTENT =
        element -> element.getAttribute(STAGED_ARTIFACT_NAME_LEGACY) == null;

    static final Predicate<Element> IS_FLEX_ENVIRONMENT = element -> {
      String environmentString = element.getAttributeValue(ENVIRONMENT_ATTRIBUTE);
      if (Strings.isNullOrEmpty(environmentString)) {
        return false;
      }

      try {
        AppEngineEnvironment environment = AppEngineEnvironment.valueOf(environmentString);
        return environment.equals(AppEngineEnvironment.APP_ENGINE_FLEX);
      } catch (IllegalArgumentException e) {
        return false;
      }
    };

    final ConversionContext conversionContext;

    StagedArtifactNameConversionProcessor(ConversionContext conversionContext) {
      this.conversionContext = conversionContext;
    }

    @Override
    public boolean isConversionNeeded(RunManagerSettings runManagerSettings) {
      return runManagerSettings
          .getRunConfigurations()
          .stream()
          .filter(IS_GCP_APP_ENGINE_DEPLOY)
          .flatMap(TO_DEPLOYMENT_SETTINGS)
          .anyMatch(IS_LEGACY_BIT_NONEXISTENT.and(IS_FLEX_ENVIRONMENT));
    }

    @Override
    public void process(RunManagerSettings runManagerSettings) throws CannotConvertException {
      runManagerSettings
          .getRunConfigurations()
          .stream()
          .filter(IS_GCP_APP_ENGINE_DEPLOY)
          .flatMap(TO_DEPLOYMENT_SETTINGS)
          .filter(IS_LEGACY_BIT_NONEXISTENT.and(IS_FLEX_ENVIRONMENT))
          .forEach(
              element -> element.setAttribute(STAGED_ARTIFACT_NAME_LEGACY, Boolean.toString(true)));
    }
  }
}
