/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.gradle.facet;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.AbstractCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds configuration property values for the AppEngine facet.
 */
public class AppEngineConfigurationProperties {
  public String HTTP_ADDRESS;
  public Integer HTTP_PORT;
  @AbstractCollection(surroundWithTag = false, elementTag = "jvmflags", elementValueAttribute = "")
  public List<String> JVM_FLAGS = new ArrayList<String>();
  public String WAR_DIR;
  public String WEB_APP_DIR;
  public String APPENGINE_SDKROOT;
  public boolean DISABLE_UPDATE_CHECK;

  public String getJvmFlags() {
    return StringUtil.join(JVM_FLAGS, " ");
  }
}
