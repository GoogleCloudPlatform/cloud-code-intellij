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

import com.google.auto.value.AutoValue;
import com.intellij.openapi.application.ApplicationManager;
import java.util.ArrayList;
import org.picocontainer.MutablePicoContainer;

/**
 * Handles modifications to the {@link MutablePicoContainer} stored in the {@link
 * ApplicationManager} for tests that wish to mock registered components.
 */
public final class PicoContainerTestUtil {

  private static final PicoContainerTestUtil INSTANCE = new PicoContainerTestUtil();

  private final ArrayList<Component> components = new ArrayList<>();

  /** Prevents instantiation. */
  private PicoContainerTestUtil() {}

  /** Returns the static instance of this class. */
  public static PicoContainerTestUtil getInstance() {
    return INSTANCE;
  }

  /**
   * Replaces the registered component in the {@link MutablePicoContainer} with the given mocked
   * instance.
   *
   * <p>You should always call {@link #tearDown()} in the test's tear-down process if you make a
   * call to this method.
   *
   * @param clazz the class of the registered component
   * @param mockedInstance the mocked instance to register
   * @param <T> the type of the registered component
   */
  public <T> void replaceComponentWithMock(Class<T> clazz, T mockedInstance) {
    Object originalInstance = setComponent(clazz, mockedInstance);
    components.add(Component.create(clazz, originalInstance));
  }

  /** Tears down this utility's state by re-registering all of the original component instances. */
  public void tearDown() {
    components.forEach(component -> setComponent(component.clazz(), component.originalInstance()));
    components.clear();
  }

  /**
   * Replaces the component binding in the {@link MutablePicoContainer} with the given instance and
   * returns the original component instance.
   *
   * @param clazz the class of the registered component
   * @param newInstance the new instance to register
   */
  private static Object setComponent(Class<?> clazz, Object newInstance) {
    MutablePicoContainer applicationContainer =
        (MutablePicoContainer) ApplicationManager.getApplication().getPicoContainer();
    Object originalInstance = applicationContainer.getComponentInstanceOfType(clazz);
    applicationContainer.unregisterComponent(clazz.getName());
    applicationContainer.registerComponentInstance(clazz.getName(), newInstance);
    return originalInstance;
  }

  /** Represents a bound component in the {@link MutablePicoContainer}. */
  @AutoValue
  abstract static class Component {

    /** Returns a new instance for the given class and original component instance. */
    static Component create(Class<?> clazz, Object originalInstance) {
      return new AutoValue_PicoContainerTestUtil_Component(clazz, originalInstance);
    }

    /** The class of the bound component. */
    abstract Class<?> clazz();

    /** The original instance of the bound component. */
    abstract Object originalInstance();
  }
}
