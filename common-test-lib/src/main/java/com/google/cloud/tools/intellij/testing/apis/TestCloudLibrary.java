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

import com.google.auto.value.AutoValue;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClient;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A JSON object for {@link CloudLibrary} instances that need to be built in unit tests.
 *
 * <p>The method names must match with the names of the fields in {@link CloudLibrary}.
 */
@AutoValue
public abstract class TestCloudLibrary extends TestJson {

  /**
   * Returns an empty instance.
   *
   * <p>All string parameters are empty strings (i.e. "") and embedded {@link TestJson} types are
   * built with empty strings too.
   */
  public static TestCloudLibrary createEmpty() {
    return create("", "", "", "", "", "", ImmutableList.of(TestCloudLibraryClient.createEmpty()));
  }

  /**
   * Returns a new instance for the given parameters.
   *
   * @see CloudLibrary
   */
  public static TestCloudLibrary create(
      String name,
      String id,
      String serviceName,
      String documentation,
      String description,
      String icon,
      List<TestCloudLibraryClient> clients) {
    return new AutoValue_TestCloudLibrary(
        name, id, serviceName, documentation, description, icon, clients);
  }

  /** @see CloudLibrary#name */
  public abstract String name();

  /** @see CloudLibrary#id */
  public abstract String id();

  /** @see CloudLibrary#serviceName */
  public abstract String serviceName();

  /** @see CloudLibrary#documentation */
  public abstract String documentation();

  /** @see CloudLibrary#description */
  public abstract String description();

  /** @see CloudLibrary#icon */
  public abstract String icon();

  /** @see CloudLibrary#clients */
  @Nullable
  public abstract List<TestCloudLibraryClient> clients();

  /** Returns a newly built {@link CloudLibrary} for the parameters in this class. */
  public CloudLibrary toCloudLibrary() {
    return new Gson().fromJson(toJson(), CloudLibrary.class);
  }

  /**
   * A JSON object for {@link CloudLibraryClient} instances that need to be built in unit tests.
   *
   * <p>The method names must match with the names of the fields in {@link CloudLibraryClient}.
   */
  @AutoValue
  public abstract static class TestCloudLibraryClient extends TestJson {

    /**
     * Returns an empty instance.
     *
     * <p>All string parameters are empty strings (i.e. "") and embedded {@link TestJson} types are
     * built with empty strings too.
     */
    public static TestCloudLibraryClient createEmpty() {
      return create("", "", "", "", "", "", TestCloudLibraryClientMavenCoordinates.createEmpty());
    }

    /**
     * Returns a new instance for the given parameters.
     *
     * @see CloudLibraryClient
     */
    public static TestCloudLibraryClient create(
        String name,
        String language,
        String apireference,
        String launchStage,
        String source,
        String languageLevel,
        TestCloudLibraryClientMavenCoordinates mavenCoordinates) {
      return new AutoValue_TestCloudLibrary_TestCloudLibraryClient(
          name, language, apireference, launchStage, source, languageLevel, mavenCoordinates);
    }

    /** @see CloudLibraryClient#name */
    public abstract String name();

    /** @see CloudLibraryClient#language */
    public abstract String language();

    /** @see CloudLibraryClient#apireference */
    public abstract String apireference();

    /** @see CloudLibraryClient#launchStage */
    public abstract String launchStage();

    /** @see CloudLibraryClient#source */
    public abstract String source();

    /** @see CloudLibraryClient#languageLevel */
    public abstract String languageLevel();

    /** @see CloudLibraryClient#mavenCoordinates */
    @Nullable
    public abstract TestCloudLibraryClientMavenCoordinates mavenCoordinates();

    /** Returns a newly built {@link CloudLibraryClient} for the parameters in this class. */
    public CloudLibraryClient toCloudLibraryClient() {
      return new Gson().fromJson(toJson(), CloudLibraryClient.class);
    }
  }

  /**
   * A JSON object for {@link CloudLibraryClientMavenCoordinates} instances that need to be built in
   * unit tests.
   *
   * <p>The method names must match with the names of the fields in {@link
   * CloudLibraryClientMavenCoordinates}.
   */
  @AutoValue
  public abstract static class TestCloudLibraryClientMavenCoordinates extends TestJson {

    /**
     * Returns an empty instance.
     *
     * <p>All string parameters are empty strings (i.e. "") and embedded {@link TestJson} types are
     * built with empty strings too.
     */
    public static TestCloudLibraryClientMavenCoordinates createEmpty() {
      return create("", "", "");
    }

    /**
     * Returns a new instance for the given parameters.
     *
     * @see CloudLibraryClientMavenCoordinates
     */
    public static TestCloudLibraryClientMavenCoordinates create(
        String groupId, String artifactId, String version) {
      return new AutoValue_TestCloudLibrary_TestCloudLibraryClientMavenCoordinates(
          groupId, artifactId, version);
    }

    /** @see CloudLibraryClientMavenCoordinates#groupId */
    public abstract String groupId();

    /** @see CloudLibraryClientMavenCoordinates#artifactId */
    public abstract String artifactId();

    /** @see CloudLibraryClientMavenCoordinates#version */
    public abstract String version();
  }
}
