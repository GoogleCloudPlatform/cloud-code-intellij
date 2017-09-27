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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * A proxy {@link ExecutorService} that delays the execution of {@link
 * ExecutorService#submit(Runnable)}. This proxy instead stores the supplied {@link Runnable} and
 * then executes it only after {@link #doSubmit()} is invoked.
 */
public final class DelayedSubmitExecutorServiceProxy extends AbstractExecutorService {

  private final ExecutorService executor;
  private Runnable task;

  /** Creates a new instance that is a proxy for the given {@code executor}. */
  public DelayedSubmitExecutorServiceProxy(ExecutorService executor) {
    this.executor = executor;
  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    return executor.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, @NotNull TimeUnit unit)
      throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

  @Override
  public void execute(@NotNull Runnable command) {
    executor.execute(command);
  }

  /** Stores the submitted {@code task} to be submitted later on {@link #doSubmit()}. */
  @Override
  public Future<?> submit(Runnable task) {
    this.task = task;
    return new FutureTask<>(task, null);
  }

  /** Submits the stored task. */
  public Future<?> doSubmit() {
    if (task == null) {
      throw new AssertionError("There is no task to submit.");
    }
    return executor.submit(task);
  }
}
