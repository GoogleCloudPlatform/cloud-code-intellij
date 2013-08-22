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
package com.google.gct.intellij.endpoints.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCommandArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;

/* utilities for editing gradle files (like settings.gradle and build.gradle) */
public class GradleUtils {

  public static final String REPO_MAVEN_CENTRAL = "mavenCentral()";
  public static final String DEPENDENCY_COMPILE = "compile project(':%s')";
  public static final String ARGUMENT_INCLUDE = "':%s'";

  public static final String METHOD_REPOSITORIES = "repositories";
  public static final String METHOD_DEPENDENCIES = "dependencies";
  public static final String STATEMENT_INCLUDE = "include";

  private GradleUtils() {}

  /* convenience method for adding gradle repositories */
  public static void addRepository(Project project, GroovyFile gradleFile, String repository) {
    addExpressionToMethodClosure(project, gradleFile, METHOD_REPOSITORIES, repository);
  }

  /* convenience method for adding gradle dependencies */
  public static void addDependency(Project project, GroovyFile gradleFile, String dependency) {
    addExpressionToMethodClosure(project, gradleFile, METHOD_DEPENDENCIES, dependency);
  }

  /**
   * Adds an expression to a top level method's closure
   * TODO: This seems to work for broken gradle files, NEED to find a way to check if the gradle file is traversable
   *
   * @param project
   * @param gradleFile
   * @param methodName
   *    The top level method to add to
   * @param expression
   *    The expression to add in
   */
  public static void addExpressionToMethodClosure(Project project, GroovyFile gradleFile, String methodName, String expression) {
    for (PsiElement statement : gradleFile.getStatements()) {
      if (statement instanceof GrMethodCallExpression) {

        // the first child is always a grReferenceExpression for method calls
        if (statement.getFirstChild() instanceof GrReferenceExpression && statement.getFirstChild().getText().equals(methodName)) {
          GrClosableBlock[] closureArguments = ((GrMethodCallExpression)statement).getClosureArguments();
          if (closureArguments.length > 0) {
            // really, there should only be one closure argument, should we error if more? for now just adding to the first
            for (PsiElement repoStatement : closureArguments[0].getStatements()) {
              if (repoStatement.getText().equals(expression)) {
                // if we found the expression, then do nothing
                return;
              }
            }
            // we didn't find it, so add it in
            closureArguments[0].addBefore(GroovyPsiElementFactory.getInstance(project).createExpressionFromText(expression),
                                          closureArguments[0].getLastChild());
            CodeStyleManager.getInstance(project).reformat(closureArguments[0]);
            return;
          }
          // if we find no closure for this "method" we can just add in the expression in another block or create a block (bottom)
        }
      }
    }
    // we didn't find method or something was malformed so add it
    gradleFile.add(CodeStyleManager.getInstance(project).reformat(
      GroovyPsiElementFactory.getInstance(project).createExpressionFromText(methodName + "{\n" + expression + "}\n")));
  }

  /* convenience method for adding gradle includes of modules */
  public static void includeModule(Project project, GroovyFile gradleFile, String module) {
    addArgumentToApplicationStatement(project, gradleFile, STATEMENT_INCLUDE, String.format(ARGUMENT_INCLUDE, module));
  }

  /**
   * Adds an argument to a top level application statement
   * TODO: This seems to work for broken gradle files, NEED to find a way to check if the gradle file is traversable
   *
   * @param project
   * @param gradleFile
   * @param applicationStatement
   * @param argument
   */
  public static void addArgumentToApplicationStatement(Project project,
                                                       GroovyFile gradleFile,
                                                       String applicationStatement,
                                                       String argument) {
    for (PsiElement statement : gradleFile.getStatements()) {
      if (statement instanceof GrApplicationStatement) {

        // the first child is always a GrReferenceExpression for application statments
        if (statement.getFirstChild() instanceof GrReferenceExpression &&
            statement.getFirstChild().getText().equals(applicationStatement)) {

          GrCommandArgumentList args = ((GrApplicationStatement)statement).getArgumentList();
          for (PsiElement arg : args.getChildren()) {
            if (arg.getText().equals(argument)) {
              // if we found the arg, then do nothing
              return;
            }
          }
          // we didn't find it so add it in
          args.addAfter(GroovyPsiElementFactory.getInstance(project).createExpressionFromText(argument), args.getLastChild());
          return;
        }
      }
    }
    // we didn't find statement or something was malformed so add it
    gradleFile.add(CodeStyleManager.getInstance(project)
                     .reformat(GroovyPsiElementFactory.getInstance(project).createExpressionFromText(applicationStatement + argument)));
  }
}
