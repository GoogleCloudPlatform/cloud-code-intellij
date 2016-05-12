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


import com.intellij.psi.PsiAnnotation;

/**
 * Exception when an attribute does not exist in an annotation.
 */
public class MissingAttributeException extends Exception {

  public MissingAttributeException(PsiAnnotation annotation, String attribute) {
    super("Attribute \"" + attribute + "\" does not exist in annotation \"" + annotation.getText()
        + "\"");
  }
}

