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

package com.google.cloud.tools.intellij.appengine.java.validation;

import com.google.cloud.tools.intellij.appengine.java.GctConstants;
import com.google.cloud.tools.intellij.appengine.java.util.PsiUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/** A visitor that has endpoint validation specific functionality. */
public class EndpointPsiElementVisitor extends JavaElementVisitor {

  // TODO: Add tests
  private static final String API_TRANSFORMER_ATTRIBUTE = "transformers";

  /**
   * Returns true if the class containing <code>psiElement</code> has a transformer specified by
   * using the @ApiTransformer annotation on a class or by using the transformer attribute of the
   *
   * @return True if the class containing <code>psiElement</code> has a transformer and false
   *     otherwise. @Api annotation. Returns false otherwise.
   */
  public boolean hasTransformer(PsiElement psiElement) {
    PsiClass psiClass = PsiUtils.findClass(psiElement);
    if (psiClass == null) {
      return false;
    }

    PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null) {
      return false;
    }

    // Check if class has @ApiTransformer to specify a transformer
    PsiAnnotation apiTransformerAnnotation =
        modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API_TRANSFORMER);
    if (apiTransformerAnnotation != null) {
      return true;
    }

    // Check if class utilizes the transformer attribute of the @Api annotation
    // to specify its transformer
    PsiAnnotation apiAnnotation =
        modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API);
    if (apiAnnotation != null) {
      PsiAnnotationMemberValue transformerMember =
          apiAnnotation.findAttributeValue(API_TRANSFORMER_ATTRIBUTE);
      if (transformerMember != null && !transformerMember.getText().equals("{}")) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the value for @Named if it exists for <code>psiParameter</code> or null if it does not
   * exist.
   *
   * @param psiParameter The parameter whose @Named value is to be returned.
   * @return The @Named value if it exists for <code>psiParameter</code> or null if it does not
   *     exist.
   */
  @Nullable
  public PsiAnnotationMemberValue getNamedAnnotationValue(PsiParameter psiParameter) {
    PsiModifierList modifierList = psiParameter.getModifierList();
    if (modifierList == null) {
      return null;
    }

    PsiAnnotation annotation = modifierList.findAnnotation("javax.inject.Named");
    if (annotation == null) {
      annotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NAMED);
      if (annotation == null) {
        return null;
      }
    }

    PsiNameValuePair[] nameValuePairs = annotation.getParameterList().getAttributes();
    if (nameValuePairs.length != 1) {
      return null;
    }

    if (nameValuePairs[0] == null) {
      return null;
    }

    return nameValuePairs[0].getValue();
  }

  /**
   * Returns set of supported parameter types.
   *
   * @param project The current project.
   * @return Set of parameter types.
   */
  private static Set<PsiClassType> createParameterTypes(Project project) {
    Set<PsiClassType> parameterTypes = new HashSet<PsiClassType>();
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Enum"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.String"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Boolean"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Integer"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Long"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Float"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Double"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Date"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("com.google.api.server.spi.types.DateAndTime"));
    parameterTypes.add(
        JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("com.google.api.server.spi.types.SimpleDate"));

    return Collections.unmodifiableSet(parameterTypes);
  }

  /**
   * Returns set of endpoint injected types.
   *
   * @param project The current project.
   * @return Set of injected types.
   */
  private static Set<PsiClassType> createInjectedClassTypes(Project project) {
    Set<PsiClassType> injectedClassTypes = new HashSet<PsiClassType>();
    injectedClassTypes.add(
        JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("com.google.appengine.api.users.User"));
    injectedClassTypes.add(
        JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("javax.servlet.http.HttpServletRequest"));
    injectedClassTypes.add(
        JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("javax.servlet.ServletContext"));

    return Collections.unmodifiableSet(injectedClassTypes);
  }

  /**
   * @return true if the raw or base type of <code>psiType</code> is one of the endpoint parameter
   *     types.
   */
  public boolean isApiParameter(PsiType psiType, Project project) {
    PsiType baseType = psiType;
    PsiClassType collectionType =
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");

    // If type is an array or collection, get the component type
    if (psiType instanceof PsiArrayType) {
      PsiArrayType arrayType = (PsiArrayType) psiType;
      baseType = arrayType.getDeepComponentType();
    } else if (collectionType.isAssignableFrom(psiType)) {
      assert (psiType instanceof PsiClassType);
      PsiClassType classType = (PsiClassType) psiType;
      PsiType[] parameters = classType.getParameters();
      if (parameters.length == 0) {
        return false;
      }
      baseType = parameters[0];
    }

    Set<PsiClassType> parameterTypes = createParameterTypes(project);
    for (PsiClassType classType : parameterTypes) {
      if (classType.isAssignableFrom(baseType)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the raw or base type of <code>psiParameter</code> is one of endpoint injected
   * type.
   */
  public boolean isInjectedParameter(PsiType psiType, Project project) {
    Set<PsiClassType> injectedClassTypes = createInjectedClassTypes(project);
    for (PsiClassType classType : injectedClassTypes) {
      if (classType.isAssignableFrom(psiType)) {
        return true;
      }
    }

    return false;
  }

  /**
   * @return true if the raw or base type of <code>psiType</code> is an entity(resource) type; i.e.
   *     not of parameter type nor of entity type
   */
  public boolean isEntityParameter(PsiType psiType, Project project) {
    boolean isApiParameter = isApiParameter(psiType, project);
    boolean isInjectedParameter = isInjectedParameter(psiType, project);
    if (!isApiParameter && !isInjectedParameter) {
      return true;
    } else {
      return false;
    }
  }
}
