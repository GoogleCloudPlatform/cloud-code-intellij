/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.google.gct.intellij.endpoints;

import com.android.SdkConstants;

/** A list of constants used by the CloudTools Plugin */
public final class GctConstants {
  public static final String APP_ENGINE_SRC_DIR = "src";
  public static final String APP_ENGINE_MAIN_DIR = "main";
  public static final String APP_ENGINE_JAVA_DIR = "java";
  public static final String APP_ENGINE_RES_DIR = "resources";
  public static final String APP_ENGINE_WEBAPP_DIR = "webapp";
  public static final String APP_ENGINE_META_INF_DIR = "META-INF";
  public static final String APP_ENGINE_META_INF_PATH = APP_ENGINE_SRC_DIR + "/" + APP_ENGINE_MAIN_DIR + "/" +
                                                        APP_ENGINE_RES_DIR + "/" + APP_ENGINE_META_INF_DIR;
  public static final String APP_ENGINE_WEB_INF_DIR = "WEB-INF";
  public static final String APP_ENGINE_JS_DIR = "js";
  public static final String APP_ENGINE_CSS_DIR = "css";
  public static final String APP_ENGINE_IMG_DIR = "img";
  public static final String APP_ENGINE_MODULE_SUFFIX = "-AppEngine";
  public static final String APP_ENGINE_GENERATED_LIB_DIR = "google_generated";
  public static final String APP_ENGINE_ANNOTATION_API = "com.google.api.server.spi.config.Api";
  public static final String APP_ENGINE_ANNOTATION_API_CLASS = "com.google.api.server.spi.config.ApiClass";
  public static final String APP_ENGINE_ANNOTATION_API_METHOD = "com.google.api.server.spi.config.ApiMethod";
  public static final String APP_ENGINE_ANNOTATION_API_NAMESPACE = "com.google.api.server.spi.config.ApiNamespace";
  public static final String APP_ENGINE_ANNOTATION_API_REFERENCE = "com.google.api.server.spi.config.ApiReference";
  public static final String APP_ENGINE_ANNOTATION_API_RESOURCE_PROPERTY = "com.google.api.server.spi.config.ApiResourceProperty";
  public static final String APP_ENGINE_ANNOTATION_API_TRANSFORMER = "com.google.api.server.spi.config.ApiTransformer";
  public static final String APP_ENGINE_ANNOTATION_DEFAULT_VALUE = "com.google.api.server.spi.config.DefaultValue";
  public static final String APP_ENGINE_ANNOTATION_ENTITY = "javax.persistence.Entity";
  public static final String APP_ENGINE_ANNOTATION_ID = "javax.persistence.Id";
  public static final String APP_ENGINE_ANNOTATION_NAMED = "com.google.api.server.spi.config.Named";
  public static final String APP_ENGINE_ANNOTATION_NULLABLE = "com.google.api.server.spi.config.Nullable";
  public static final String APP_ENGINE_TYPE_KEY = "com.google.appengine.api.datastore.Key";


  public static final String ANDROID_GCM_LIB_MODULE_SUFFIX = "-endpoints";
  public static final String ANDROID_ENDPOINT_SRC_PATH = SdkConstants.SRC_FOLDER + "/endpoint-src/java";
  public static final String ANDROID_SDK_GCM_PATH = "/extras/google/gcm/gcm-client/dist/gcm.jar";
  public static final String ANDROID_SDK_GCM_SERVER_PATH = "/extras/google/gcm/gcm-server/dist/gcm-server.jar";

  public static final String API_CLIENT_GENERATED_SOURCES = "generated-sources";
  public static final String API_CLIENT_SOURCES_JAR_SUFFIX = "-sources.jar";
  public static final String API_CLIENT_EXPANDED_SOURCE_SUBFOLDER_NAME = "lib-expanded-source";
  public static final String API_CLIENT_LIBS_SUBFOLDER_NAME = "libs";
  public static final String API_CLIENT_PROGUARD_FILENAME = "proguard-google-api-client.txt";

}
