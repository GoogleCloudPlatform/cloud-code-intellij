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

package com.google.gct.idea.appengine.validation;

import com.google.common.collect.Maps;
import com.google.gct.idea.appengine.GctConstants;
import com.google.gct.idea.appengine.util.EndpointBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Inspection to check that REST signatures in an Endpoint class are unique.
 */
public class RestSignatureInspection extends EndpointInspectionBase {
  public enum RestMethod {
    LIST("list", "GET"),
    GET("get", "GET"),
    INSERT("insert", "POST"),
    UPDATE("update", "PUT"),
    DELETE("delete", "DELETE"),
    REMOVE("remove", "DELETE"),
    DEFAULT("", "POST");

    protected final String methodNamePrefix;
    private final String httpMethod;

    /**
     * Specifies a default REST method prefix, as well as what HTTP method it should use by default.
     * @param methodNamePrefix A method name prefix.
     * @param httpMethod The default HTTP method for this prefix.
     */
    RestMethod(String methodNamePrefix, String httpMethod) {
      this.methodNamePrefix = methodNamePrefix;
      this.httpMethod = httpMethod;
    }

    /**
     * Gets the method name prefix for this instance.
     * @return The method name prefix.
     */
    public String getMethodNamePrefix() {
      return this.methodNamePrefix;
    }

    /**
     * Gets the default HTTP method for this instance.
     * @return The HTTP method.
     */
    public String getHttpMethod() {
      return this.httpMethod;
    }
  }

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("unique.rest.signature.description");
  }


  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("unique.rest.signature.name");
  }


  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("unique.rest.signature.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitClass(PsiClass aClass){
        if (!isEndpointClass(aClass)) {
          return;
        }

        PsiMethod[] allMethods = aClass.getMethods();
        Map<String, PsiMethod> restfulSignatures = Maps.newHashMap();

        for(PsiMethod aMethod : allMethods)  {
          validateRestSignatureUnique(aMethod, restfulSignatures );
        }
      }

      private void validateRestSignatureUnique(PsiMethod psiMethod, Map<String, PsiMethod> restfulSignatures) {
        // Check if method public or non-static
        if(!isApiMethod(psiMethod)) {
          return;
        }

        if(psiMethod.isConstructor()) {
          return;
        }

        String restSignature = getRestfulSignature(psiMethod);
        PsiMethod seenMethod = restfulSignatures.get(restSignature);
        if (seenMethod == null) {
          restfulSignatures.put(restSignature, psiMethod);
        } else {
          holder.registerProblem(psiMethod, getErrorMessage(restSignature, psiMethod.getName(), seenMethod.getName()),
                                 LocalQuickFix.EMPTY_ARRAY);
        }
      }
    };
  }

  /**
   * Returns the REST signature of the specified method. The REST signature is derived from
   * a combination of httpMethod and path.
   * @param psiMethod The method whose REST signature is to be determined
   * @return  The Rest Signature of psiMethod
   */
  public String getRestfulSignature(PsiMethod psiMethod)  {
    return getHttpMethod(psiMethod) + " " + getPath(psiMethod).replaceAll("\\{([^\\}]*)\\}", "\\{\\}");
  }

  /**
   * Returns the http method of the specified psiMethod. The httpMethod can be set by
   * the user by setting the httpMethod attribute in @ApiMethod. If the httpMethod attribute
   * of the @ApiMethod is not set, the default value of the method is used.
   * @param psiMethod The method hose HTTP method is to be determined.
   * @return The http Method pf psiMethod.
   */
  public String getHttpMethod(PsiMethod psiMethod) {
    PsiModifierList modifierList = psiMethod.getModifierList();
    String httpMethod = null;

    // Check if the httpMethod was specified by uses in @ApiMethod's httpMethod attribute
    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      try {
        httpMethod = getAttributeFromAnnotation(annotation, GctConstants.APP_ENGINE_ANNOTATION_API_METHOD, "httpMethod");
      } catch (InvalidAnnotationException e) {
        // do nothing
      } catch (MissingAttributeException e) {
        break;
      }

      if (httpMethod != null) {
        if (!httpMethod.isEmpty()) {
          return httpMethod;
        }
      }
    }

    // Create httpMethod from method name
    return getDefaultHttpMethod(psiMethod);
  }

  /**
   * Returns the path for psiMethod. The path can be set by the user by setting the path
   * attribute of @ApiMethod. If the path attribute is not set, a default value will be returned.
   * @param psiMethod The method whose path is to be determined.
   * @return  The path for psiMethod.
   */
  public String getPath(PsiMethod psiMethod)  {
    PsiModifierList modifierList = psiMethod.getModifierList();
    String path = null;

    // Check if the httpMethod was specified by user in @ApiMethod's httpMethod attribute
    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      try {
        path = getAttributeFromAnnotation(annotation, GctConstants.APP_ENGINE_ANNOTATION_API_METHOD, "path");
      } catch (InvalidAnnotationException e) {
        // do nothing
      } catch (MissingAttributeException e) {
        break;
      }

      if (path != null) {
        // path has a default value of ""
        if (!path.isEmpty()) {
          return path;
        } else {
          break;
        }
      }
    }

    // Determine default path
    return getDefaultPath(psiMethod);
  }

  /**
   * Returns the default path of psiMethod. The default path is determined in the following order
   * 1. If the resource attribute of @ApiClass is set, the default value is this attribute
   * 2. If the resource attribute of @Api is set, the default value is this attribute
   * 3. Uses the method's name as the resource name.
   * @param psiMethod The method whose default path is to be determined.
   * @return The default path for psiMethod
   */
  private String getDefaultPath(PsiMethod psiMethod) {
    // Get path from @ApiClass or @Api's resource attribute if it exists
    String apiDefaultResource = getResourceProperty(psiMethod);
    if(apiDefaultResource != null) {
      return apiDefaultResource.toLowerCase();
    }

    // Use the method name
    return psiMethod.getName();
  }

  /**
   * Returns a default HTTP method for <code>psiMethod</code>. The default HTTP method is
   * determined by parsing the method name. If the method name begins with any of the REST
   * method's prefixes, the HTTP method of the respective RESTMethod is returned.
   * If not, the HTTP method of the POST RestMethod is returned.
   * @param psiMethod The method whose default HTTP method is to be determined
   * @return
   */
  private String getDefaultHttpMethod(PsiMethod psiMethod) {
    String methodName = psiMethod.getName();
    for (RestMethod entry : RestMethod.values()) {
      if (methodName.startsWith(entry.getMethodNamePrefix())) {
        return entry.getHttpMethod();
      }
    }
    throw new AssertionError("It's impossible for method" + psiMethod.getName() + " to map to no REST path.");
  }

  @Nullable
  private String getResourceProperty(PsiMethod psiMethod) {
    PsiClass psiClass = psiMethod.getContainingClass();
    PsiModifierList modifierList = psiClass.getModifierList();
    String resource = null;

    // Get @ApiClass's resource attribute if it exists
    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      try{
        resource = getAttributeFromAnnotation(annotation, GctConstants.APP_ENGINE_ANNOTATION_API_CLASS, "resource");
      } catch (InvalidAnnotationException e) {
        // do nothing
      } catch (MissingAttributeException e) {
        break;
      }

      if(resource != null) {
        // resource attribute is "" by default
        if(!resource.isEmpty()) {
          return resource;
        }
      }
    }

    // Get @Api's resource attribute if it exists
    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      try {
        resource = getAttributeFromAnnotation(annotation, GctConstants.APP_ENGINE_ANNOTATION_API, "resource");
      } catch (InvalidAnnotationException e) {
        // do nothing;
      } catch (MissingAttributeException e) {
        break;
      }

      if (resource != null) {
        // resource attribute is "" by default
        if (resource.isEmpty()) {
          break;
        } else {
          return resource;
        }
      }
    }

    return null;
  }

  private String getAttributeFromAnnotation (PsiAnnotation annotation, String annotationType,
    final String attribute) throws InvalidAnnotationException, MissingAttributeException {

    if(annotation.getQualifiedName().equals(annotationType)) {
      PsiAnnotationMemberValue annotationMemberValue =  annotation.findAttributeValue(attribute);
      if(annotationMemberValue == null) {
        throw new MissingAttributeException(annotation, attribute);
      }

      String httpMethodWithQuotes = annotationMemberValue.getText();
      return httpMethodWithQuotes.substring(1,httpMethodWithQuotes.length()-1);
    } else {
      throw new InvalidAnnotationException(annotation, annotationType);
    }
  }

  private String getErrorMessage(String restSignature, String method1, String method2) {
    return String.format("Multiple methods with same rest path \"%s\": \"%s\" and \"%s\"",
                         restSignature, method1, method2);
  }

}
