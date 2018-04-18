/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.debugger;

import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the result of comparing local source to the source the cloud debugger is attached to on
 * the server.
 */
public class SyncResult {

  private final boolean invalidDebuggee;

  private final boolean needsStash;
  private final boolean needsSync;
  private final GitRepository targetLocalRepository;
  private final boolean hasRemoteRepository;
  private final String targetSyncSha;
  private final String repoType;

  SyncResult(
      boolean invalidDebuggee,
      boolean needsStash,
      boolean needsSync,
      @Nullable String targetSyncSha,
      @Nullable GitRepository targetRepository,
      boolean hasRemoteRepository,
      String repoType) {
    this.invalidDebuggee = invalidDebuggee;
    this.needsStash = needsStash;
    this.needsSync = needsSync;
    this.targetSyncSha = targetSyncSha;
    targetLocalRepository = targetRepository;
    this.hasRemoteRepository = hasRemoteRepository;
    this.repoType = repoType;
  }

  public GitRepository getLocalRepository() {
    return targetLocalRepository;
  }

  public boolean hasLocalRepository() {
    return targetLocalRepository != null;
  }

  public boolean hasRemoteRepository() {
    return hasRemoteRepository;
  }

  @Nullable
  public String getTargetSyncSha() {
    return targetSyncSha;
  }

  public boolean isValidDebuggee() {
    return !invalidDebuggee;
  }

  /** Whether the local repository has uncommitted changes we need to stash. */
  public boolean needsStash() {
    return needsStash;
  }

  /** Return the kind of remote repository found, or null if no matching repository was found. */
  @Nullable
  public String getRepositoryType() {
    return repoType;
  }

  /** Whether the local repository needs to be synced with the remote repository. */
  public boolean needsSync() {
    return needsSync;
  }
}
