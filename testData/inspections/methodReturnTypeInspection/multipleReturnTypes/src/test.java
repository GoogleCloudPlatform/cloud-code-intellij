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
package com.example.app;

import com.google.api.server.spi.config.Api;
import com.google.appengine.api.users.User;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequestWrapper;


@Api
public class Foo {
  public void function1() {
    // do nothing
  }

  public String function2() {
    return "";
  }

  public HttpServletRequestWrapper function3() {
    return new HttpServletRequestWrapper(null);
  }

  public Set<String> function4() {
    return new HashSet<String>();
  }

}