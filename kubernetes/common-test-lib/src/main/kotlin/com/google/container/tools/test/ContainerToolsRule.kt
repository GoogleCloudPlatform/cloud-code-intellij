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

package com.google.container.tools.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import io.mockk.MockKAnnotations
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.picocontainer.MutablePicoContainer
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

/**
 * A custom [TestRule] for Container Tools unit tests.
 *
 * This rule adds the following functionality:
 *
 *  * Creates an [IdeaProjectTestFixture] and makes it available for tests via property. By default
 *    this text fixture is "light", i.e. instance of [LightIdeaTestFixture].
 *  * Replaces all fields annotated with [TestService] in the
 *    [PicoContainer][org.picocontainer.PicoContainer] with the field values.
 */
class ContainerToolsRule(private val testInstance: Any) : TestRule {
    lateinit var ideaProjectTestFixture: IdeaProjectTestFixture
    private val filesToDelete: MutableList<File> = mutableListOf()

    override fun apply(baseStatement: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    setUpRule(description)
                    executeTest(baseStatement, description)
                } finally {
                    tearDownRule()
                }
            }
        }

    /** Checks the test method for additional annotations such as UI thread, and runs it. */
    private fun executeTest(baseStatement: Statement, description: Description) {
        if (description.annotations.any { it is UiTest }) {
            EdtTestUtil.runInEdtAndWait(ThrowableRunnable { baseStatement.evaluate() })
        } else {
            baseStatement.evaluate()
        }
    }

    private fun setUpRule(description: Description) {
        ideaProjectTestFixture =
            IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder().fixture
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { ideaProjectTestFixture.setUp() })

        MockKAnnotations.init(testInstance, relaxed = true)
        replaceServices()
        createTestFiles(description.methodName)
    }

    private fun tearDownRule() {
        filesToDelete.forEach { it.delete() }
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { ideaProjectTestFixture.tearDown() })
    }

    /**
     * Replaces all services annotated with [TestService].
     */
    private fun replaceServices() {
        for (member in getMembersWithAnnotation(TestService::class)) {
            // See https://youtrack.jetbrains.com/issue/KT-16432
            @Suppress("UNCHECKED_CAST")
            member as KProperty1<Any?, Any?>

            member.isAccessible = true
            val service: Any = member.get(testInstance)!!
            setService(member.returnType.javaType.typeName, service)
        }
    }

    /**
     * Creates all [File] annotated with [TestFile] in the given directory name.
     *
     * @param directoryName the name of the directory to create the test files in
     */
    private fun createTestFiles(directoryName: String) {
        for (member in getMembersWithAnnotation(TestFile::class)) {
            member.isAccessible = true
            if (!member.returnType.isSubtypeOf(File::class.createType()) ||
                member !is KMutableProperty<*>
            ) {
                throw IllegalArgumentException(
                    "@TestFile can only annotate mutable fields of type " +
                        "java.io.File"
                )
            }

            val annotation: TestFile? = member.findAnnotation()
            val directory: File = FileUtil.createTempDirectory(directoryName, null)
            val file = File(directory, annotation?.name)
            if (!file.createNewFile()) {
                throw IOException("Can't create file: $file")
            }
            annotation?.let {
                if (!it.contents.isEmpty()) {
                    FileUtil.writeToFile(file, it.contents)
                }
            }
            filesToDelete.add(file)
            member.setter.call(testInstance, file)
        }
    }

    /**
     * Returns a list of members containing the given annotation.
     *
     * @param annotation find members containing this annotation
     */
    private fun getMembersWithAnnotation(annotation: KClass<out Annotation>):
        List<KProperty1<out Any, Any?>> =
        testInstance::class.declaredMemberProperties.filter { member ->
            member.annotations.filter {
                annotation.isInstance(it)
            }.isNotEmpty()
        }

    /**
     * Replaces the service binding in the [MutablePicoContainer] with the given instance and
     * returns the original service instance.
     *
     * @param javaClassName Java class name that is used to register/replace IDE service.
     * @param newInstance the new instance to register
     */
    private fun setService(javaClassName: String, newInstance: Any) {
        with(ApplicationManager.getApplication().picoContainer as MutablePicoContainer) {
            unregisterComponent(javaClassName)
            registerComponentInstance(javaClassName, newInstance)
        }
    }
}
