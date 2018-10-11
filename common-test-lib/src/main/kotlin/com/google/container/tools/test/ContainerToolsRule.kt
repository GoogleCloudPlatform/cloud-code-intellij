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
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import io.mockk.MockKAnnotations
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.picocontainer.MutablePicoContainer
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

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

    override fun apply(baseStatement: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    setUpRule()
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

    private fun setUpRule() {
        ideaProjectTestFixture =
            IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder().fixture
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { ideaProjectTestFixture.setUp() })

        MockKAnnotations.init(testInstance, relaxed = true)
        replaceServices()
    }

    private fun tearDownRule() {
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { ideaProjectTestFixture.tearDown() })
    }

    /**
     * Replaces all services annotated with [TestService].
     */
    private fun replaceServices() {
        for (member in testInstance::class.declaredMemberProperties) {
            if (member.annotations.filter { it is TestService }.isNotEmpty()) {
                member as KProperty1<Any?, Any?>
                member.isAccessible = true
                val service: Any = member.get(testInstance)!!
                setService(service)
            }
        }
    }

    /**
     * Replaces the service binding in the [MutablePicoContainer] with the given instance and
     * returns the original service instance.
     *
     * @param newInstance the new instance to register
     */
    private fun setService(newInstance: Any) {
        with(ApplicationManager.getApplication().picoContainer as MutablePicoContainer) {
            unregisterComponent(newInstance::class.java.name)
            registerComponentInstance(
                newInstance::class.java.name,
                newInstance
            )
        }
    }
}
