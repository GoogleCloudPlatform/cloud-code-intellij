/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.application;

import com.google.api.client.http.HttpResponseException;

/**
 * Exception type for errors encountered when calling a Google API.
 */
public class GoogleApiException extends Exception {

  private final int statusCode;

  /**
   * Static builder to construct a {@code GoogleApiException} from a {@code HttpResponseException}
   * @param exception a HttpResponseException that was thrown by a Google API call
   */
  public static GoogleApiException from(HttpResponseException exception) {
    return new GoogleApiException(exception.getMessage(), exception, exception.getStatusCode());
  }

  public GoogleApiException(String message, Throwable cause, int statusCode) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public GoogleApiException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

}
