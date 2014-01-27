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
package com.google.gct.intellij.endpoints.validation;

import com.google.gct.intellij.endpoints.GctConstants;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.psi.*;
import junit.framework.Assert;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestSignatureInspectionTest extends EndpointTestBase {
  private PsiMethod mockPsiMethod;
  private PsiClass mockPsiClass;

  private void initializePsiMethod(String methodName, String httpMethodValue, String pathValue) {
    PsiAnnotationMemberValue mockAnnotationMemberValue1 = mock(PsiAnnotationMemberValue.class);
    when(mockAnnotationMemberValue1.getText()).thenReturn(httpMethodValue);

    PsiAnnotationMemberValue mockAnnotationMemberValue2 = mock(PsiAnnotationMemberValue.class);
    when(mockAnnotationMemberValue2.getText()).thenReturn(pathValue);

    PsiAnnotation mockAnnotation = mock(PsiAnnotation.class);
    when(mockAnnotation.getQualifiedName()).thenReturn(GctConstants.APP_ENGINE_ANNOTATION_API_METHOD);
    when(mockAnnotation.findAttributeValue("httpMethod")).thenReturn(mockAnnotationMemberValue1);
    when(mockAnnotation.findAttributeValue("path")).thenReturn(mockAnnotationMemberValue2);
    PsiAnnotation[] mockAnnotationsArray = {mockAnnotation};

    PsiModifierList mockModifierList = mock(PsiModifierList.class);
    when(mockModifierList.getAnnotations()).thenReturn(mockAnnotationsArray);

    mockPsiMethod = mock(PsiMethod.class);
    when(mockPsiMethod.getModifierList()).thenReturn(mockModifierList);
    when(mockPsiMethod.getName()).thenReturn(methodName);
    when(mockPsiMethod.getContainingClass()).thenReturn(mockPsiClass);
  }

  private void initializePsiClass(String apiResource, String apiClassResource) {
    PsiAnnotationMemberValue mockAnnotationMemberValue1 = mock(PsiAnnotationMemberValue.class);
    when(mockAnnotationMemberValue1.getText()).thenReturn(apiResource);

    PsiAnnotationMemberValue mockAnnotationMemberValue2 = mock(PsiAnnotationMemberValue.class);
    when(mockAnnotationMemberValue2.getText()).thenReturn(apiClassResource);

    // Mock @Api(resource = "")
    PsiAnnotation mockAnnotation1 = mock(PsiAnnotation.class);
    when(mockAnnotation1.getQualifiedName()).thenReturn(GctConstants.APP_ENGINE_ANNOTATION_API);
    when(mockAnnotation1.findAttributeValue("resource")).thenReturn(mockAnnotationMemberValue1);

    // Mock @ApiClass(resource = "")
    PsiAnnotation mockAnnotation2 = mock(PsiAnnotation.class);
    when(mockAnnotation2.getQualifiedName()).thenReturn(GctConstants.APP_ENGINE_ANNOTATION_API_CLASS);
    when(mockAnnotation2.findAttributeValue("resource")).thenReturn(mockAnnotationMemberValue2);

    PsiAnnotation[] mockAnnotationsArray = {mockAnnotation1, mockAnnotation2};

    PsiModifierList mockModifierList = mock(PsiModifierList.class);
    when(mockModifierList.getAnnotations()).thenReturn(mockAnnotationsArray);

    mockPsiClass = mock(PsiClass.class);
    when(mockPsiClass.getModifierList()).thenReturn(mockModifierList);
  }

  public void testGetRestfulSignature() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("methodName", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String restfulSignature = inspection.getRestfulSignature(mockPsiMethod);
    Assert.assertEquals("POST methodName", restfulSignature);
  }

  public void testGetHttpMethod_SpecifiedHttpMethod() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("methodName", "\"post\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String httpMethod = inspection.getHttpMethod(mockPsiMethod);
    Assert.assertEquals("post", httpMethod);
  }

  /**
   * Test getHttpMethod when the method name starts with a REST prefix.
   */
  public void testGetHttpMethod_NameWithRestPrefix() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("updateFoo", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String httpMethod = inspection.getHttpMethod(mockPsiMethod);
    Assert.assertEquals("PUT", httpMethod);
  }

  /**
   * Test getHttpMethod when the method name does not start with a REST prefix.
   */
  public void testGetHttpMethod_NameWithoutRestPrefix() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("foo", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String httpMethod = inspection.getHttpMethod(mockPsiMethod);
    Assert.assertEquals("POST", httpMethod);
  }

  public void testGetHttpMethod_WithHttpMethodAndValidPrefix() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("deleteFoo", "\"list\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String httpMethod = inspection.getHttpMethod(mockPsiMethod);
    Assert.assertEquals("list", httpMethod);
  }

  public void testGetPath_SpecifiedPath() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("\"\"", "\"\"", "\"abc\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("abc", path);
  }

  public void testGetPath_SpecifiedApiClassResource() {
    initializePsiClass("\"\"", "\"abc\"");
    initializePsiMethod("\"\"", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("abc", path);
  }

  public void testGetPath_SpecifiedApiResource() {
    initializePsiClass("\"abc\"", "\"\"");
    initializePsiMethod("\"\"", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("abc", path);
  }

  public void testGetPath_NoAttributeSpecified() {
    initializePsiClass("\"\"", "\"\"");
    initializePsiMethod("foo", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("foo", path);
  }

  public void testGetPath_PathAndApiClassResourceSet() {
    initializePsiClass("\"\"", "\"res\"");
    initializePsiMethod("foo", "\"\"", "\"boo\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("boo", path);
  }

  public void testGetPath_ApiAndApiClassResourceSet() {
    initializePsiClass("\"res1\"", "\"res2\"");
    initializePsiMethod("foo", "\"\"", "\"\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("res2", path);
  }

  /**
   * Testing getPath() with the @ApiMethod's path attribute and the @Api's
   * resource attribute set.
   */
  public void testGetPath_PathAndApiResourceSet() {
    initializePsiClass("\"res\"", "\"\"");
    initializePsiMethod("foo", "\"\"", "\"boo\"");
    MockitoAnnotations.initMocks(this);

    RestSignatureInspection inspection = new RestSignatureInspection();
    String path = inspection.getPath(mockPsiMethod);
    Assert.assertEquals("boo", path);
  }

  public void testNonUniqueRestSignatures() {
    doTest();
  }

  public void testNonUniqueSigWithPathNameSet() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new RestSignatureInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.testInspection("inspections/restSignatureInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }

}
