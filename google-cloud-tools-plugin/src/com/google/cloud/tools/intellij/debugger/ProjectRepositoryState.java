/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.debugger;

import com.intellij.openapi.util.Key;
import com.intellij.util.xmlb.annotations.Transient;

import git4idea.repo.GitRepository;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds state and performs operations related to source control contexts.
 */
public class ProjectRepositoryState {

  private static final Key<ProjectRepositoryState> REPO_KEY =
      Key.create("ProjectRepositoryState");
  private String originalBranchName;
  private GitRepository sourceRepository;
  private String stashMessage;

  private ProjectRepositoryState() {
  }

  /**
   * Given a {@link CloudDebugProcessState}.
   */
  @NotNull
  public static ProjectRepositoryState fromProcessState(
      @NotNull CloudDebugProcessState processState) {
    ProjectRepositoryState repoState = processState.getUserData(REPO_KEY);
    if (repoState == null) {
      repoState = new ProjectRepositoryState();
      processState.putUserData(REPO_KEY, repoState);
    }
    return repoState;
  }

  public void clearForNextSession() {
    setStashMessage(null);
  }

  /**
   * This is the branch the user was on before they started a debug session and we moved them to the
   * target SHA.
   */
  @Transient
  @Nullable
  protected String getOriginalBranchName() {
    return originalBranchName;
  }

  @Transient
  public void setOriginalBranchName(@Nullable String originalBranchName) {
    this.originalBranchName = originalBranchName;
  }

  /**
   * The source repository is used during stash/unstash and sync to perform Git operations. Right
   * now we only support Git.  If we added citc or other clients, this would need to be factored
   * out.
   */
  @Transient
  @Nullable
  protected GitRepository getSourceRepository() {
    return sourceRepository;
  }

  @Transient
  public void setSourceRepository(@Nullable GitRepository sourceRepository) {
    this.sourceRepository = sourceRepository;
  }

  /**
   * The stash message is how we identify which item to unstash when the session ends. Stashes are
   * ordered and may not necessarily have the same ordinal value because new stashes are inserted at
   * the top.
   */
  @Transient
  @Nullable
  protected String getStashMessage() {
    return stashMessage;
  }

  @Transient
  public void setStashMessage(@Nullable String stashMessage) {
    this.stashMessage = stashMessage;
  }

  /**
   * @return True if we have a valid git repo.
   */
  protected boolean hasSourceRepository() {
    return getSourceRepository() != null;
  }

}
