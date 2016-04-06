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

package com.google.cloud.tools.intellij.debugger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for {@link ServerToIDEFileResolver}.
 */
public class ServerToIDEFileResolverTest extends JavaCodeInsightFixtureTestCase {
  private Project project;
  private PsiClass class1;
  private PsiClass class2;
  private PsiFile file1;
  private PsiFile file2;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    project = this.getProject();

    class1 = this.myFixture.addClass("package com.java.pkg; class Class {}");
    class2 = this.myFixture.addClass("package com.java.pkg; class ClassTest {}");
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testGetCloudPathFromJavaFile() {
    PsiJavaFile psiJavaFile = mock(PsiJavaFile.class);
    when(psiJavaFile.getPackageName()).thenReturn("com.java.package");
    when(psiJavaFile.getName()).thenReturn("Class.java");
    assertEquals(ServerToIDEFileResolver.getCloudPathFromJavaFile(psiJavaFile),
        "com/java/package/Class.java");
  }

  // When searching for full file system path.
  @Ignore
  public void ignore_testGetFileFromPath_fullPath() {
    // TODO(joaomartins): Find out why project.getBaseDir() is returning a different tempDir to
    // myFixture.
    file1 = this.myFixture.addFileToProject(
        "path/to/prj/src/main/com/java/package/Class.java", "");
    file2 = this.myFixture.addFileToProject(
            "path/to/prj/src/test/com/java/package/ClassTest.java", "");

    ServerToIDEFileResolver fileResolver = new ServerToIDEFileResolver();
    assertEquals(
        fileResolver.getFileFromPath(
            project, "path/to/prj/src/main/com/java/package/Class.java"),
        file1.getVirtualFile());
  }

  // When searching for the package and class name.
  @Test
  public void testGetFileFromPath_packageClass() {
    ServerToIDEFileResolver fileResolver = new ServerToIDEFileResolver();

    assertEquals(class1.getContainingFile().getVirtualFile(),
        fileResolver.getFileFromPath(project, "com/java/pkg/Class.java"));
  }

  // When searching for file name only.
  @Test
  public void testGetFileFromPath_fileName() {
    ServerToIDEFileResolver fileResolver = new ServerToIDEFileResolver();

    assertEquals(class1.getContainingFile().getVirtualFile(),
        fileResolver.getFileFromPath(project, "Class.java"));
  }

  @Test
  public void testGetPackageFromPath() {
    assertEquals("com.java.package",
        ServerToIDEFileResolver.getPackageFromPath("/com/java/package/Class.java"));
    assertEquals("",
        ServerToIDEFileResolver.getPackageFromPath("Class.java"));
    assertEquals("package",
        ServerToIDEFileResolver.getPackageFromPath("/package/Class.java"));
    assertEquals("com.java.package",
        ServerToIDEFileResolver.getPackageFromPath("/com//java/package//Class.java"));
    assertEquals("com.java.package",
        ServerToIDEFileResolver.getPackageFromPath("com/java/package/Class.java"));
    assertEquals("", ServerToIDEFileResolver.getPackageFromPath(""));
  }

  @Test
  public void testGetClassNameFromPath() {
    assertEquals("Class",
        ServerToIDEFileResolver.getClassNameFromPath("com/java/package/Class.java"));
    assertEquals("Class",
        ServerToIDEFileResolver.getClassNameFromPath("com/java/package/Class"));
    assertEquals("", ServerToIDEFileResolver.getClassNameFromPath(""));
    assertEquals("Class", ServerToIDEFileResolver.getClassNameFromPath("Class"));
    assertEquals("Class",
        ServerToIDEFileResolver.getClassNameFromPath("com.java/package/Class.java"));
    assertEquals("Class", ServerToIDEFileResolver.getClassNameFromPath("com.java/package/Class"));
  }
}
