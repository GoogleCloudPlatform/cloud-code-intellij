package com.google.container.tools.test

/**
 * Marks a temporary [java.io.File] used for testing purposes.
 *
 * [ContainerToolsRule] manages the creation, injection, and destruction of this file. For
 * example:
 *
 * ```
 * @get:Rule val  ContainerToolsRule rule = new ContainerToolsRule(this)
 *
 * @TestFile(name = "my.file", contents = "Some contents") testFile: File
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestFile(val name: String, val contents: String = "")
