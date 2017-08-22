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
import static com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType.UNKNOWN;

import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ConversionContext;
import com.intellij.conversion.ConversionProcessor;
import com.intellij.conversion.ConverterProvider;
import com.intellij.conversion.ProjectConverter;
import com.intellij.conversion.RunManagerSettings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ConverterProvider} for converting the staged artifact name in App Engine run
 * configurations.
 *
 * <p>With the introduction of this field, existing run configurations may rely on the previous
 * behavior for staging artifacts for custom runtime deployments. This behavior was to always rename
 * the built artifact to "target.jar" or "target.war", depending on the type of artifact that was
 * built. This conversion explicitly sets this new field to the expected default value so these
 * existing run configurations continue to operate correctly.
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
      @Nullable
      @Override
      public ConversionProcessor<RunManagerSettings> createRunConfigurationsConverter() {
        return new StagedArtifactNameConversionProcessor();
      }
    };
  }

  /** A {@link ConversionProcessor} for converting the staged artifact name. */
  private static final class StagedArtifactNameConversionProcessor
      extends ConversionProcessor<RunManagerSettings> {

    private static final Function<Element, Stream<Element>> TO_DEPLOYMENT_SETTINGS =
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

    private static final Predicate<Element> IS_STAGED_ARTIFACT_NAME_NULL =
        element -> element.getAttribute(STAGED_ARTIFACT_NAME) == null;

    private static final Predicate<Element> HAS_ARTIFACT_PATH =
        element -> element.getAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE) != null;

    @Override
    public boolean isConversionNeeded(RunManagerSettings runManagerSettings) {
      return runManagerSettings
          .getRunConfigurations()
          .stream()
          .flatMap(TO_DEPLOYMENT_SETTINGS)
          .anyMatch(IS_STAGED_ARTIFACT_NAME_NULL.and(HAS_ARTIFACT_PATH));
    }

    @Override
    public void process(RunManagerSettings runManagerSettings) throws CannotConvertException {
      runManagerSettings
          .getRunConfigurations()
          .stream()
          .flatMap(TO_DEPLOYMENT_SETTINGS)
          .filter(IS_STAGED_ARTIFACT_NAME_NULL.and(HAS_ARTIFACT_PATH))
          .forEach(
              element -> {
                Attribute artifactPathAttribute =
                    element.getAttribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE);
                Path artifactPath = Paths.get(artifactPathAttribute.getValue());
                AppEngineFlexibleDeploymentArtifactType type =
                    AppEngineFlexibleDeploymentArtifactType.typeForPath(artifactPath);

                if (!type.equals(UNKNOWN)) {
                  element.setAttribute(STAGED_ARTIFACT_NAME, "target." + type.name().toLowerCase());
                }
              });
    }
  }
}
