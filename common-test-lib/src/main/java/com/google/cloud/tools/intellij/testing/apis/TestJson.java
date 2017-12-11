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

package com.google.cloud.tools.intellij.testing.apis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A helper for dealing with JSON in tests.
 *
 * <p>Classes that use AutoValue should extend from this if they are designed to represent some JSON
 * object. See {@link TestJson#toJson()} to see how the object is turned into JSON or see {@link
 * TestCloudLibrary} for an example.
 */
abstract class TestJson {

  /**
   * Returns the JSON representation of this object.
   *
   * <p>The built JSON will contain key/value pairs for every abstract method defined in this class.
   * The key is the name of the method and the value is the result of invoking the method on this
   * instance.
   *
   * <p>For all object types, the {@link Object#toString()} method is invoked to determine the
   * value. Note that for classes derived from {@link TestJson}, this is overridden to call this
   * method. There are a few special cases:
   *
   * <ol>
   *   <li>Strings: the JSON value is escaped with double-quotation marks (i.e. "value")
   *   <li>Collections: the JSON value is wrapped in array brackets (i.e. [value])
   * </ol>
   */
  protected String toJson() {
    String json =
        Arrays.stream(getClass().getSuperclass().getDeclaredMethods())
            .filter(method -> Modifier.isAbstract(method.getModifiers()))
            .flatMap(
                method -> {
                  try {
                    Object object = method.invoke(this);
                    if (object == null) {
                      return Stream.of();
                    }

                    if (Collection.class.isAssignableFrom(method.getReturnType())) {
                      Collection<?> objects = (Collection<?>) object;
                      String mergedResult =
                          objects.stream().map(Object::toString).collect(Collectors.joining(","));
                      return Stream.of(String.format("%s:[%s]", method.getName(), mergedResult));
                    }

                    String format = (object instanceof String) ? "%s:\"%s\"" : "%s:%s";
                    return Stream.of(String.format(format, method.getName(), object.toString()));
                  } catch (IllegalAccessException | InvocationTargetException e) {
                    return Stream.of();
                  }
                })
            .collect(Collectors.joining(","));
    return String.format("{%s}", json);
  }

  @Override
  public final String toString() {
    return toJson();
  }
}
