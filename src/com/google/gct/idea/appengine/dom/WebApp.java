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
package com.google.gct.idea.appengine.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;

import java.util.List;

/**
 * Represents the web.xml file in an AppEngine module.
 */
public interface WebApp extends DomElement {

  List<Servlet> getServlets();

  List<ServletMapping> getServletMappings();

  List<Filter> getFilters();

  Filter addFilter();

  List<FilterMapping> getFilterMappings();

  FilterMapping addFilterMapping();

  interface Servlet extends DomElement {

    ServletName getServletName();

    ServletClass getServletClass();

    List<InitParam> getInitParams();

    interface ServletName extends GenericDomValue<String> {
    }

    interface ServletClass extends GenericDomValue<String> {
    }

    interface InitParam extends DomElement {

      ParamName getParamName();

      ParamValue getParamValue();

      interface ParamName extends GenericDomValue<String> {
      }

      interface ParamValue extends GenericDomValue<String> {
      }
    }
  }

  interface ServletMapping extends DomElement {

    ServletName getServletName();

    UrlPattern getUrlPattern();

    interface ServletName extends GenericDomValue<String> {
    }

    interface UrlPattern extends GenericDomValue<String> {
    }
  }

  interface Filter extends DomElement {
    FilterName getFilterName();

    FilterClass getFilterClass();

    interface FilterName extends GenericDomValue<String> {
    }

    interface FilterClass extends GenericDomValue<String> {
    }
  }

  interface FilterMapping extends DomElement {
    FilterName getFilterName();

    UrlPattern getUrlPattern();

    interface FilterName extends GenericDomValue<String> {
    }

    interface UrlPattern extends GenericDomValue<String> {
    }
  }
}
