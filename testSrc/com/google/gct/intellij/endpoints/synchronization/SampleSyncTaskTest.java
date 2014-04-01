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
package com.google.gct.intellij.endpoints.synchronization;


import com.google.common.collect.Lists;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Tests for {@link SampleSyncTask}
 */
public class SampleSyncTaskTest extends RepositoryTestCase {
  private Git mockGitHubRepo;
  private String mockAndroidRepoPath;
  private  String mockGitHubRepoPath;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    mockAndroidRepoPath = System.getProperty("java.io.tmpdir") + "/android/mockAndroidRepo";
    String mockGitHubRepoGitPath = db.getDirectory().getPath();
    mockGitHubRepoPath = mockGitHubRepoGitPath.substring(0, mockGitHubRepoGitPath.lastIndexOf('/'));

    // Configure the mock github repo
    StoredConfig targetConfig = db.getConfig();
    targetConfig.setString("branch", "master", "remote", "origin");
    targetConfig.setString("branch", "master", "merge", "refs/heads/master");

    RemoteConfig config = new RemoteConfig(targetConfig, "origin");
    config.addURI(new URIish(mockGitHubRepoGitPath));
    config.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
    config.update(targetConfig);

    targetConfig.save();

    mockGitHubRepo = new Git(db);

    // commit something
    writeTrashFile("Test.txt", "Hello world");
    mockGitHubRepo.add().addFilepattern("Test.txt").call();
    mockGitHubRepo.commit().setMessage("Initial commit").call();
    mockGitHubRepo.tag().setName("tag-initial").setMessage("Tag initial").call();

  }

  @Override
  public void tearDown() throws IOException {
    FileUtils.forceDelete(new File(mockAndroidRepoPath));
  }

  @Ignore
  @Test
  public void testSync_noLocalRepo() throws IOException, GitAPIException {
    // Sync files from mock Git Hub repo to mock local Android sample template repo
    SampleSyncTask sampleSyncTask = new SampleSyncTask(mockAndroidRepoPath, mockGitHubRepoPath);
    sampleSyncTask.run();

    File mockAndroidRepoDir = new File(mockAndroidRepoPath);
    Assert.assertTrue(mockAndroidRepoDir.exists());

    Git mockAndroidRepo = Git.open(mockAndroidRepoDir);

    Assert.assertEquals("refs/heads/master", mockAndroidRepo.getRepository().getFullBranch());
    Assert.assertEquals(1, mockAndroidRepo.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().size());

    File mockGitHubRepoDir = new File(mockGitHubRepoPath);
    Assert.assertTrue(mockGitHubRepoDir.exists());

    File[] mockAndroidRepoFiles =  mockAndroidRepoDir.listFiles();
    File[] mockGitHubRepoFiles =  mockGitHubRepoDir.listFiles();

    Assert.assertEquals(mockGitHubRepoFiles.length, mockAndroidRepoFiles.length);

    int num = 0;
    for(File aFile : mockGitHubRepoFiles) {
      aFile.getName().equals(mockAndroidRepoFiles[0].getName());
      num++;
    }
  }

  @Ignore
  @Test
  public void testSync_singleCommit() throws GitAPIException, IOException, URISyntaxException {
    // Sync files from mock Git Hub repo to mock local Android sample template repo
    SampleSyncTask sampleSyncTask = new SampleSyncTask(mockAndroidRepoPath, mockGitHubRepoPath);
    sampleSyncTask.run();

    // Add a file to mock github repo
    RevCommit commit = addFileToMockGitHubRepo("a.txt", "Adding a.txt");

    // Sync files from mock Git Hub repo to mock local Android sample template repo
    sampleSyncTask.run();

    // Check that the last commit in the mock Android repo is the commit made to add a new file
    Git mockAndroidRepo = Git.open(new File(mockAndroidRepoPath));
    Iterable<RevCommit> logs = mockAndroidRepo.log().call();
    Assert.assertNotNull(logs);

    // Check that commits exist
    boolean hasCommits = false;

    for (RevCommit aLog : logs) {
      hasCommits = true;
      Assert.assertEquals(commit.getCommitTime(), aLog.getCommitTime());
      Assert.assertEquals(commit.getFullMessage(), aLog.getFullMessage());
      break;
    }

    Assert.assertTrue(hasCommits);
  }

  @Ignore
  @Test
  public void testSync_multipleCommits() throws GitAPIException, IOException {
    // Sync files from mock Git Hub repo to mock local Android sample template repo
    SampleSyncTask sampleSyncTask = new SampleSyncTask(mockAndroidRepoPath, mockGitHubRepoPath);
    sampleSyncTask.run();

    // Add 2 files to mock github repo
    RevCommit commit1 = addFileToMockGitHubRepo("a.txt", "Adding a.txt");
    Assert.assertNotNull(commit1);
    RevCommit commit2 = addFileToMockGitHubRepo("b.txt", "Adding b.txt");
    Assert.assertNotNull(commit2);

    // Delete a file from github repo
    RevCommit commit3 = removeFileFromMockGitHubRepo("a.txt", "Removing a.txt");
    Assert.assertNotNull(commit3);

    // Sync files from mock Git Hub repo to mock local Android sample template repo
    sampleSyncTask.run();

    // Check that last 3 commits in mock Android repo
    Git mockAndroidRepo = Git.open(new File(mockAndroidRepoPath));
    Iterable<RevCommit> logs = mockAndroidRepo.log().call();
    Assert.assertNotNull(logs);
    ArrayList<RevCommit> logsList = Lists.newArrayList(logs);

    Assert.assertTrue(logsList.size() >= 3);
    Assert.assertEquals(commit3.getCommitTime(), logsList.get(0).getCommitTime());
    Assert.assertEquals(commit3.getFullMessage(), logsList.get(0).getFullMessage());
    Assert.assertEquals(commit2.getCommitTime(), logsList.get(1).getCommitTime());
    Assert.assertEquals(commit2.getFullMessage(), logsList.get(1).getFullMessage());
    Assert.assertEquals(commit1.getCommitTime(), logsList.get(2).getCommitTime());
    Assert.assertEquals(commit1.getFullMessage(), logsList.get(2).getFullMessage());
  }

  private RevCommit addFileToMockGitHubRepo(String fileName, String commitMessage) throws GitAPIException, IOException {
    // Create a new file
    File file = new File(db.getWorkTree(), fileName);
    file.createNewFile();
    PrintWriter writer = new PrintWriter(file);
    writer.print("Some content");
    writer.close();

    // Add file to mocked Github repo
    mockGitHubRepo.add().addFilepattern(fileName).call();

    RevCommit commit = mockGitHubRepo.commit()
      .setMessage(commitMessage)
      .call();
    Assert.assertNotNull(commit);

    mockGitHubRepo.push().call();

    return commit;
  }

  private RevCommit removeFileFromMockGitHubRepo(String fileName, String commitMessage) throws GitAPIException {
    mockGitHubRepo.rm()
      .addFilepattern(fileName)
      .call();

    RevCommit commit = mockGitHubRepo.commit()
      .setMessage(commitMessage)
      .call();
    Assert.assertNotNull(commit);

    mockGitHubRepo.push().call();

    return commit;
  }
}
