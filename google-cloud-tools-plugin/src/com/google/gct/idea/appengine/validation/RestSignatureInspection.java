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
import com.google.gct.idea.appengine.util.EndpointUtilities;
import com.google.gct.idea.appengine.util.PsiUtils;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Inspection to check that REST signatures in an Endpoint class are unique.
 */
public class RestSignatureInspection extends EndpointInspectionBase {
  public enum RestMethod {
    LIST("list", "GET") {
      /**
       * If the return type of {@code method} is a parameterized collection,
       * this function constructs the resource name from the parameterized parameters
       * of the return type else it returns {@code null}.
       *
       * @param method the method in which the resource name will be generated from
       * @return the guessed resource name
       */
      @Override
      @Nullable
      public String guessResourceName(PsiMethod method) {
        Project project = getProject(method);
        if (project == null) {
          return null;
        }

        PsiType returnType = method.getReturnType();
        if(isValidCollectionType(project, returnType)) {
          assert(returnType instanceof PsiClassType);
          PsiClassType classType = (PsiClassType) returnType;
          PsiType[] typeParams = classType.getParameters();

          // TODO: Add inspection to verify that the the type parameter is specified
          // for paramerterized types, since trying to generate client libs without one generates
          // a : "Object type T not supported"
          return typeParams.length > 0 ? getSimpleName(project, typeParams[0]).toLowerCase() : null;
        }
        return null;
      }

      private boolean isValidCollectionType (Project project, PsiType type) {
        // Check if type is a Collection
        if(PsiUtils.isParameterizedType(type)) {
          PsiClassType collectionType =
            JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");
          PsiClassType collectionResponseType = JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("com.google.api.server.spi.response.CollectionResponse");

          return collectionType.isAssignableFrom(type) || collectionResponseType.isAssignableFrom(type);
        }
        return false;
      }
    },
    GET("get", "GET"),
    INSERT("insert", "POST"),
    UPDATE("update", "PUT"),
    DELETE("delete", "DELETE") {
      /**
       * Returns {@code null} if the length of the name of {@code method} is
       * shorter or equal to the current RestMethod's prefix. Otherwise returns the substring
       * of the name of {@code method} beginning at the length of the current RestMethod's
       * prefix.
       */
      @Override
      public String guessResourceName(PsiMethod method) {
        String methodName = method.getName();
        return methodNamePrefix.length() >= methodName.length() ? null :
               methodName.substring(methodNamePrefix.length()).toLowerCase();
      }
    },
    REMOVE("remove", "DELETE") {
      /**
       * Returns {@code null} if the length of the name of {@code method} is
       * shorter or equal to the current RestMethod's prefix. Otherwise returns the substring
       * of the name of {@code method} beginning at the length of the current RestMethod's
       * prefix.
       */
      @Override
      public String guessResourceName(PsiMethod method) {
        String methodName = method.getName();
        return methodNamePrefix.length() >= methodName.length() ? null :
               methodName.substring(methodNamePrefix.length()).toLowerCase();
      }
    },
    DEFAULT("", "POST"){
      @Override
      @Nullable
      public String guessResourceName(PsiMethod method) {
        return null;
      }
    };

    protected final String methodNamePrefix;
    private final String httpMethod;

    /**
     * Specifies a default REST method prefix, as well as what HTTP method it should use by default.
     *
     * @param methodNamePrefix a method name prefix
     * @param httpMethod the default HTTP method for this prefix
     */
    RestMethod(String methodNamePrefix, String httpMethod) {
      this.methodNamePrefix = methodNamePrefix;
      this.httpMethod = httpMethod;
    }

    /**
     * Gets the method name prefix for this instance.
     *
     * @return the method name prefix
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

    /**
     * Generates a resource name from either {@code method}'s name or its return type.
     *
     * @param method the method in which the resource name will be generated from
     * @return the guessed resource name
     */
    @Nullable
    public String guessResourceName(PsiMethod method) {
      Project project = getProject(method);
      if (project == null) {
        return null;
      }

      String simpleName = getSimpleName(project, method.getReturnType());
      return simpleName == null ? null : simpleName.toLowerCase();
    }

    @Nullable
    private static String getSimpleName(Project project, PsiType type) {
      PsiClassType collectionType =
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");

      if (type == null) {
        return null;
      } else if (type instanceof PsiArrayType) {
        PsiType arrayComponentType = ((PsiArrayType) type).getComponentType();
        return getSimpleName(project, arrayComponentType) + "collection";

      } else if (collectionType.isAssignableFrom(type)) {
        assert (type instanceof PsiClassType);
        PsiClassType classType = (PsiClassType) type;
        PsiType[] typeParams = classType.getParameters();
        return typeParams.length > 0 ? getSimpleName(project, typeParams[0]) + "collection" : null;
      } else if (PsiUtils.isParameterizedType(type)) {
        assert (type instanceof PsiClassType);
        StringBuilder builder = new StringBuilder();
        PsiClassType classType = (PsiClassType) type;
        builder.append(getSimpleName(project, classType.rawType()));

        PsiType[] typeParams = classType.getParameters();
        for(PsiType aType : typeParams) {
          builder.append('_');
          builder.append(getSimpleName(project, aType));
        }
        return builder.toString();

      } else {
        return type.getPresentableText();
      }
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
        if (!EndpointUtilities.isEndpointClass(aClass)) {
          return;
        }

        if(hasTransformer(aClass)) {
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
        if(!EndpointUtilities.isApiMethod(psiMethod)) {
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
   *
   * @param psiMethod the method whose REST signature is to be determined
   * @return  the Rest Signature of psiMethod
   */
  public String getRestfulSignature(PsiMethod psiMethod) {
    return getHttpMethod(psiMethod) + " " + getPath(psiMethod).replaceAll("\\{([^\\}]*)\\}", "\\{\\}");
  }

  /**
   * Returns the http method of the specified psiMethod. The httpMethod can be set by
   * the user by setting the httpMethod attribute in @ApiMethod. If the httpMethod attribute
   * of the @ApiMethod is not set, the default value of the method is used.
   *
   * @param psiMethod the method hose HTTP method is to be determined
   * @return the http Method pf psiMethod
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
    return getDefaultRestMethod(psiMethod).getHttpMethod();
  }

  /**
   * Returns the path for psiMethod. The path can be set by the user by setting the path
   * attribute of @ApiMethod. If the path attribute is not set, a default value will be returned.
   *
   * @param psiMethod the method whose path is to be determined
   * @return the path for psiMethod
   */
  public String getPath(PsiMethod psiMethod) {
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
   * 1. If the resource attribute of @ApiClass is set, the default value is this attribute + the path parameters
   * 2. If the resource attribute of @Api is set, the default value is this attribute + the path parameters
   * 3. If the method return type is not void, we guess the resource value in
   *    {@link RestSignatureInspection#guessResourceName(com.intellij.psi.PsiMethod)} and add the path parameters
   * 4. Else use the method's name as the resource name + the path parameters.
   *
   * @param psiMethod the method whose default path is to be determined
   * @return the default path for psiMethod
   */
  private String getDefaultPath(PsiMethod psiMethod) {
    // Get path from @ApiClass or @Api's resource attribute if it exists
    String apiDefaultResource = getResourceProperty(psiMethod);
    if(apiDefaultResource != null) {
      return apiDefaultResource.toLowerCase() + getPathParameter(psiMethod);
    }

    // If the method return type is not void, use guessed resource type name
    String guessedResourceName = guessResourceName(psiMethod);
    if(guessedResourceName != null) {
      return guessedResourceName + getPathParameter(psiMethod);
    }

    // Use the method name
    return psiMethod.getName() + getPathParameter(psiMethod);
  }

  /**
   * Returns the default REST method for {@code psiMethod}. The default REST method is
   * determined by parsing the method name. If the method name begins with any of the REST
   * method's prefixes, the REST method of the respective RESTMethod is returned.
   * If not, the POST RestMethod is returned.
   *
   * @param psiMethod the method whose default HTTP method is to be determined
   */
  private RestMethod getDefaultRestMethod(PsiMethod psiMethod) {
    String methodName = psiMethod.getName();
    for (RestMethod entry : RestMethod.values()) {
      if (methodName.startsWith(entry.getMethodNamePrefix())) {
        return entry;
      }
    }
    throw new AssertionError("It's impossible for method" + psiMethod.getName() + " to map to no REST path.");
  }

  @Nullable
  private String getResourceProperty(PsiMethod psiMethod) {
    PsiClass psiClass = psiMethod.getContainingClass();
    PsiModifierList modifierList = psiClass.getModifierList();
    String resource = null;

    if(modifierList == null) {
      return null;
    }

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

    String annotationQualifiedName = annotation.getQualifiedName();
    if (annotationQualifiedName == null) {
      throw new InvalidAnnotationException(annotation, annotationType);
    }

    if(annotationQualifiedName.equals(annotationType)) {
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

  /**
   * Guesses a resource name based off the method name or return type.
   */
  @Nullable
  private String guessResourceName(PsiMethod method) {
    // Check if return type is void
    if(method.getReturnType() == PsiType.VOID) {
      return null;
    }

    // Determine a RestMethod based off the method name
    RestMethod restMethod = getDefaultRestMethod(method);

    // Guess the resource name using the RestMethod's guessResourceName function
    return restMethod.guessResourceName(method);
  }

  /**
   * Returns "/{}" for every parameter with a valid @Named annotation in {@code method}
   * that does not have @Nullable/@Default.
   */
  private String getPathParameter(PsiMethod method) {
    StringBuilder path = new StringBuilder();
    EndpointPsiElementVisitor elementVisitor = new EndpointPsiElementVisitor();
    List<String> annotions =
      Arrays.asList(GctConstants.APP_ENGINE_ANNOTATION_NULLABLE, "javax.annotation.Nullable", GctConstants.APP_ENGINE_ANNOTATION_DEFAULT_VALUE);

    for(PsiParameter aParameter : method.getParameterList().getParameters()) {
      // Check for @Nullable/@Default
      PsiModifierList modifierList = aParameter.getModifierList();
      if(modifierList == null) {
        continue;
      }

      if (AnnotationUtil.isAnnotated(aParameter, annotions)) {
        continue;
      }

      PsiAnnotationMemberValue namedValue = elementVisitor.getNamedAnnotationValue(aParameter);
      if(namedValue != null) {
        path.append("/{}");
      }
    }

    return path.toString();
  }

  /**
   * Returns the project associated with {@code method} or null if it cannot retrieve the project.
   */
  @Nullable
  private static Project getProject(PsiMethod method) {
    Project project;
    try {
      project = method.getContainingFile().getProject();
    } catch (PsiInvalidElementAccessException e) {
      LOG.error("Error getting project with parameter " + method.getText(), e);
      return null;
    }
    return project;
  }
}
