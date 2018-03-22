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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xml.DomUtil;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependencies;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/** A helper class that adds dependencies of Cloud libraries to a given module. */
final class CloudLibraryDependencyWriter {

  private static final NotificationGroup NOTIFICATION_GROUP =
      new NotificationGroup(
          new PropertiesFileFlagReader().getFlagString("notifications.plugin.groupdisplayid"),
          NotificationDisplayType.BALLOON,
          true,
          null,
          GoogleCloudCoreIcons.CLOUD);

  /** Prevents instantiation. */
  private CloudLibraryDependencyWriter() {}

  /**
   * Adds the given set of {@link CloudLibrary CloudLibraries} to the given {@link Module}.
   *
   * <p>For a module whose project manages its dependencies through Maven, the list of libraries
   * will be added as dependencies to the {@code pom.xml}. For all other dependency management
   * systems, the libraries will be downloaded and added directly to the module's classpath.
   *
   * <p>If a BOM version is specified, then the google-cloud-java BOM will be written to the module
   * and the individual dependencies will defer to this BOM to manage their versions.
   *
   * @param libraries the set of {@link CloudLibrary CloudLibraries} to add
   * @param module the {@link Module} to add the libraries to
   * @param bomVersion the version of the BOM to write. May be null
   */
  static void addLibraries(
      @NotNull Set<CloudLibrary> libraries, @NotNull Module module, @Nullable String bomVersion) {
    if (libraries.isEmpty()) {
      return;
    }

    Project project = module.getProject();
    if (MavenProjectsManager.getInstance(project).isMavenizedModule(module)) {
      addLibrariesToMavenModule(libraries, module, bomVersion);
    } else {
      // TODO(nkibler): Handle non-Maven projects.
    }
  }

  /**
   * Adds the given set of {@link CloudLibrary CloudLibraries} to the given Maven {@link Module}.
   *
   * <p>If the given {@link Module} is not a Maven module, this method may throw an undefined {@link
   * RuntimeException}.
   *
   * @param libraries the set of {@link CloudLibrary CloudLibraries} to add
   * @param module the Maven {@link Module} to add the libraries to
   * @param bomVersion the version of the BOM to write. May be null
   */
  private static void addLibrariesToMavenModule(
      @NotNull Set<CloudLibrary> libraries, @NotNull Module module, @Nullable String bomVersion) {
    Project project = module.getProject();
    MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
    MavenDomProjectModel model =
        MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());

    new WriteCommandAction(project, DomUtil.getFile(model)) {
      @Override
      protected void run(@NotNull Result result) {
        List<MavenDomDependency> dependencies = model.getDependencies().getDependencies();
        Map<Boolean, List<MavenId>> mavenIdsMap =
            libraries
                .stream()
                .map(CloudLibraryUtils::getFirstJavaClientMavenCoordinates)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(MavenUtils::toMavenId)
                .collect(
                    Collectors.partitioningBy(
                        mavenId -> MavenUtils.isMavenIdInDependencyList(mavenId, dependencies)));

        // The MavenIds in the "true" list are already in the pom.xml, so we don't duplicate them
        // and warn the user that they weren't added.
        List<MavenId> ignoredMavenIds = mavenIdsMap.get(true);
        notifyIgnoredDependencies(ignoredMavenIds, project);

        // The MavenIds in the "false" list are not in the pom.xml, so we add them and notify the
        // user when it's complete.
        List<MavenId> newMavenIds = mavenIdsMap.get(false);
        if (!newMavenIds.isEmpty()) {
          newMavenIds.forEach(mavenId -> writeNewMavenDependency(model, mavenId, bomVersion));

          if (bomVersion != null) {
            addBomToMavenModule(bomVersion, model);
          }

          notifyAddedDependencies(newMavenIds, project);
        }
      }
    }.execute();
  }

  /**
   * Adds the google-cloud-java BOM dependency to the module's dependencyManagement section.
   *
   * <p>If the BOM already exists, then this will update the version of the BOM to match the
   * supplied version.
   */
  private static void addBomToMavenModule(MavenDomProjectModel model, String bomVersion) {
    MavenDomDependencies mavenDomDependencies = model.getDependencyManagement().getDependencies();
    Optional<MavenDomDependency> bomDependencyOptional =
        mavenDomDependencies
            .getDependencies()
            .stream()
            .filter(
                mdd ->
                    CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_ARTIFACT_NAME.equals(
                        mdd.getArtifactId().getStringValue()))
            .findFirst();

    if (!bomDependencyOptional.isPresent()) {
      writeNewBom(model, bomVersion);
    } else {
      // update the version
      bomDependencyOptional.get().getVersion().setStringValue(bomVersion);
    }
  }

  /**
   * Writes the google-cloud-java dependency to the users pom.xml.
   *
   * <p>If a BOM version is supplied, the dependency will not be written with a version so that the
   * BOM can manage the version.
   */
  private static void writeNewMavenDependency(
      MavenDomProjectModel model, MavenId mavenId, @Nullable String bomVersion) {
    MavenDomDependency dependency = MavenDomUtil.createDomDependency(model, /* editor= */ null);
    dependency.getGroupId().setStringValue(mavenId.getGroupId());
    dependency.getArtifactId().setStringValue(mavenId.getArtifactId());

    // Only write the version to the dependency if there is no BOM
    if (bomVersion == null) {
      dependency.getVersion().setStringValue(mavenId.getVersion());
    }

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.CLIENT_LIBRARY_ADD_LIBRARY)
        .addMetadata(GctTracking.METADATA_BUILD_SYSTEM_KEY, GctTracking.METADATA_BUILD_SYSTEM_MAVEN)
        .addMetadata(GctTracking.METADATA_LABEL_KEY, mavenId.getDisplayString())
        .ping();
  }

  /** Writes the google-cloud-java BOM to the user's pom.xml. */
  private static void writeNewBom(MavenDomProjectModel model, String bomVersion) {
    MavenDomDependency bomDependency =
        model.getDependencyManagement().getDependencies().addDependency();
    bomDependency
        .getGroupId()
        .setStringValue(CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_GROUP_NAME);
    bomDependency
        .getArtifactId()
        .setStringValue(CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_ARTIFACT_NAME);
    bomDependency.getVersion().setStringValue(bomVersion);
    bomDependency.getType().setStringValue(CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_TYPE);
    bomDependency.getScope().setStringValue(CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_SCOPE);
  }

  /**
   * Notifies the user that the given list of {@link MavenId MavenIds} were ignored when attempting
   * to add them to their pom.xml because they already exist.
   *
   * <p>The notification shown has a {@link NotificationType#WARNING} type.
   *
   * @param ignoredMavenIds the list of {@link MavenId MavenIds} that were ignored
   * @param project the {@link Project} that was affected
   */
  private static void notifyIgnoredDependencies(List<MavenId> ignoredMavenIds, Project project) {
    if (ignoredMavenIds.isEmpty()) {
      return;
    }

    String title = GctBundle.message("cloud.libraries.depwriter.maven.ignored.deps.title");
    String mavenIdString = joinMavenIds(ignoredMavenIds);
    String message =
        GctBundle.message("cloud.libraries.depwriter.maven.ignored.deps.message", mavenIdString);
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            title, /* subtitle= */ null, message, NotificationType.WARNING);
    notification.notify(project);
  }

  /**
   * Notifies the user that the given list of {@link MavenId MavenIds} were added to their pom.xml.
   *
   * <p>The notification shown has an {@link NotificationType#INFORMATION} type.
   *
   * @param addedMavenIds the list of {@link MavenId MavenIds} that were added to the user's pom.xml
   * @param project the {@link Project} that was affected
   */
  private static void notifyAddedDependencies(List<MavenId> addedMavenIds, Project project) {
    if (addedMavenIds.isEmpty()) {
      return;
    }

    String title = GctBundle.message("cloud.libraries.depwriter.maven.added.deps.title");
    String mavenIdString = joinMavenIds(addedMavenIds);
    String message =
        GctBundle.message("cloud.libraries.depwriter.maven.added.deps.message", mavenIdString);
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            title, /* subtitle= */ null, message, NotificationType.INFORMATION);
    notification.notify(project);
  }

  /**
   * Joins the given list of {@link MavenId MavenIds} into a human-readable string.
   *
   * @param mavenIds the list of {@link MavenId MavenIds} to join
   */
  private static String joinMavenIds(List<MavenId> mavenIds) {
    return mavenIds
        .stream()
        .map(MavenId::getDisplayString)
        .map(string -> "- " + string)
        .collect(Collectors.joining("<br>"));
  }
}
