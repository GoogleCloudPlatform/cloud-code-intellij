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

package com.google.cloud.tools.intellij.appengine.validation;

import com.google.cloud.tools.intellij.appengine.util.EndpointUtilities;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for {@link EndpointUtilities}.
 */
public class EndpointUtilitiesTest extends TestCase {

  /**
   *  Tests {@link EndpointUtilities#removeBeginningAndEndingQuotes(String)} }
   */
  public void testRemoveBeginningAndEndingQuotes() {
    String a = "";
    String b = "abc";
    String c = "\"abc\"";
    String cResult = "abc";
    String d = "\"\"abc\"\"";
    String dResult = "\"abc\"";
    String e = "a\"a";
    String f =  "\"abc";
    String g = "abc\"";

    Assert.assertEquals(null, EndpointUtilities.removeBeginningAndEndingQuotes(null));
    Assert.assertEquals(a, EndpointUtilities.removeBeginningAndEndingQuotes(a));
    Assert.assertEquals(b, EndpointUtilities.removeBeginningAndEndingQuotes(b));
    Assert.assertEquals(cResult, EndpointUtilities.removeBeginningAndEndingQuotes(c));
    Assert.assertEquals(dResult, EndpointUtilities.removeBeginningAndEndingQuotes(d));
    Assert.assertEquals(e, EndpointUtilities.removeBeginningAndEndingQuotes(e));
    Assert.assertEquals(f, EndpointUtilities.removeBeginningAndEndingQuotes(f));
    Assert.assertEquals(g, EndpointUtilities.removeBeginningAndEndingQuotes(g));
  }

}
