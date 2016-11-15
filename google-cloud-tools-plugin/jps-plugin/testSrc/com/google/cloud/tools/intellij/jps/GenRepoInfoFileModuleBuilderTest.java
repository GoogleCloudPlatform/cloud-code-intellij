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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.debug.GenRepoInfoFile;
import com.google.cloud.tools.appengine.api.debug.GenRepoInfoFileConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ExitCodeRecorderProcessExitListener;
import com.google.cloud.tools.intellij.jps.GenRepoInfoFileModuleBuilder.GenRepoInfoFileActionFactory;
import com.google.cloud.tools.intellij.jps.model.impl.JpsStackdriverModuleExtensionImpl;
import com.google.cloud.tools.intellij.jps.model.impl.StackdriverProperties;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode;
import org.jetbrains.jps.incremental.ModuleLevelBuilder.OutputConsumer;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.model.JpsElementContainer;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaModuleExtension;
import org.jetbrains.jps.model.java.JpsJavaProjectExtension;
import org.jetbrains.jps.model.java.impl.JavaModuleExtensionRole;
import org.jetbrains.jps.model.java.impl.JavaProjectExtensionRole;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class GenRepoInfoFileModuleBuilderTest {

  @Mock
  private CompileContext context;
  @Mock
  private ModuleChunk chunk;
  @Mock
  private DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder;
  @Mock
  private OutputConsumer outputConsumer;
  @Mock
  private GenRepoInfoFile genRepoInfoFile;
  @Mock
  private GenRepoInfoFileActionFactory actionFactory;
  @Mock
  private JpsModule module1;
  @Mock
  private JpsModule module2;
  @Mock
  private JpsModule module3;
  @Mock
  private JpsElementContainer container1;
  @Mock
  private JpsElementContainer container2;
  @Mock
  private JpsElementContainer container3;
  @Mock
  private ExitCodeRecorderProcessExitListener exitListener1;
  @Mock
  private JpsModuleSourceRoot sourceRoot;
  @Mock
  private JpsProject project;
  @Mock
  private JpsJavaModuleExtension javaModuleExtension;
  @Mock
  private JpsJavaProjectExtension javaProjectExtension;

  @Before
  public void setUp() throws IOException {
    when(actionFactory.newAction(isA(Path.class))).thenReturn(genRepoInfoFile);
    when(actionFactory.getExitListener()).thenReturn(exitListener1);
    doNothing().when(genRepoInfoFile).generate(isA(GenRepoInfoFileConfiguration.class));
    when(chunk.getModules()).thenReturn(ImmutableSet.of(module1));
    when(module1.getContainer()).thenReturn(container1);
    when(module1.getSourceRoots()).thenReturn(ImmutableList.of(sourceRoot));
    when(module1.getName()).thenReturn("module1");
    when(module1.getProject()).thenReturn(project);
    when(module2.getContainer()).thenReturn(container2);
    when(module2.getSourceRoots()).thenReturn(ImmutableList.of(sourceRoot));
    when(module2.getName()).thenReturn("module2");
    when(module2.getProject()).thenReturn(project);
    when(module3.getContainer()).thenReturn(container3);
    when(module3.getSourceRoots()).thenReturn(ImmutableList.of(sourceRoot));
    when(module3.getName()).thenReturn("module3");
    when(module3.getProject()).thenReturn(project);

    when(container1.getChild(JavaModuleExtensionRole.INSTANCE)).thenReturn(
        javaModuleExtension);
    when(container2.getChild(JavaModuleExtensionRole.INSTANCE)).thenReturn(
        javaModuleExtension);
    when(container3.getChild(JavaModuleExtensionRole.INSTANCE)).thenReturn(
        javaModuleExtension);
    when(container1.getChild(JavaProjectExtensionRole.INSTANCE)).thenReturn(javaProjectExtension);
    when(container2.getChild(JavaProjectExtensionRole.INSTANCE)).thenReturn(javaProjectExtension);
    when(container3.getChild(JavaProjectExtensionRole.INSTANCE)).thenReturn(javaProjectExtension);
    when(javaModuleExtension.isInheritOutput()).thenReturn(true);
    when(project.getContainer()).thenReturn(container1);
    when(javaProjectExtension.getOutputUrl()).thenReturn("url");
    doNothing().when(outputConsumer).registerOutputFile(
        isA(BuildTarget.class), isA(File.class), isA(Collection.class));
  }

  @Test
  public void testGen() throws ProjectBuildException, IOException {
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, true, "sdkPath1", "source1")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(0);

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    verify(genRepoInfoFile, times(1)).generate(isA(GenRepoInfoFileConfiguration.class));
    assertEquals(result, ExitCode.OK);
  }

  @Test
  public void testNotGen() throws ProjectBuildException, IOException {
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(false, true, "sdkPath1", "source1")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(1);

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    assertEquals(result, ExitCode.OK);
  }

  @Test
  public void testIgnore() throws ProjectBuildException, IOException {
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, true, "sdkPath1", "source1")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(1);

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    assertEquals(result, ExitCode.OK);
  }

  @Test
  public void testNotIgnore() throws ProjectBuildException, IOException {
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, false, "sdkPath1", "source1")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(1);

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    assertEquals(result, ExitCode.ABORT);
  }

  @Test
  public void testNullCloudSdk() throws ProjectBuildException, IOException {
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, true, null, "source1")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(1);

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    assertEquals(result, ExitCode.NOTHING_DONE);
  }

  @Test
  public void testEmptySourceRoots() throws ProjectBuildException, IOException {
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, true, null, "source1")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(1);
    when(module1.getSourceRoots()).thenReturn(Collections.<JpsModuleSourceRoot>emptyList());

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    assertEquals(result, ExitCode.OK);
  }

  @Test
  public void testMultipleModules() throws ProjectBuildException, IOException {
    when(chunk.getModules()).thenReturn(ImmutableSet.of(module1, module2, module3));
    when(container1.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, true, "sdkPath1", "source1")));
    when(container2.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(true, true, "sdkPath2", "source2")));
    when(container3.getChild(JpsStackdriverModuleExtensionImpl.ROLE)).thenReturn(
        new JpsStackdriverModuleExtensionImpl(
            new StackdriverProperties(false, true, "SdkPath3", "source3")));
    when(exitListener1.getMostRecentExitCode()).thenReturn(0);

    GenRepoInfoFileModuleBuilder subject = new GenRepoInfoFileModuleBuilder(actionFactory);
    ExitCode result = subject.build(context, chunk, dirtyFilesHolder, outputConsumer);

    verify(genRepoInfoFile, times(2)).generate(isA(GenRepoInfoFileConfiguration.class));
    assertEquals(result, ExitCode.OK);
  }
}