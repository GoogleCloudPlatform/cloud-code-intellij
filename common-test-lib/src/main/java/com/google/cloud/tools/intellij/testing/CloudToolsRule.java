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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.intellij.testFramework.fixtures.BareTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
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
 *   <li>Creates a {@link com.intellij.testFramework.fixtures.BareTestFixture BareTestFixture} and
 *       handles setting it up and tearing it down
 *   <li>Uses the {@link PicoContainerTestUtil} to replace all fields annotated with {@link
 *       MockComponent} in the {@link org.picocontainer.PicoContainer PicoContainer} with the mocked
 *       field value. After the test finishes, it replaces the mocked value with the real registered
 *       component.
 * </ul>
 */
public final class CloudToolsRule implements TestRule {

  private final Object testInstance;
  private final BareTestFixture bareTestFixture;

  /** Returns a new instance for the given {@code testInstance}. */
  public CloudToolsRule(Object testInstance) {
    this.testInstance = Preconditions.checkNotNull(testInstance);
    bareTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        setUp();
        base.evaluate();
        tearDown();
      }
    };
  }

  /** Sets up utilities before the test runs. */
  private void setUp() throws Exception {
    MockitoAnnotations.initMocks(testInstance);
    bareTestFixture.setUp();

    for (Field field : getFieldsWithAnnotation(testInstance.getClass(), MockComponent.class)) {
      field.setAccessible(true);
      Object mockInstance = field.get(testInstance);
      PicoContainerTestUtil.getInstance().replaceComponentWithMock(field.getType(), mockInstance);
    }
  }

  /** Tears down utilities after the test has finished. */
  private void tearDown() throws Exception {
    bareTestFixture.tearDown();
    PicoContainerTestUtil.getInstance().tearDown();
  }

  /**
   * Returns the list of {@link Field fields} in the given {@code clazz} and its superclasses that
   * are annotated with the given {@code annotationClass}.
   *
   * @param clazz the {@link Class} to search for annotated fields
   * @param annotationClass the {@link Class} of the {@link Annotation} to search for
   */
  private static ImmutableList<Field> getFieldsWithAnnotation(
      Class<?> clazz, Class<? extends Annotation> annotationClass) {
    ImmutableList.Builder<Field> builder = ImmutableList.builder();
    for (Class<?> testClass = clazz; testClass != null; testClass = testClass.getSuperclass()) {
      Arrays.stream(testClass.getDeclaredFields())
          .filter(field -> field.isAnnotationPresent(annotationClass))
          .forEach(builder::add);
    }
    return builder.build();
  }
}
