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

package org.jetbrains.jps.appengine.builder;

import com.intellij.appengine.rt.EnhancerRunner;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.execution.ParametersListUtil;

import gnu.trove.THashSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.appengine.model.JpsAppEngineExtensionService;
import org.jetbrains.jps.appengine.model.JpsAppEngineModuleExtension;
import org.jetbrains.jps.appengine.model.PersistenceApi;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.FileProcessor;
import org.jetbrains.jps.builders.java.JavaBuilderUtil;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.builders.logging.ProjectBuilderLogger;
import org.jetbrains.jps.incremental.BuilderCategory;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ExternalProcessUtil;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class AppEngineEnhancerBuilder extends ModuleLevelBuilder {

  public static final String NAME = "Google AppEngine Enhancer";

  public AppEngineEnhancerBuilder() {
    super(BuilderCategory.CLASS_POST_PROCESSOR);
  }

  @Override
  public ExitCode build(final CompileContext context,
      ModuleChunk chunk,
      DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
      OutputConsumer outputConsumer)
      throws ProjectBuildException, IOException {

    boolean doneSomething = false;
    for (final JpsModule module : chunk.getModules()) {
      JpsAppEngineModuleExtension extension = JpsAppEngineExtensionService.getInstance()
          .getExtension(module);
      if (extension != null && extension.isRunEnhancerOnMake()) {
        doneSomething |= processModule(context, dirtyFilesHolder, extension);
      }
    }

    return doneSomething ? ExitCode.OK : ExitCode.NOTHING_DONE;
  }

  @Override
  public List<String> getCompilableFileExtensions() {
    return Collections.emptyList();
  }

  private static boolean processModule(final CompileContext context,
      DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
      JpsAppEngineModuleExtension extension) throws IOException, ProjectBuildException {
    final Set<File> roots = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
    for (String path : extension.getFilesToEnhance()) {
      roots.add(new File(FileUtil.toSystemDependentName(path)));
    }

    final List<String> pathsToProcess = new ArrayList<String>();
    dirtyFilesHolder
        .processDirtyFiles(new FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>() {
          @Override
          public boolean apply(ModuleBuildTarget target, File file, JavaSourceRootDescriptor root)
              throws IOException {
            if (JpsPathUtil.isUnder(roots, file)) {
              Collection<String> outputs = context.getProjectDescriptor().dataManager
                  .getSourceToOutputMap(target).getOutputs(file.getAbsolutePath());
              if (outputs != null) {
                pathsToProcess.addAll(outputs);
              }
            }
            return true;
          }
        });
    if (pathsToProcess.isEmpty()) {
      return false;
    }

    JpsModule module = extension.getModule();
    context.processMessage(
        new ProgressMessage("Enhancing classes in module '" + module.getName() + "'..."));

    List<String> classpath = new ArrayList<String>();
    classpath.add(extension.getToolsApiJarPath());
    classpath.add(PathManager.getJarPathForClass(EnhancerRunner.class));
    boolean removeOrmJars = Boolean
        .parseBoolean(System.getProperty("jps.appengine.enhancer.remove.orm.jars", "true"));
    for (File file : JpsJavaExtensionService.dependencies(module).recursively().compileOnly()
        .productionOnly().classes().getRoots()) {
      if (removeOrmJars && FileUtil.isAncestor(new File(extension.getOrmLibPath()), file, true)) {
        continue;
      }
      classpath.add(file.getAbsolutePath());
    }

    List<String> programParams = new ArrayList<String>();
    final File argsFile = FileUtil.createTempFile("appEngineEnhanceFiles", ".txt");
    PrintWriter writer = new PrintWriter(argsFile, StandardCharsets.UTF_8.name());
    try {
      for (String path : pathsToProcess) {
        writer.println(FileUtil.toSystemDependentName(path));
      }
    } finally {
      writer.close();
    }

    programParams.add(argsFile.getAbsolutePath());
    programParams.add("com.google.appengine.tools.enhancer.Enhance");
    programParams.add("-api");
    PersistenceApi api = extension.getPersistenceApi();
    programParams.add(api.getEnhancerApiName());
    if (api.getEnhancerVersion() == 2) {
      programParams.add("-enhancerVersion");
      programParams.add("v2");
    }
    programParams.add("-v");

    List<String> vmParams = Collections.singletonList("-Xmx256m");

    JpsSdk<JpsDummyElement> sdk = JavaBuilderUtil.ensureModuleHasJdk(module, context, NAME);
    List<String> commandLine = ExternalProcessUtil
        .buildJavaCommandLine(JpsJavaSdkType.getJavaExecutable(sdk), EnhancerRunner.class.getName(),
            Collections.<String>emptyList(), classpath, vmParams, programParams);

    Process process = new ProcessBuilder(commandLine).start();
    ExternalEnhancerProcessHandler handler = new ExternalEnhancerProcessHandler(process,
        commandLine, context);
    handler.startNotify();
    handler.waitFor();
    ProjectBuilderLogger logger = context.getLoggingManager().getProjectBuilderLogger();
    if (logger.isEnabled()) {
      logger.logCompiledPaths(pathsToProcess, NAME, "Enhancing classes:");
    }
    return true;
  }


  @NotNull
  @Override
  public String getPresentableName() {
    return NAME;
  }

  private static class ExternalEnhancerProcessHandler extends EnhancerProcessHandlerBase {

    private final CompileContext myContext;

    public ExternalEnhancerProcessHandler(Process process, List<String> commandLine,
        CompileContext context) {
      super(process, ParametersListUtil.join(commandLine), null);
      myContext = context;
    }

    @Override
    protected void reportInfo(String message) {
      myContext.processMessage(new CompilerMessage(NAME, BuildMessage.Kind.INFO, message));
    }

    @Override
    protected void reportError(String message) {
      myContext.processMessage(new CompilerMessage(NAME, BuildMessage.Kind.ERROR, message));
    }
  }
}
