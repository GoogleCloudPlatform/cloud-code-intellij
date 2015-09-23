/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.samples;

import com.android.SdkConstants;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.wizard.dynamic.*;
import com.appspot.gsamplesindex.samplesindex.model.Sample;
import com.appspot.gsamplesindex.samplesindex.model.SampleCollection;
import com.google.common.base.Strings;
import com.google.gct.login.stats.UsageTrackerService;
import com.google.gct.idea.util.GctStudioBundle;
import com.google.gct.idea.util.GctTracking;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.android.tools.idea.wizard.dynamic.ScopedStateStore.Scope.PATH;
import static com.android.tools.idea.wizard.dynamic.ScopedStateStore.createKey;

/**
 * Wizard path for importing a Sample as a Project
 */
public class SampleImportWizardPath extends DynamicWizardPath {

  private static final Logger LOG = Logger.getInstance(SampleImportWizardPath.class);

  @NotNull private final Disposable myParentDisposable;
  @NotNull private final SampleCollection mySampleList;

  static final ScopedStateStore.Key<Sample> SAMPLE_KEY = createKey("SampleObject", PATH, Sample.class);
  static final ScopedStateStore.Key<String> SAMPLE_NAME = createKey("SampleName", PATH, String.class);
  static final ScopedStateStore.Key<String> SAMPLE_DIR = createKey("SampleDirectory", PATH, String.class);
  static final ScopedStateStore.Key<String> SAMPLE_URL = createKey("SampleUrl", PATH, String.class);

  public SampleImportWizardPath(@NotNull SampleCollection sampleList, @NotNull Disposable parentDisposable) {
    mySampleList = sampleList;
    myParentDisposable = parentDisposable;
  }

  @Override
  protected void init() {
    addStep(new SampleBrowserStep(mySampleList, myParentDisposable));
    addStep(new SampleSetupStep(myParentDisposable));
  }

  @NotNull
  @Override
  public String getPathName() {
    return "Sample Import";
  }

  @Override
  public boolean performFinishingActions() {
    final Ref<Boolean> result = Ref.create();
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        result.set(performFinishingActionsInternal());
      }
    }, ModalityState.any());
    return result.get();
  }

  private boolean performFinishingActionsInternal() {
    Sample sample = myState.get(SAMPLE_KEY);
    String sampleName = myState.get(SAMPLE_NAME);
    File sampleDir = new File(myState.get(SAMPLE_DIR));

    assert !sampleDir.exists();

    if (!FileUtilRt.createDirectory(sampleDir)) {
      Messages.showErrorDialog(
          GctStudioBundle.message("create.project.dir.failed"), GctStudioBundle.message("sample.import.error.title"));
      return false;
    }
    Project project = ProjectManager.getInstance().createProject(sampleName, sampleDir.getAbsolutePath());

    String url = trimSlashes(sample.getCloneUrl());

    GithubRepoContents downloadResult = GithubRepoContents.download(project, url, null, null);

    String errorMessage = downloadResult.getErrorMessage();
    if (errorMessage != null) {
      LOG.error(errorMessage);
      Messages.showErrorDialog(errorMessage, GctStudioBundle.message("sample.import.error.title"));
      return false;
    }

    List<File> sampleRoots = downloadResult.getSampleRoots();
    if (sampleRoots.size() == 0) {
      Messages.showErrorDialog(
          GctStudioBundle.message("git.project.dir.empty"), GctStudioBundle.message("sample.import.error.title"));
      return false;
    }

    File rootFolder = downloadResult.getRootFolder();
    try {
      String path = sample.getPath();
      if (!Strings.isNullOrEmpty(path)) {
        // we have a path to work with, find the project that matches it
        path = trimSlashes(path);
        sampleSearch: {
          for (File sampleRoot : sampleRoots) {
            if (sampleRoot.getCanonicalPath().equals(new File(rootFolder, path).getCanonicalPath())) {
              // we found our sample root
              FileUtil.copyDir(sampleRoot, new File(project.getBasePath()));
              break sampleSearch;
            }
          }
          // we have a project that doesn't contain the sample root we're looking for... notify the user
          Messages.showErrorDialog(GctStudioBundle.message("git.project.missing.sample.root", path),
                                   GctStudioBundle.message("sample.import.error.title"));
          return false;
        }
      }
      else {
        // no root was specified, just grab the first root
        FileUtil.copyDir(sampleRoots.get(0), new File(project.getBasePath()));
      }
    }
    catch (IOException e) {
      LOG.error(e);
      Messages.showErrorDialog(
          GctStudioBundle.message("sample.copy.to.project.failed"), GctStudioBundle
          .message("sample.import.error.title"));
      return false;
    }
    // TODO : eventually refactor this out with common code from Android wizard Util
    if (SystemInfo.isUnix) {
      File gradlewFile = new File(project.getBasePath(), SdkConstants.FN_GRADLE_WRAPPER_UNIX);
      if (!gradlewFile.isFile()) {
        LOG.error("Could not find gradle wrapper for sample: " + sampleName + ". Command line builds may not work properly.");
      }
      else {
        try {
          FileUtil.setExecutableAttribute(gradlewFile.getPath(), true);
        } catch (IOException e) {
          Messages.showWarningDialog(GctStudioBundle.message("sample.import.no.gradlew.exec", sampleName),
                                     GctStudioBundle.message("sample.import.warning.title"));
        }
      }
    }

    UsageTrackerService.getInstance().trackEvent(GctTracking.CATEGORY, GctTracking.SAMPLES, sampleName, null);
    GradleProjectImporter.getInstance().importProject(project.getBaseDir());
    // TODO : display the correct starting file for users
    return true;
  }

  /**
   * Trim trailing and leading forward slashes from a string
   */
  @NotNull
  static String trimSlashes(@NotNull String path) {
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    while (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }
    return path;
  }
}
