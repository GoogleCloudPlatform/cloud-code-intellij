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
package com.google.gct.login;


import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Holds the list of OAuth2 scopes for Google Login.
 */
class OAuthScopeRegistry {
  private static final SortedSet<String> sScopes;

  static {
    SortedSet<String> scopes = new TreeSet<String>();
    scopes.add("https://www.googleapis.com/auth/userinfo#email");
    scopes.add("https://www.googleapis.com/auth/appengine.admin");
    scopes.add("https://www.googleapis.com/auth/cloud-platform");

    sScopes = Collections.unmodifiableSortedSet(scopes);
  }

  @NotNull
  public static SortedSet<String> getScopes() {
    return sScopes;
  }
}
