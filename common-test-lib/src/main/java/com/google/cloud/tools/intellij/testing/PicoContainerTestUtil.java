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
 * ApplicationManager} for tests that wish to replace registered services for testing purposes.
 */
final class PicoContainerTestUtil {

  private static final PicoContainerTestUtil INSTANCE = new PicoContainerTestUtil();

  private final ArrayList<Service> services = new ArrayList<>();

  /** Prevents instantiation. */
  private PicoContainerTestUtil() {}

  /** Returns the static instance of this class. */
  static PicoContainerTestUtil getInstance() {
    return INSTANCE;
  }

  /**
   * Replaces the registered service in the {@link MutablePicoContainer} with the given instance.
   *
   * <p>You should always call {@link #tearDown()} in the test's tear-down process if you make a
   * call to this method.
   *
   * @param clazz the class of the registered service
   * @param instance the new instance to register
   */
  void replaceServiceWithInstance(Class<?> clazz, Object instance) {
    Object originalInstance = setService(clazz, instance);
    services.add(Service.create(clazz, originalInstance));
  }

  /** Tears down this utility's state by re-registering all of the original service instances. */
  void tearDown() {
    services.forEach(service -> setService(service.clazz(), service.originalInstance()));
    services.clear();
  }

  /**
   * Replaces the service binding in the {@link MutablePicoContainer} with the given instance and
   * returns the original service instance.
   *
   * @param clazz the class of the registered service
   * @param newInstance the new instance to register
   */
  private static Object setService(Class<?> clazz, Object newInstance) {
    MutablePicoContainer applicationContainer =
        (MutablePicoContainer) ApplicationManager.getApplication().getPicoContainer();
    Object originalInstance = applicationContainer.getComponentInstanceOfType(clazz);
    applicationContainer.unregisterComponent(clazz.getName());
    applicationContainer.registerComponentInstance(clazz.getName(), newInstance);
    return originalInstance;
  }

  /** Represents a bound service in the {@link MutablePicoContainer}. */
  @AutoValue
  abstract static class Service {

    /** Returns a new instance for the given class and original service instance. */
    static Service create(Class<?> clazz, Object originalInstance) {
      return new AutoValue_PicoContainerTestUtil_Service(clazz, originalInstance);
    }

    /** The class of the bound service. */
    abstract Class<?> clazz();

    /** The original instance of the bound service. */
    abstract Object originalInstance();
  }
}
