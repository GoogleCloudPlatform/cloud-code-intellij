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

package com.google.cloud.tools.intellij.testing;

import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.MoreExecutors;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

/**
 * A custom {@link TestRule} for Cloud Tools unit tests.
 *
 * <p>This rule adds the following functionality:
 *
 * <ul>
 *   <li>Initializes mocks annotated with {@link org.mockito.Mock Mock}
 *   <li>Creates an {@link IdeaProjectTestFixture} and injects the value into any fields annotated
 *       with {@link TestFixture}
 *   <li>Uses the {@link PicoContainerTestUtil} to replace all fields annotated with {@link
 *       TestService} in the {@link org.picocontainer.PicoContainer PicoContainer} with the field
 *       values. After the test finishes, it re-registers the original services.
 *   <li>Creates {@link Module Modules} for any fields annotated with {@link TestModule} and adds
 *       them to the project
 *   <li>Creates {@link File Files} for any fields annotated with {@link TestFile} and manages the
 *       creation and deletion of them
 *   <li>Binds the {@link java.util.concurrent.ExecutorService} in {@link ThreadUtil} to a direct
 *       executor service, which executes every submitted task immediately and on the same thread
 *       that submitted the task
 * </ul>
 */
public final class CloudToolsRule implements TestRule {

  private final Object testInstance;
  private final List<File> filesToDelete = new ArrayList<>();
  private final AtomicInteger moduleCounter = new AtomicInteger();
  private IdeaProjectTestFixture testFixture;

  /** Returns a new instance for the given {@code testInstance}. */
  public CloudToolsRule(Object testInstance) {
    this.testInstance = Preconditions.checkNotNull(testInstance);
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        setUp(description);
        try {
          base.evaluate();
        } finally {
          tearDown();
        }
      }
    };
  }

  /** Sets up utilities before the test runs. */
  private void setUp(Description description) throws Exception {
    MockitoAnnotations.initMocks(testInstance);
    testFixture =
        IdeaTestFixtureFactory.getFixtureFactory()
            .createFixtureBuilder(description.getMethodName())
            .getFixture();
    testFixture.setUp();

    populateTestFixture();
    replaceServices();
    createTestModules();
    createTestFiles(description.getMethodName());
    createTestDirectories();
    bindDirectExecutorService();
  }

  /** Tears down utilities after the test has finished. */
  private void tearDown() throws Exception {
    PicoContainerTestUtil.getInstance().tearDown();
    filesToDelete.forEach(File::delete);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              try {
                testFixture.tearDown();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  /** Populates all fields annotated with {@link TestFixture} with the created test fixture. */
  private void populateTestFixture() throws IllegalAccessException {
    for (Field field : getFieldsWithAnnotation(testInstance.getClass(), TestFixture.class)) {
      field.setAccessible(true);
      field.set(testInstance, testFixture);
    }
  }

  /**
   * Replaces all services annotated with {@link TestService} using the {@link
   * PicoContainerTestUtil}.
   */
  private void replaceServices() throws IllegalAccessException {
    for (Field field : getFieldsWithAnnotation(testInstance.getClass(), TestService.class)) {
      field.setAccessible(true);
      Object service = field.get(testInstance);
      PicoContainerTestUtil.getInstance().replaceServiceWithInstance(field.getType(), service);
    }
  }

  /** Creates all {@link Module modules} annotated with {@link TestModule}. */
  private void createTestModules() throws IllegalAccessException {
    for (Field field : getFieldsWithAnnotation(testInstance.getClass(), TestModule.class)) {
      field.setAccessible(true);
      if (!field.getType().equals(Module.class)) {
        throw new IllegalArgumentException(
            "@TestModule can only annotate fields of type com.intellij.openapi.module.Module");
      }

      writeOnMainThread(
          () -> {
            Module module =
                ModuleManager.getInstance(testFixture.getProject())
                    .newModule(
                        testFixture.getProject().getBasePath()
                            + "/"
                            + moduleCounter.incrementAndGet()
                            + ModuleFileType.DOT_DEFAULT_EXTENSION,
                        ModuleType.EMPTY.getId());
            field.set(testInstance, module);

            String facetTypeId = field.getAnnotation(TestModule.class).facetTypeId();
            if (!Strings.isNullOrEmpty(facetTypeId)) {
              FacetType<?, ?> facetType =
                  FacetTypeRegistry.getInstance().findFacetType(facetTypeId);
              FacetManager.getInstance(module)
                  .addFacet(facetType, facetTypeId, /* underlying= */ null);
            }
          });
    }
  }

  /**
   * Creates all {@link File files} annotated with {@link TestFile} in the given directory name.
   *
   * @param directoryName the name of the directory to create the test files in
   */
  private void createTestFiles(String directoryName) throws IllegalAccessException, IOException {
    for (Field field : getFieldsWithAnnotation(testInstance.getClass(), TestFile.class)) {
      field.setAccessible(true);
      if (!field.getType().equals(File.class)) {
        throw new IllegalArgumentException(
            "@TestFile can only annotate fields of type java.io.File");
      }

      TestFile annotation = field.getAnnotation(TestFile.class);
      File directory = FileUtil.createTempDirectory(directoryName, null);
      File file = new File(directory, annotation.name());
      if (!file.createNewFile()) {
        throw new IOException("Can't create file: " + file);
      }
      if (!annotation.contents().isEmpty()) {
        FileUtil.writeToFile(file, annotation.contents());
      }

      filesToDelete.add(file);
      field.set(testInstance, file);
    }
  }

  /**
   * Creates all directories, represented as {@link File Files}, annotated with {@link
   * TestDirectory}.
   */
  private void createTestDirectories() throws IllegalAccessException, IOException {
    for (Field field : getFieldsWithAnnotation(testInstance.getClass(), TestDirectory.class)) {
      field.setAccessible(true);
      if (!field.getType().equals(File.class)) {
        throw new IllegalArgumentException(
            "@TestDirectory can only annotate fields of type java.io.File");
      }

      TestDirectory annotation = field.getAnnotation(TestDirectory.class);
      File directory = FileUtil.createTempDirectory(annotation.name(), null);
      filesToDelete.add(directory);
      field.set(testInstance, directory);
    }
  }

  /** Binds the executor service in {@link ThreadUtil} to a direct executor service. */
  private void bindDirectExecutorService() {
    ThreadUtil.getInstance().setBackgroundExecutorService(MoreExecutors.newDirectExecutorService());
  }

  /**
   * Returns the list of {@link Field fields} in the given {@code clazz} that are annotated with the
   * given {@code annotationClass}.
   *
   * @param clazz the {@link Class} to search for annotated fields
   * @param annotationClass the {@link Class} of the {@link Annotation} to search for
   */
  private static List<Field> getFieldsWithAnnotation(
      Class<?> clazz, Class<? extends Annotation> annotationClass) {
    return Arrays.stream(clazz.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(annotationClass))
        .collect(Collectors.toList());
  }

  /** Runs the given {@link ThrowingRunnable} as a write action on the main thread. */
  private static void writeOnMainThread(ThrowingRunnable runnable) {
    ApplicationManager.getApplication()
        .invokeAndWait(() -> ApplicationManager.getApplication().runWriteAction(wrap(runnable)));
  }

  /**
   * Wraps the given {@link ThrowingRunnable} in a {@link Runnable} that transforms all {@link
   * Throwable throwables} to a {@link RuntimeException}.
   */
  private static Runnable wrap(ThrowingRunnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    };
  }
}
