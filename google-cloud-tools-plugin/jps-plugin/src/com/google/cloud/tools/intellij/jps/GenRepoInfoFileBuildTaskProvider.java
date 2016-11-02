/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.jps;

import com.google.cloud.tools.appengine.api.debug.DefaultGenRepoInfoFileConfiguration;
import com.google.cloud.tools.appengine.api.debug.GenRepoInfoFile;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkGenRepoInfoFile;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.artifacts.ArtifactBuildTaskProvider;
import org.jetbrains.jps.incremental.BuildTask;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Generates source context files to be used by the Google Stackdriver Debugger.
 */
public class GenRepoInfoFileBuildTaskProvider extends ArtifactBuildTaskProvider {
  // Logging information from here goes to build/idea-sandbox/system/log/build-log/build.log.
  private static final Logger LOG =
      Logger.getInstance("#com.google.cloud.tools.intellij.jps.GenRepoInfoFileBuildTaskProvider");

  @NotNull
  @Override
  public List<? extends BuildTask> createArtifactBuildTasks(@NotNull JpsArtifact artifact,
      @NotNull ArtifactBuildPhase buildPhase) {
    if (buildPhase.equals(ArtifactBuildPhase.PRE_PROCESSING)) { // and only WAR/JAR (no exploded?)
      return Collections.singletonList(
          new GenRepoInfoBuildTask(Paths.get(artifact.getOutputFilePath())));
    }
    return Collections.emptyList();
  }

  class GenRepoInfoBuildTask extends BuildTask {
    private Path sourceDirectory;
    private Path outputDirectory;

    private GenRepoInfoBuildTask(Path outputDirectory) {
      this.outputDirectory = outputDirectory;
    }

    private GenRepoInfoBuildTask(Path sourceDirectory, Path outputDirectory) {
      this(outputDirectory);
      this.sourceDirectory = sourceDirectory;
    }

    @Override
    public void build(CompileContext context) throws ProjectBuildException {
      // Get source directory from project's modules. Consider only the first module.
      List<JpsModule> modules = context.getProjectDescriptor().getProject().getModules();
      if (!modules.isEmpty()) {
        List<JpsModuleSourceRoot> sourceRoots = modules.iterator().next().getSourceRoots();
        if (!sourceRoots.isEmpty()) {
          sourceDirectory = Paths.get(sourceRoots.iterator().next().getFile().getPath());
        }
      }

      if (sourceDirectory == null) {
        LOG.info("Source directory couldn't be discovered. Skipping source context generation.");
        return;
      }

//      Path sdkPath = CloudSdkService.getInstance().getSdkHomePath();
//      if (sdkPath == null) {
//        LOG.info("No Cloud SDK specified. Skipping source context generation.");
//        return;
//      }

      CloudSdk sdk = new CloudSdk.Builder()
          .sdkPath(Paths.get("/usr/local/google/home/joaomartins/Downloads/google-cloud-sdk"))
//          .sdkPath(CloudSdkService.getInstance().getSdkHomePath())
          .exitListener(new ProcessExitListener() {
            @Override
            public void onExit(int exitCode) {
              // Stop build if error is thrown. Should be off by default.

            }
          })
          .build();
      GenRepoInfoFile genAction = new CloudSdkGenRepoInfoFile(sdk);
      DefaultGenRepoInfoFileConfiguration configuration = new DefaultGenRepoInfoFileConfiguration();
      configuration.setSourceDirectory(sourceDirectory.toFile());
      configuration.setOutputDirectory(outputDirectory.toFile());
//      genAction.generate(configuration);
    }
  }
}
