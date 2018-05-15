/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.gradle;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.module.Module;

/** Service for working with the App Engine Gradle facet */
public class AppEngineGradleFacetService {

  private static final AppEngineGradleFacetService INSTANCE = new AppEngineGradleFacetService();

  static AppEngineGradleFacetService getInstance() {
    return INSTANCE;
  }

  /**
   * Adds the App Engine Gradle facet to the module. Adds the data in {@link AppEngineGradleModule}
   * to the facet.
   */
  void addFacet(
      AppEngineGradleModule appEngineGradleModule,
      Module module,
      ModifiableFacetModel modifiableFacetModel) {
    // TODO (eshaul) ignore for now in code review; will comment in facet logic in next PR
    //    AppEngineGradleFacet facet = AppEngineGradleFacet.getInstance(module);
    //
    //    if (facet == null) {
    //      facet =
    //          FacetManager.getInstance(module)
    //              .createFacet(
    //                  AppEngineGradleFacet.getFacetType(),
    //                  AppEngineGradleFacetType.NAME,
    //                  null /*underlying*/);
    //
    //      modifiableFacetModel.addFacet(facet);
    //    }
    //
    //
    // facet.getConfiguration().setGradleBuildDir(appEngineGradleModule.getModel().gradleBuildDir());
  }
}
