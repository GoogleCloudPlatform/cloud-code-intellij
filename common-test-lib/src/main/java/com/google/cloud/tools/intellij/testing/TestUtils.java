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

import static org.junit.Assert.fail;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.intellij.mock.MockApplicationEx;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingManagerImpl;
import com.intellij.util.pico.DefaultPicoContainer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Test utilities.
 */
public class TestUtils {

  private static Disposable parentDisposableForCleanup;

  @NotNull
  public static Project mockProject() {
    return mockProject(null);
  }

  /**
   * Construct a mock project.
   */
  @NotNull
  public static MockProject mockProject(@Nullable PicoContainer container) {
    Extensions.registerAreaClass("IDEA_PROJECT", null);
    container = container != null
        ? container
        : new DefaultPicoContainer();
    return new MockProject(container, getParentDisposableForCleanup());
  }

  static class PluginMockApplication extends MockApplicationEx {

    private final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();

    public PluginMockApplication(@NotNull Disposable parentDisposable) {
      super(parentDisposable);
    }

    @NotNull
    @Override
    public Future<?> executeOnPooledThread(@NotNull Runnable action) {
      return executor.submit(action);
    }

    @NotNull
    @Override
    public <T> Future<T> executeOnPooledThread(@NotNull Callable<T> action) {
      return executor.submit(action);
    }
  }

  /**
   * For every #createMockApplication there needs to be a corresponding call to
   * #disposeMockApplication when the test is complete.
   */
  public static Disposable createMockApplication() {
    Disposable parentDisposable = getParentDisposableForCleanup();

    final PluginMockApplication instance = new PluginMockApplication(parentDisposable);

    ApplicationManager.setApplication(instance,
        new Getter<FileTypeRegistry>() {
          @Override
          public FileTypeRegistry get() {
            return FileTypeManager.getInstance();
          }
        },
        parentDisposable);
    instance.registerService(EncodingManager.class, EncodingManagerImpl.class);
    return parentDisposable;
  }

  /**
   * Cleanup.
   */
  public static void disposeMockApplication() {
    // Originally the application was replaced with an empty mock application to make any subsequent
    // test cases fail that do not setup their own application. However having quite many legacy
    // tests that do rely on the application object created by IntelliJ when starting the tests
    // we'll just restore the application used previously this test.
    Disposer.dispose(getParentDisposableForCleanup());
    parentDisposableForCleanup = null;
  }

  private static Disposable getParentDisposableForCleanup() {
    synchronized (BasePluginTestCase.class) {
      if (parentDisposableForCleanup == null) {
        parentDisposableForCleanup = Mockito.mock(Disposable.class);
      }
      return parentDisposableForCleanup;
    }
  }

  /**
   * Register a service class with the container.
   */
  @NotNull
  public static <T> T installMockService(@NotNull Class<T> serviceInterface) {
    T mock = Mockito.mock(serviceInterface);
    MutablePicoContainer picoContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();
    picoContainer.unregisterComponent(serviceInterface.getName());
    picoContainer.registerComponentInstance(serviceInterface.getName(), mock);
    return mock;
  }

  /**
   * Serialize input and fail on exception.
   */
  public static void assertIsSerializable(@NotNull Serializable object) {
    ObjectOutputStream out = null;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      out = new ObjectOutputStream(byteArrayOutputStream);
      out.writeObject(object);
    } catch (NotSerializableException nse) {
      fail("An object is not serializable: " + nse.getMessage());
    } catch (IOException ioe) {
      fail("Could not serialize object: " + ioe.getMessage());
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }
  }
}
