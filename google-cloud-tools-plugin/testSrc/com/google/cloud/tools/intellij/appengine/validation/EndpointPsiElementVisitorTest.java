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

import com.google.cloud.tools.intellij.appengine.GctConstants;

import com.intellij.psi.*;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EndpointPsiElementVisitor}
 */
public class EndpointPsiElementVisitorTest extends TestCase {
  private PsiMethod mockPsiMethod;
  private PsiClass mockPsiClass;
  private PsiParameter mockPsiParameter;
  private static final String API_TRANSFORMER_ATTRIBUTE = "transformers";

  private void initializePsiClass(boolean hasApiTransformerApi, boolean hasTransformerAttribute) {
    PsiAnnotation mockTransformerAnnotation = null;
    PsiAnnotation mockApiAnnotation = null;

    if(hasApiTransformerApi) {
      // Mock @ApiTransformer(MyTransformer.class))
      mockTransformerAnnotation = mock(PsiAnnotation.class);
      when(mockTransformerAnnotation.getQualifiedName()).thenReturn(GctConstants.APP_ENGINE_ANNOTATION_API_TRANSFORMER);
    }

    if(hasTransformerAttribute) {
      PsiAnnotationMemberValue mockAnnotationMemberValue = mock(PsiAnnotationMemberValue.class);
      when(mockAnnotationMemberValue.getText()).thenReturn("MyTransformer.class");

      // Mock @Api(transformer = MyTransformer.class)
      mockApiAnnotation = mock(PsiAnnotation.class);
      when(mockApiAnnotation.getQualifiedName()).thenReturn(GctConstants.APP_ENGINE_ANNOTATION_API);
      when(mockApiAnnotation.findAttributeValue(API_TRANSFORMER_ATTRIBUTE)).thenReturn(mockAnnotationMemberValue);
    }

    PsiModifierList mockModifierList = mock(PsiModifierList.class);
    when(mockModifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API_TRANSFORMER))
      .thenReturn(mockTransformerAnnotation);
    when(mockModifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API))
      .thenReturn(mockApiAnnotation);

    mockPsiClass = mock(PsiClass.class);
    when(mockPsiClass.getModifierList()).thenReturn(mockModifierList);
  }

  /**
   * Tests that {@Link EndpointPsiElementVisitor#hasTransformer} returns true
   * for a PsiClass that uses the @ApiTransformer to specify its transformer.
   */
  public void testHasTransformer_ClassWithTransformerAnnotation() {
    initializePsiClass(true, false);
    MockitoAnnotations.initMocks(this);

    EndpointPsiElementVisitor endpointPsiElementVisitor = new EndpointPsiElementVisitor();
    Assert.assertTrue(endpointPsiElementVisitor.hasTransformer(mockPsiClass));
  }

  /**
   * Tests that {@Link EndpointPsiElementVisitor#hasTransformer} returns true
   * for a PsiClass that uses the transformers attribute on @Api to specify its transformer.
   */
  public void testHasTransformer_ClassWithTransformerAttribute() {
    initializePsiClass(false, true);
    MockitoAnnotations.initMocks(this);

    EndpointPsiElementVisitor endpointPsiElementVisitor = new EndpointPsiElementVisitor();
    Assert.assertTrue(endpointPsiElementVisitor.hasTransformer(mockPsiClass));
  }

  /**
   * Tests that {@Link EndpointPsiElementVisitor#hasTransformer} returns false
   * for a PsiClass that doe not specify a transformer.
   */
  public void testHasTransformer_ClassWithoutTransformer() {
    initializePsiClass(false, false);
    MockitoAnnotations.initMocks(this);

    EndpointPsiElementVisitor endpointPsiElementVisitor = new EndpointPsiElementVisitor();
    Assert.assertFalse(endpointPsiElementVisitor.hasTransformer(mockPsiClass));
  }

  /**
   * Tests that {@Link EndpointPsiElementVisitor#hasTransformer} returns true
   * for a PsiMethod whose containing class uses the @ApiTransformer to specify its transformer.
   */
  public void testHasTransformer_MethodWithTransformer() {
    initializePsiClass(true, false);
    mockPsiMethod = mock(PsiMethod.class);
    when(mockPsiMethod.getParent()).thenReturn(mockPsiClass);
    MockitoAnnotations.initMocks(this);

    EndpointPsiElementVisitor endpointPsiElementVisitor = new EndpointPsiElementVisitor();
    Assert.assertTrue(endpointPsiElementVisitor.hasTransformer(mockPsiMethod));
  }

  /**
   * Tests that {@Link EndpointPsiElementVisitor#hasTransformer} returns false
   * for a PsiParameter whose containing class does not specify a transformer.
   */
  public void testHasTransformer_ParameterWithoutTransformer() {
    initializePsiClass(false, false);
    mockPsiMethod = mock(PsiMethod.class);
    when(mockPsiMethod.getParent()).thenReturn(mockPsiClass);
    mockPsiParameter = mock(PsiParameter.class);
    when(mockPsiParameter.getParent()).thenReturn(mockPsiMethod);
    MockitoAnnotations.initMocks(this);

    EndpointPsiElementVisitor endpointPsiElementVisitor = new EndpointPsiElementVisitor();
    Assert.assertFalse(endpointPsiElementVisitor.hasTransformer(mockPsiParameter));
  }

}
