/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.MavenTestUtils;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import java.io.File;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DependencyVersionWithBomInspectionTest {
  private CodeInsightTestFixture fixture;
  private MavenModuleBuilder moduleBuilder;

  @Before
  public void setUp() throws Exception {
    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
        IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder();

    fixture =
        IdeaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(
                projectBuilder.getFixture(), new LightTempDirTestFixtureImpl(true));
    fixture.setUp();

    fixture.enableInspections(new DependencyVersionWithBomInspection());
  }

  @Test
  public void inspect_dependencyVersionWithBom_showWarning() {

//    Module module =
//        MavenTestUtils.getInstance().createNewMavenModule(moduleBuilder, fixture.getProject());
    fixture.getProject().getComponent(CloudLibraryProjectState.class);


    String pomXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <artifactId>test-bom-inpection</artifactId>\n"
            + "    <packaging>war</packaging>\n"
            + "    <dependencyManagement>\n"
            + "        <dependencies>\n"
            + "            <dependency>\n"
            + "                <groupId>com.google.cloud</groupId>\n"
            + "                <artifactId>google-cloud-bom</artifactId>\n"
            + "                <version>0.42.1-alpha</version>\n"
            + "            </dependency>\n"
            + "        </dependencies>\n"
            + "    </dependencyManagement>\n"
            + "    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>com.google.cloud</groupId>\n"
            + "            <artifactId>google-cloud-spanner</artifactId>\n"
            + "            <version>0.33.0-beta</version>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n"
            + "</project>";

    fixture.configureByText(XmlFileType.INSTANCE, pomXml);
    fixture.checkHighlighting();
    //    testInspection(
    //
    // "inspections/dependencyVersionWithBomInspection/dependencyVersionAndNoBomDoesNotWarn");
  }

  private void testInspection(String testDataDirName) {
    fixture.setTestDataPath(new File("").getAbsolutePath() + "/testData/");

    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                ApplicationManager.getApplication()
                    .runReadAction(
                        () ->
                            fixture.testInspection(
                                testDataDirName,
                                new LocalInspectionToolWrapper(
                                    new DependencyVersionWithBomInspection()))));
  }

  @After
  public void tearDown() throws Exception {
    fixture.tearDown();
  }
}
