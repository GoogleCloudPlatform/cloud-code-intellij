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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.gson.JsonParseException;

import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.junit.Test;
import org.mockito.Mock;

/**
 * Test case for {@link AppEngineDeploymentRuntimeImpl}.
 */

public class AppEngineDeploymentRuntimeImplTest extends BasePluginTestCase {

  @Mock private AppEngineHelper appEngineHelper;
  @Mock private LoggingHandler loggingHandler;

  @Test
  public void testDeployOutputJsonParsingOneVersion() {
    String jsonOutput =
        "{\n" +
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

    AppEngineDeploymentRuntimeImpl.DeployOutput deployOutput =
        AppEngineDeploymentRuntimeImpl.parseDeployOutputToService(jsonOutput);
    assertEquals(deployOutput.getVersion(), "20160429t112518");
    assertEquals(deployOutput.getService(), "default");
  }

  @Test
  public void testDeployOutputJsonParsingTwoVersions() {
    String jsonOutput =
        "{\n" +
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
      AppEngineDeploymentRuntimeImpl.parseDeployOutputToService(jsonOutput);
      fail();
    } catch (JsonParseException e) {
      // Success! Should throw a JsonParseException.
    }
  }

  @Test
  public void testDeployOutputJsonParsingOldFormat() {
    String jsonOutput =
        "{\n" +
        "  \"default\": \"https://springboot-maven-project.appspot.com\"\n" +
        "}\n";

    try {
      AppEngineDeploymentRuntimeImpl.parseDeployOutputToService(jsonOutput);
      fail();
    } catch (JsonParseException e) {
      // Success! Should throw a JsonParseException.
    }
  }
}
