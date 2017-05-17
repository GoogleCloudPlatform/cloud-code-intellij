/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.debugger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import org.junit.Ignore;

/** Unit tests for {@link ServerToIdeFileResolver}. */
public class ServerToIDEFileResolverTest extends JavaCodeInsightFixtureTestCase {
  private PsiClass class1;
  private PsiClass class2;
  private PsiFile file1;
  private PsiFile file2;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    class1 = this.myFixture.addClass("package com.java.pkg; class Class {}");
    class2 = this.myFixture.addClass("package com.java.pkg; class ClassTest {}");
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetCloudPathFromJavaFile() {
    PsiJavaFile psiJavaFile = mock(PsiJavaFile.class);
    when(psiJavaFile.getPackageName()).thenReturn("com.java.package");
    when(psiJavaFile.getName()).thenReturn("Class.java");
    assertEquals(
        ServerToIdeFileResolver.getCloudPathFromJavaFile(psiJavaFile),
        "com/java/package/Class.java");
  }

  // When searching for full file system path.
  @Ignore
  public void ignore_testGetFileFromPath_fullPath() {
    // TODO(joaomartins): Find out why project.getBaseDir() is returning a different tempDir to
    // myFixture.
    file1 = this.myFixture.addFileToProject("path/to/prj/src/main/com/java/package/Class.java", "");
    file2 =
        this.myFixture.addFileToProject("path/to/prj/src/test/com/java/package/ClassTest.java", "");

    ServerToIdeFileResolver fileResolver = new ServerToIdeFileResolver();
    assertEquals(
        fileResolver.getFileFromPath(getProject(), "path/to/prj/src/main/com/java/package/Class.java"),
        file1.getVirtualFile());
  }

  // When searching for the package and class name.
  public void testGetFileFromPath_packageClass() {
    ServerToIdeFileResolver fileResolver = new ServerToIdeFileResolver();

    assertEquals(
        class1.getContainingFile().getVirtualFile(),
        fileResolver.getFileFromPath(getProject(), "com/java/pkg/Class.java"));
  }

  // When searching for file name only.
  public void testGetFileFromPath_fileName() {
    ServerToIdeFileResolver fileResolver = new ServerToIdeFileResolver();

    assertEquals(
        class1.getContainingFile().getVirtualFile(),
        fileResolver.getFileFromPath(getProject(), "Class.java"));
  }

  public void testGetPackageFromPath() {
    assertEquals(
        "com.java.package",
        ServerToIdeFileResolver.getPackageFromPath("/com/java/package/Class.java"));
    assertEquals("", ServerToIdeFileResolver.getPackageFromPath("Class.java"));
    assertEquals("package", ServerToIdeFileResolver.getPackageFromPath("/package/Class.java"));
    assertEquals(
        "com.java.package",
        ServerToIdeFileResolver.getPackageFromPath("/com//java/package//Class.java"));
    assertEquals(
        "com.java.package",
        ServerToIdeFileResolver.getPackageFromPath("com/java/package/Class.java"));
    assertEquals("", ServerToIdeFileResolver.getPackageFromPath(""));
  }

  public void testGetClassNameFromPath() {
    assertEquals(
        "Class", ServerToIdeFileResolver.getClassNameFromPath("com/java/package/Class.java"));
    assertEquals("Class", ServerToIdeFileResolver.getClassNameFromPath("com/java/package/Class"));
    assertEquals("", ServerToIdeFileResolver.getClassNameFromPath(""));
    assertEquals("Class", ServerToIdeFileResolver.getClassNameFromPath("Class"));
    assertEquals(
        "Class", ServerToIdeFileResolver.getClassNameFromPath("com.java/package/Class.java"));
    assertEquals("Class", ServerToIdeFileResolver.getClassNameFromPath("com.java/package/Class"));
  }
}
