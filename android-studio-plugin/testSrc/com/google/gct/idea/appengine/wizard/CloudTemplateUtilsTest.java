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
package com.google.gct.idea.appengine.wizard;

import com.google.common.collect.Maps;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Tests for cloud module templates.
 */
public class CloudTemplateUtilsTest extends TestCase {

  /** Ensure we are populating endpoints owner domain/package correctly */
  public void testOwnerDomainConversion() {
    Map<String, Object> myMap = Maps.newHashMap();
    assertEquals(myMap.size(),0);
    CloudModuleUtils.populateEndpointParameters(myMap, "com.test.package.reverse");

    assertEquals(myMap.size(), 2);
    assertEquals("reverse.package.test.com",myMap.get(CloudModuleUtils.ATTR_ENDPOINTS_OWNER));
    assertEquals("", myMap.get(CloudModuleUtils.ATTR_ENDPOINTS_PACKAGE));
  }
}
