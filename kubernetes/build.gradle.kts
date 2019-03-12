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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//ext {
//    kotlin_version = '1.2.0'
//}

buildscript {

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.0")

    }
}

allprojects {
    intellij {
        setPlugins("yaml")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    dependencies {
        compile( "io.kubernetes:client-java:4.0.0")

        testCompile("io.mockk:mockk:+") {
            // this ensures kotlin plugin/version takes precedence, mockk updates less often
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compile(project(":kubernetes:core"))
    compile(project(":kubernetes:skaffold"))

    compile("com.google.protobuf:protobuf-java:2.5.0")

    testCompile(project(":kubernetes:common-test-lib"))

    compile("compile /org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.2.0/")
}

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
