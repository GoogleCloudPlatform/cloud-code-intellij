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
import com.google.api.server.spi.config.ApiMethod;

@Api(name="")
public class Foo {
  @ApiMethod(name = "boo")
  public void function1() {
    // do nothing
  }

  @ApiMethod(name = "boo")
  public void function2() {
    // do nothing
  }

  @ApiMethod(name = "")
  public void function3() {
    // do nothing
  }

  @ApiMethod
  public void function4() {
    // do nothing
  }
}