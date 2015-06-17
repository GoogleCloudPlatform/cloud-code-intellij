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
package com.google.gct.idea.appengine.synchronization;

import com.intellij.openapi.diagnostic.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;

/**
 * Runnable that create and synchronized the local sample repo with a github repo.
 */
public class SampleSyncTask implements Runnable  {
  public static final Logger LOG =  Logger.getInstance(SampleSyncTask.class);

  private static SampleSyncTask instance;
  private final String ANDROID_REPO_PATH = System.getProperty("user.home") + "/.android/cloud/templates";
  private final String GITHUB_SAMPLE_LINK = "https://github.com/GoogleCloudPlatform/gradle-appengine-templates.git";
  private final String LOCAL_REPO_PATH;
  private final String SAMPLE_LINK;

  public static SampleSyncTask getInstance() {
    if(instance == null) {
      instance = new SampleSyncTask();
    }
    return instance;
  }

  @Override
  public void run() {
    updateSampleRepo();
  }

  @TestOnly
  public SampleSyncTask(String testRepoDirectory, String testSampleLink) {
    LOCAL_REPO_PATH = testRepoDirectory;
    SAMPLE_LINK = testSampleLink;
  }

  private SampleSyncTask() {
    LOCAL_REPO_PATH = ANDROID_REPO_PATH;
    SAMPLE_LINK = GITHUB_SAMPLE_LINK;
  }

  private void updateSampleRepo(){
    Git localGitRepo = getLocalRepo();

    if (localGitRepo == null) {
      // clone repo
      localGitRepo = cloneGithubRepo();
    }  else {
      // sync repo
      try {
        localGitRepo.pull().call();
      }
      catch (GitAPIException e) {
        LOG.error("Error syncing local sample repository", e);
      }
    }

    if(localGitRepo != null) {
      localGitRepo.getRepository().close();
    }
  }

  private Git getLocalRepo() {
    File localRepo = new File(LOCAL_REPO_PATH);

    if(localRepo.exists()) {
      try {
        return Git.open(localRepo);
      }
      catch (IOException e) {
        LOG.error("Error getting the local sample repository", e);
      }
    }
    return null;
  }

  private Git cloneGithubRepo() {
    File localRepoDirectory = new File(LOCAL_REPO_PATH);
    if(localRepoDirectory.exists()) {
      try {
        FileUtils.forceDelete(localRepoDirectory);
      }
      catch (IOException e) {
        LOG.error("Error deleting " + LOCAL_REPO_PATH, e);
      }
    }

    try {
      Git localGitRepo = Git.cloneRepository()
        .setURI(SAMPLE_LINK)
        .setDirectory(new File(LOCAL_REPO_PATH))
        .call();
      return localGitRepo;
    }
    catch (GitAPIException e) {
      LOG.error("Error cloning github sample repository: " + SAMPLE_LINK, e);
    }

    return null;
  }
}
