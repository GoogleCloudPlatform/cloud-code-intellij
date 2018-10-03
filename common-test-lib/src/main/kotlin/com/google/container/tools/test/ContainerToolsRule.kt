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

import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A custom [TestRule] for Container Tools unit tests.
 *
 * This rule adds the following functionality:
 *
 *  * Creates an [IdeaProjectTestFixture] and makes it available for tests via property. By default
 *    this text fixture is "light", i.e. instance of [LightIdeaTestFixture].
 */
class ContainerToolsRule : TestRule {
    lateinit var ideaProjectTestFixture: IdeaProjectTestFixture

    override fun apply(baseStatement: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    setUpRule()
                    baseStatement.evaluate()
                } finally {
                    tearDownRule()
                }
            }
        }

    private fun setUpRule() {
        ideaProjectTestFixture =
            IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder().fixture
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { ideaProjectTestFixture.setUp() })
    }

    private fun tearDownRule() {
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { ideaProjectTestFixture.tearDown() })
    }
}
