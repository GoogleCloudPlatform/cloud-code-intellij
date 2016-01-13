package com.google.gct.idea.debugger;

import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the result of comparing local source to the source the cloud debugger is
 * attached to on the server.
 */
public class SyncResult {
  private final boolean myInvalidDebuggee;

  private final boolean myNeedsStash;
  private final boolean myNeedsSync;
  private final GitRepository myTargetLocalRepository;
  private final boolean myHasRemoteRepository;
  private final String myTargetSyncSHA;
  private final String myRepoType;

  SyncResult(boolean invalidDebuggee,
             boolean needsStash,
             boolean needsSync,
             @Nullable String targetSyncSHA,
             @Nullable GitRepository targetRepository,
             boolean hasRemoteRepository,
             String repoType) {
    myInvalidDebuggee = invalidDebuggee;
    myNeedsStash = needsStash;
    myNeedsSync = needsSync;
    myTargetSyncSHA = targetSyncSHA;
    myTargetLocalRepository = targetRepository;
    myHasRemoteRepository = hasRemoteRepository;
    myRepoType = repoType;
  }

  public GitRepository getLocalRepository() {
    return myTargetLocalRepository;
  }

  public boolean hasLocalRepository() {
    return myTargetLocalRepository != null;
  }

  @Nullable
  public boolean hasRemoteRepository() {
    return myHasRemoteRepository;
  }

  @Nullable
  public String getTargetSyncSHA() {
    return myTargetSyncSHA;
  }

  public boolean isValidDebuggee() {
    return !myInvalidDebuggee;
  }

  /**
   * Whether the local repository has uncommitted changes we need to stash
   */
  public boolean needsStash() {
    return myNeedsStash;
  }

  /**
   * Return the kind of remote repository found, or null if no matching repository was found
   */
  @Nullable
  public String getRepositoryType() {
    return myRepoType;
  }

  /**
   * Whether the local repository needs to be synced with the remote repository
   */
   public boolean needsSync() {
    return myNeedsSync;
   }

}
