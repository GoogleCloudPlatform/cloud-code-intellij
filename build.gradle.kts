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

import net.researchgate.release.GitAdapter
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.3.7"
    id("com.diffplug.gradle.spotless") version "3.14.0"
    id("net.researchgate.release") version "2.7.0"

    kotlin("jvm") version "1.3.11"
}

allprojects {
    repositories {
        jcenter()
    }

    apply(plugin = "org.jetbrains.intellij")
    apply(plugin = "kotlin")
    apply(plugin = "com.diffplug.gradle.spotless")

    intellij {
        setPlugins("yaml")

        type = project.properties["ideaEdition"].toString()
        version = project.properties["ideaVersion"].toString()
        intellijRepo = project.properties["intellijRepoUrl"].toString()
        updateSinceUntilBuild = false
        project.properties["alternativeIdePath"]?.let { alternativeIdePath = it.toString() }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    dependencies {
        testCompile("com.google.truth:truth:+") {
            exclude(group = "com.google.guava", module = "guava")
        }
        testCompile("io.mockk:mockk:+") {
            // this ensures kotlin plugin/version takes precedence, mockk updates less often
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        }
    }

    spotless {
        kotlin {
            target("**/src/**/*.kt")
            // Set ktlint to follow the Android Style Guide for source files
            ktlint().userData(mapOf("android" to "true"))
        }

        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint()
        }
    }
}

dependencies {
    compile(project(":skaffold"))
    compile(project(":skaffold-editing"))
    compile(project(":common-test-lib"))

    compile(files("lib/GoogleFeedback.jar"))

    compile("com.google.protobuf:protobuf-java:2.5.0")
}

val intellijRepoChannel: String by project
val publishPlugin: PublishTask by tasks
publishPlugin {
    username(System.getenv("CONTAINER_TOOLS_REPO_USERNAME"))
    password(System.getenv("CONTAINER_TOOLS_REPO_PASSWORD"))
    channels(intellijRepoChannel)
}

release {
    tagTemplate = "v\${version}"

    val git: GitAdapter.GitConfig = getProperty("git") as GitAdapter.GitConfig
    git.requireBranch = "^release_v\\d+.*$"
}

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
