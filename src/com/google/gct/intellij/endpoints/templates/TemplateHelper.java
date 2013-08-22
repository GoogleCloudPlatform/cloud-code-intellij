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

package com.google.gct.intellij.endpoints.templates;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gct.intellij.endpoints.util.PsiUtils;
import com.google.gct.intellij.endpoints.util.ResourceUtils;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PropertyUtil;

import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Functionality to help load in templates for AppEngine endpoints module
 */
public class TemplateHelper {

  /**
   * Helper class to track of ownerDomain and packagePath information for a
   * given Cloud Endpoint.
   */
  public static class EndpointPackageInfo {

    private final String ownerDomain;
    private final String packagePath;

    public EndpointPackageInfo(String ownerDomain, String packagePath) {
      this.ownerDomain = ownerDomain;
      this.packagePath = packagePath;
    }

    public String getOwnerDomain() {
      return ownerDomain;
    }

    public String getPackagePath() {
      return packagePath;
    }

    /**
     * Returns ownerDomain.packagePath, unless ownerDomain is empty. In that
     * case, just ownerDomain is returned.
     */
    @Override
    public String toString() {
      return (packagePath.length() == 0 ? ownerDomain : ownerDomain + "." + packagePath);
    }
  }

  /**
   * Given a root package, generates the Cloud Endpoint ownerDomain and
   * packagePath information.
   *
   * If rootPackage is com.foo.bar, then the ownerDomain will be "foo.com", and
   * the packagePath will be "bar".
   *
   * As another example, if the rootPackage is "com.foo.bar.baz", the
   * ownerDomain is still "foo.com", and the packgePath is "bar.baz".
   *
   * In the case where the rootPackage has only two components, packagePath is
   * the empty string.
   *
   * If rootPackage has less than two components, then
   * <code>DEFAULT_PACKAGE_INFO</code> will be returned.
   *
   * @param rootPackage the root package. Cannot be null.
   * @return
   */
  public static EndpointPackageInfo getEndpointPackageInfo(String rootPackage) {

    assert (rootPackage != null);

    String[] packageComponents = rootPackage.split("\\.");

    String ownerDomain = "";
    String packagePath = "";

    if (packageComponents.length < 2) {
      return DEFAULT_PACKAGE_INFO;
    } else {
      ownerDomain = packageComponents[1] + "." + packageComponents[0];
      for (int i = 2; i < packageComponents.length; i++) {
        packagePath += packageComponents[i];
        if (i != packageComponents.length - 1) {
          packagePath += ".";
        }
      }
    }

    return new EndpointPackageInfo(ownerDomain, packagePath);
  }

  public static final EndpointPackageInfo DEFAULT_PACKAGE_INFO = new EndpointPackageInfo(
    "mycompany.com", "services");

  private static final String FILE_TEMPLATE_MANAGER_FACTORY_CLASS = "ManagerFactoryClass.java.template";
  private static final String FILE_TEMPLATE_JPA_SWARM_SERVICE = "JpaSwarmService.java.template";
  private static final String FILE_TEMPLATE_PERSISTENCE_XML = "persistence.xml.template";
  private static final String FILE_TEMPLATE_WEB_XML = "web.xml.template";
  private static final String FILE_TEMPLATE_APPENGINE_WEB_XML = "appengine-web.xml.template";
  private static final String FILE_TEMPLATE_POM_XML = "pom.xml.template";
  private static final String FILE_TEMPLATE_BUILD_GRADLE = "build.gradle.template";

  private static final String TEMPLATE_MAVEN_ARTIFACT_NAME = "@MavenArtifactName@";
  private static final String TEMPLATE_APP_ID = "@AppId@";
  private static final String TEMPLATE_API_KEY = "@ApiKey@";
  private static final String TEMPLATE_API_NAME = "@ApiName@";
  private static final String TEMPLATE_ENDPOINT_OWNER_DOMAIN = "@EndpointOwnerDomain@";
  private static final String TEMPLATE_ENDPOINT_PACKAGE_PATH = "@EndpointPackagePath@";
  private static final String TEMPLATE_ENTITY_NAME = "@EntityName@";
  private static final String TEMPLATE_ENTITY_NAME_LOWER_CASE = "@EntityNameLowerCase@";
  private static final String TEMPLATE_ID_TYPE = "@IdType@";
  private static final String TEMPLATE_ID_GETTER = "@GetId@";
  private static final String TEMPLATE_PACKAGE = "@PackageName@";
  private static final String TEMPLATE_PERSISTENCE_PACKAGE_1 = "@PersistencePackageName1@";
  private static final String TEMPLATE_PERSISTENCE_PACKAGE_2 = "@PersistencePackageName2@";
  private static final String TEMPLATE_SERVICE_NAME = "@ServiceName@";
  private static final String TEMPLATE_SWARM_TYPE = "@SwarmType@";
  private static final String TEMPLATE_SWARM_FACTORY_TYPE = "@SwarmFactoryType@";
  private static final String TEMPLATE_FACTORY_CREATE = "@FactoryCreate@";

  private static final String SERVICE_CLASS_SUFFIX = "Endpoint";

  /**
   * Given the name of an entity, return the name of the API that GPE will
   * generate if generating a Cloud Endpoint for the entity.
   */
  public static String getApiNameFromEntityName(String entityName) {
    return getServiceNameFromEntityName(entityName).toLowerCase();
  }

  /**
   * Given the name of an entity, return the name of the Endpoint class that GPE
   * will generate for it.
   */
  public static String getServiceNameFromEntityName(String entityName) {
    return entityName + SERVICE_CLASS_SUFFIX;
  }


  private final String entityClass;
  private final String entityServiceClass;
  private final String idType;
  private final String idGetter;
  private final String javaPackage;
  private final Project project;

  // TODO: maybe this can just be all static functions
  public TemplateHelper(PsiClass javaClass, PsiField idField) {
    this.javaPackage = ((PsiJavaFile)javaClass.getContainingFile()).getPackageName();
    this.project = javaClass.getProject();
    this.entityClass = javaClass.getName();
    this.idGetter =
      PropertyUtil.suggestGetterName(idField.getName(), idField.getType()); //<-- should probably do a check to make sure this exists
    this.idType = idField.getType().getPresentableText();
    this.entityServiceClass = getServiceNameFromEntityName(entityClass);
  }

  /**
   * NOTE : requires runWriteAction
   * Load in entity manager factory as java file
   * @return the PsiFile reference to the loaded java file
   * @throws IOException
   */
  public static PsiFile loadJpaEntityManagerFactoryClass(Project project, String javaPackage) throws IOException {
    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_MANAGER_FACTORY_CLASS);

    templateString = templateString.replaceAll(TEMPLATE_PACKAGE, javaPackage);
    templateString = templateString.replaceAll(TEMPLATE_PERSISTENCE_PACKAGE_1, "javax.persistence.EntityManagerFactory");
    templateString = templateString.replaceAll(TEMPLATE_PERSISTENCE_PACKAGE_2, "javax.persistence.Persistence");
    templateString = templateString.replaceAll(TEMPLATE_SWARM_TYPE, "Entity");
    templateString = templateString.replaceAll(TEMPLATE_SWARM_FACTORY_TYPE, "EMF");
    templateString = templateString.replaceAll(TEMPLATE_FACTORY_CREATE, "Persistence.createEntityManagerFactory");

    return PsiUtils.createFormattedFile(project, "EMF.java", JavaFileType.INSTANCE, templateString);
  }

  /**
   * NOTE : requires runWriteAction
   * Non-static version of {@link #loadJpaEntityManagerFactoryClass(com.intellij.openapi.project.Project, String)}
   * @throws IOException
   */
  public PsiFile loadJpaEntityManagerFactoryClass() throws IOException {
    return loadJpaEntityManagerFactoryClass(project, javaPackage);
  }

  /**
   * NOTE : requires runWriteAction
   * Load in Endpoints template as java file
   * @throws IOException
   */
  public PsiFile loadJpaSwarmServiceClass() throws IOException {

    EndpointPackageInfo endpointPackageInfo = getEndpointPackageInfo(javaPackage);

    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_JPA_SWARM_SERVICE);

    templateString = templateString.replaceAll(TEMPLATE_PACKAGE, javaPackage);
    templateString = templateString.replaceAll(TEMPLATE_SERVICE_NAME, entityServiceClass);
    templateString = templateString.replaceAll(TEMPLATE_API_NAME, getApiNameFromEntityName(entityClass));
    templateString = templateString.replaceAll(TEMPLATE_ENDPOINT_OWNER_DOMAIN, endpointPackageInfo.getOwnerDomain());
    templateString = templateString.replaceAll(TEMPLATE_ENDPOINT_PACKAGE_PATH, endpointPackageInfo.getPackagePath());
    templateString = templateString.replaceAll(TEMPLATE_ENTITY_NAME, entityClass);
    templateString = templateString.replaceAll(TEMPLATE_ENTITY_NAME_LOWER_CASE, Introspector.decapitalize(entityClass));
    templateString = templateString.replaceAll(TEMPLATE_ID_TYPE, idType);
    templateString = templateString.replaceAll(TEMPLATE_ID_GETTER, idGetter);

    return PsiUtils.createFormattedFile(project, entityServiceClass + ".java", JavaFileType.INSTANCE, templateString);
  }


  /**
   * NOTE : requires runWriteAction
   * Load in the persistence.xml template
   * @throws IOException
   */
  public static PsiFile loadPersistenceXml(Project project) throws IOException {
    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_PERSISTENCE_XML);

    return PsiUtils.createFormattedFile(project, "persistence.xml", XmlFileType.INSTANCE, templateString);
  }

  /**
   * NOTE : requires runWriteAction
   * load in the gradle build file from templates
   * @throws IOException
   */
  public static PsiFile loadGradleBuildFile(Project project) throws IOException {
    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_BUILD_GRADLE);
    return PsiUtils.createFormattedFile(project, "build.gradle", PlainTextFileType.INSTANCE, templateString);

  }

  /**
   * NOTE : requires runWriteAction
   * Load in the maven pom.xml file
   * @throws IOException
   */
  public static PsiFile generatePomXml(Project project, String packageName, String artifactName) throws IOException {
    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_POM_XML);
    templateString = templateString.replaceAll(TEMPLATE_PACKAGE, packageName);
    templateString = templateString.replaceAll(TEMPLATE_MAVEN_ARTIFACT_NAME, artifactName);

    return PsiUtils.createFormattedFile(project, "pom.xml", XmlFileType.INSTANCE, templateString);
  }

  /**
   * NOTE : requires runWriteAction
   * load in the AppEngine web.xml from templates
   * @throws IOException
   */
  public static PsiFile loadWebXml(Project project) throws IOException {
    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_WEB_XML);

    return PsiUtils.createFormattedFile(project, "web.xml", XmlFileType.INSTANCE, templateString);
  }


  /**
   * NOTE : requires runWriteAction
   * load in the AppEngine appengine-web.xml from templates
   * @throws IOException
   */
  public static PsiFile generateAppEngineWebXml(Project project, String appId) throws IOException {
    String templateString = getTemplateResourceAsString(FILE_TEMPLATE_APPENGINE_WEB_XML);
    templateString = templateString.replaceAll(TEMPLATE_APP_ID, appId == null ? "" : appId.trim());
    return PsiUtils.createFormattedFile(project, "appengine-web.xml", XmlFileType.INSTANCE, templateString);
  }

  /**
   * NOTE : requires runWriteAction
   * Non-Static version of {@link #loadPersistenceXml(com.intellij.openapi.project.Project)}
   * @return
   * @throws IOException
   */
  public PsiFile loadPersistenceXml() throws IOException {
    return loadPersistenceXml(project);
  }

  /**
   * NOTE : requires runWriteAction
   * Load in generic java template
   * @throws IOException
   */
  public static PsiFile generateJavaTemplateContent(Project p, String className, String packageName) throws IOException {
    String templateString = getTemplateResourceAsString(className + ".java.template");
    templateString = templateString.replaceAll(TEMPLATE_PACKAGE, packageName);
    return PsiUtils.createFormattedFile(p, className + ".java", JavaFileType.INSTANCE, templateString);
  }

  /**
   * Load in generic java template with owner domain information
   * @throws IOException
   */
  public static PsiFile generateJavaTemplateContentWithOwnerDomain(Project p,
                                                                   String className,
                                                                   String packageName,
                                                                   EndpointPackageInfo endpointPackageInfo) throws IOException {
    String templateString = getTemplateResourceAsString(className + ".java.template");
    templateString = templateString.replaceAll(TEMPLATE_PACKAGE, packageName);
    templateString = templateString.replaceAll(TEMPLATE_ENDPOINT_OWNER_DOMAIN, endpointPackageInfo.getOwnerDomain());
    templateString = templateString.replaceAll(TEMPLATE_ENDPOINT_PACKAGE_PATH, endpointPackageInfo.getPackagePath());
    return PsiUtils.createFormattedFile(p, className + ".java", JavaFileType.INSTANCE, templateString);
  }

  /**
   * Load in generic java template with owner domain information and API Key (cloud console)
   * TODO: Maybe make this more generic, take a map?
   * @throws IOException
   */
  public static PsiFile generateJavaSampleTemplateWithOwnerDomainAndApiKey(Project p, String className, String packageName, EndpointPackageInfo endpointPackageInfo, String apiKey) throws IOException {
    String templateString = getTemplateResourceAsString(className + ".java.template");
    templateString = templateString.replaceAll(TEMPLATE_PACKAGE, packageName);
    templateString = templateString.replaceAll(TEMPLATE_ENDPOINT_OWNER_DOMAIN, endpointPackageInfo.getOwnerDomain());
    templateString = templateString.replaceAll(TEMPLATE_ENDPOINT_PACKAGE_PATH, endpointPackageInfo.getPackagePath());
    if (apiKey != null) {
      templateString = templateString.replaceAll(TEMPLATE_API_KEY, apiKey);
    }
    return PsiUtils.createFormattedFile(p, className + ".java", JavaFileType.INSTANCE, templateString);
  }


  /**
   * NOTE : requires runWriteAction
   * load in generic plain text (html, css, js, ...) file
   * @throws IOException
   */
  public static PsiFile generateStaticContent(Project p, String fileName) throws IOException {
    String templateString = getTemplateResourceAsString(fileName);
    return PsiUtils.createFormattedFile(p, fileName, PlainTextFileType.INSTANCE, templateString);
  }

  // requires runWriteAction
  private static String getTemplateResourceAsString(String resourceName) throws IOException {
    URL resourceURL = TemplateHelper.class.getResource(resourceName);
    return Resources.toString(resourceURL, Charsets.UTF_8);
  }
}
