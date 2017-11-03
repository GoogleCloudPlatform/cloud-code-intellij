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

package com.google.cloud.tools.intellij.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.messages.Topic;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/** Project utility for various {@link Project} related utilities. */
public final class ProjectUtil {

  private static final ProjectUtil INSTANCE = new ProjectUtil();

  private ProjectUtil() {}

  /** Return instance of this utility. */
  public static ProjectUtil getInstance() {
    return INSTANCE;
  }

  /** Subscribes all open projects to the given topic with the supplied handler. */
  public <L> void subscribeAll(@NotNull Topic<L> topic, @NotNull L handler) {
    Stream.of(ProjectManager.getInstance().getOpenProjects())
        .forEach(project -> project.getMessageBus().connect().subscribe(topic, handler));
  }
}
