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
package com.google.cloud.tools.intellij.appengine.util;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for working with Intellij Psi constructs
 */
public class PsiUtils {

  // don't instantiate
  private PsiUtils() {
  }

  /**
   * NOTE : requires readAction, if not on the dispatch thread
   * Get the public class in a file that is annotated with a certain annotation
   * @param psiJavaFile
   * @param annotationFqn
   * @return
   */
  public static PsiClass getPublicAnnotatedClass(PsiJavaFile psiJavaFile, String annotationFqn) {
    PsiClass[] classes = psiJavaFile.getClasses();
    for (PsiClass cls : classes) {
      PsiModifierList modifierList = cls.getModifierList();
      if (modifierList != null && modifierList.hasExplicitModifier(PsiModifier.PUBLIC)) {
        if (AnnotationUtil.isAnnotated(cls, annotationFqn, false)) {
          return cls;
        }
      }
    }
    return null;
  }

  /**
   * NOTE : requires readAction, if not on the dispatch thread
   * Get an field annotated with a particular annotation from a class
   * @param entityClass
   * @param annotationFqn
   * @return the first field found with the annotation
   */
  public static PsiField getAnnotatedFieldFromClass(PsiClass entityClass, String annotationFqn) {
    for (PsiField field : entityClass.getAllFields()) {
      if (AnnotationUtil.isAnnotated(field, annotationFqn, false)) {
        return field;
      }
    }
    return null;
  }

  /**
   * Add (or overwrite) a file in a directory
   * NOTE : This must run in a runWriteAction
   *
   * @param dir
   * @param file
   * @return the newly written file
   */
  public static PsiFile addOrReplaceFile(PsiDirectory dir, PsiFile file) {
    final PsiFile existingFile = dir.findFile(file.getName());
    if (existingFile != null) {
      existingFile.delete();
    }
    return (PsiFile)dir.add(file);
  }

  /**
   * NOTE : This must run in a runWriteAction
   * Add file to directory only if it doesn't exist
   *
   * @param dir
   * @param file
   * @return the existing file or the newly added file
   */
  // for whatever reason if we don't want to replace ?
  // this must run in a "runWriteAction" block
  public static PsiFile addIfMissingFile(PsiDirectory dir, PsiFile file) {
    final PsiFile existingFile = dir.findFile(file.getName());
    if (existingFile != null) {
      return existingFile;
    }
    return (PsiFile)dir.add(file);
  }

  /**
   * NOTE : This must run in a runWriteAction
   * Create (or overwrite) a directory
   *
   * @param parent
   * @param dirName
   * @return the new/existing directory
   */
  public static PsiDirectory addOrReplaceDirectory(PsiDirectory parent, String dirName) {
    deleteIfExists(parent, dirName);
    return parent.createSubdirectory(dirName);
  }

  /**
   * NOTE : This must run in a runWriteAction
   * Add directory to directory only if it doesn't exist
   *
   * @param parent
   * @param dirName
   * @return the new/existing directory
   */
  public static PsiDirectory addIfMissingDirectory(PsiDirectory parent, String dirName) {
    final PsiDirectory existingDir = parent.findSubdirectory(dirName);
    if (existingDir != null) {
      return existingDir;
    }
    return parent.createSubdirectory(dirName);
  }

  /**
   * NOTE : This must run in a runWriteAction
   * Delete a directory (no error if it doesn't exist)
   *
   * @param parent
   * @param dirName
   */
  public static void deleteIfExists(PsiDirectory parent, String dirName) {
    final PsiDirectory existingDir = parent.findSubdirectory(dirName);
    if (existingDir != null) {
      existingDir.delete();
    }
  }

  /**
   * NOTE this must run in a runWriteAction
   * Create a file using intellijs built in system with the defined type and format it
   *
   * @param project
   * @param filename
   * @param fileType
   * @param fileContents
   * @return
   */
  public static PsiFile createFormattedFile(Project project, String filename, FileType fileType, String fileContents) {
    final PsiFile rawFile = PsiFileFactory.getInstance(project).createFileFromText(filename, fileType, fileContents);
    final PsiFile formattedFile = (PsiFile)CodeStyleManager.getInstance(project).reformat(rawFile);
    return formattedFile;
  }

  /**
   * Returns the parent class of element. If element is a class, it returns element.
   * If element is null, it returns null.
   *
   * @param element the PsiElement we want to know the class of.
   * @return
   */
  public static PsiClass findClass(PsiElement element) {
    return (element instanceof PsiClass) ? (PsiClass) element : PsiTreeUtil.getParentOfType(element, PsiClass.class);
  }

  /**
   * Returns the PsiJavaFile associated with <code>actionEvent</code> and null if non exists.
   * @param actionEvent the actionEvent to be evaluated.
   * @return the PsiJavaFile associated with <code>actionEvent</code> and null if non exists.
   */
  @Nullable
  public static PsiJavaFile getPsiJavaFileFromContext(AnActionEvent actionEvent) {
    PsiFile psiFile = actionEvent.getData(LangDataKeys.PSI_FILE);
    if (psiFile == null || !(psiFile instanceof PsiJavaFile)) {
      return null;
    }
    else {
      return (PsiJavaFile) psiFile;
    }
  }

  /**
   * Returns {@code true} if {@code type} is a parameterized type and {@code false} otherwise.
   *
   * @param type the type to be evaluated
   * @return {@code true} if {@code type} is a parameterized type and {@code false} otherwise
   */
  public static boolean isParameterizedType(PsiType type) {
    if(!(type instanceof  PsiClassType)) {
      return false;
    }

    Boolean accepted = type.accept(new PsiTypeVisitor<Boolean>() {
      @Nullable
      @Override
      public Boolean visitClassType(PsiClassType classType) {
        return classType.getParameterCount() > 0;
      }
    });
    return Boolean.TRUE.equals(accepted);
  }
}
