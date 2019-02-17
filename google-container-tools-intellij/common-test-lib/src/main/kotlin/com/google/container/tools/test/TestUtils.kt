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

import com.intellij.util.ThrowableRunnable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

/**
 * Asserts that the given [ThrowableRunnable] throws an exception of type
 * `expectedThrowable` when executed.
 *
 * @param expectedThrowable the class of the exception that should be thrown
 * @param runnable the [ThrowableRunnable] that should throw
 * @param <T> the type of exception that should be thrown
 * @return the thrown exception of type `expectedThrowable`
 * @throws AssertionError if the given [ThrowableRunnable] throws a different type of
 * exception or does not throw at all
</T> */
fun <T : Throwable> expectThrows(
    expectedThrowable: KClass<T>,
    runnable: ThrowableRunnable<T>
): T {
    try {
        runnable.run()
    } catch (thrown: Throwable) {
        if (expectedThrowable.isInstance(thrown)) {
            return expectedThrowable.cast(thrown)
        }
        val message = String.format(
            "Unexpected exception type thrown; expected '%s' but was '%s'.",
            expectedThrowable.simpleName, thrown::class.simpleName
        )
        throw AssertionError(message, thrown)
    }

    val message = String.format(
        "Expected '%s' to be thrown, but nothing was thrown.",
        expectedThrowable.simpleName
    )
    throw AssertionError(message)
}
