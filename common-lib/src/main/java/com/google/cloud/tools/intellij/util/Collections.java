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

package com.google.cloud.tools.intellij.util;

import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper methods for collection operations.
 */
public class Collections {

  /**
   * @return read-only collection consisting of elements from both collections
   */
  @NotNull
  @Contract(pure = true)
  public static <T> Collection<T> intersection(@NotNull Collection<? extends T> collection1,
      @NotNull Collection<? extends T> collection2) {
    List<T> result = new ArrayList<T>();
    for (T t : collection1) {
      if (collection2.contains(t)) {
        result.add(t);
      }
    }
    return result.isEmpty() ? ContainerUtil.<T>emptyList() : result;
  }
}
