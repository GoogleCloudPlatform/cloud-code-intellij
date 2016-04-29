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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil.VersionService;

import org.junit.Test;

/**
 * Tests App Engine Utilities
 *
 */
public class AppEngineUtilTest {

  @Test
  public void testJsonDeployOutputJsonParsingOneVersion() {
    String jsonOutput = "{\n" +
                        "  \"configs\": [],\n" +
                        "  \"versions\": [\n" +
                        "    {\n" +
                        "      \"id\": \"20160429t112518\",\n" +
                        "      \"last_deployed_time\": null,\n" +
                        "      \"project\": \"some-project\",\n" +
                        "      \"service\": \"default\",\n" +
                        "      \"traffic_split\": null,\n" +
                        "      \"version\": null\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

    VersionService versionService = AppEngineUtil.parseDeployOutputToService(jsonOutput);
    assertEquals(versionService.version, "20160429t112518");
    assertEquals(versionService.service, "default");
  }

  @Test
  public void testJsonDeployOutputJsonParsingTwoVersions() {
    String jsonOutput = "{\n" +
                        "  \"configs\": [],\n" +
                        "  \"versions\": [\n" +
                        "    {\n" +
                        "      \"id\": \"20160429t112518\",\n" +
                        "      \"last_deployed_time\": null,\n" +
                        "      \"project\": \"some-project\",\n" +
                        "      \"service\": \"default\",\n" +
                        "      \"traffic_split\": null,\n" +
                        "      \"version\": null\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"20160429t112518\",\n" +
                        "      \"last_deployed_time\": null,\n" +
                        "      \"project\": \"some-project\",\n" +
                        "      \"service\": \"default\",\n" +
                        "      \"traffic_split\": null,\n" +
                        "      \"version\": null\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

    try {
      AppEngineUtil.parseDeployOutputToService(jsonOutput);
      fail();
    } catch (AssertionError e) {
      // Should throw an AssertionError.
    }
  }
}
