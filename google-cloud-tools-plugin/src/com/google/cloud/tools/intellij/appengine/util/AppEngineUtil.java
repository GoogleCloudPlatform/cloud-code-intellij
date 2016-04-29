/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * App Engine utility methods
 */
public class AppEngineUtil {

  // Just to return two strings.
  public static class VersionService {
    public String version = null;
    public String service = null;
  }

  // Holds de-serialized JSON output of gcloud app deploy.
  private static class DeployOutput {
    private static class versionElement {
      String id;
      String service;
    }
    ArrayList<versionElement> versions;
  }

  private AppEngineUtil() {
    // Not designed for instantiation
  }

  public static VersionService parseDeployOutputToService(String jsonOutput)
      throws JsonParseException {
    /* An example JSON output of gcloud app deloy:
        {
          "configs": [],
          "versions": [
            {
              "id": "20160429t112518",
              "last_deployed_time": null,
              "project": "springboot-maven-project",
              "service": "default",
              "traffic_split": null,
              "version": null
            }
          ]
        }
    */

    Type deployOutputType = new TypeToken<DeployOutput>() {}.getType();
    DeployOutput deployOutput = new Gson().fromJson(jsonOutput, deployOutputType);
    if(deployOutput == null || deployOutput.versions.size() != 1) {
      throw new AssertionError("Expected a single module output from flex deployment.");
    }

    VersionService versionService = new VersionService();
    versionService.version = deployOutput.versions.get(0).id;
    versionService.service = deployOutput.versions.get(0).service;
    return versionService;
  }
}
