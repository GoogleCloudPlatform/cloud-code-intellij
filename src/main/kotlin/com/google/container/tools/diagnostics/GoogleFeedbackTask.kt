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

package com.google.container.tools.diagnostics

import com.android.tools.idea.diagnostics.error.AnonymousFeedback
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.net.HttpConfigurable
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Reports an error to Google Feedback in the background.
 *
 * @param project the current IntelliJ project
 * @param title the title to be displayed in the feedback UI
 * @param canBeCancelled specified if the feedback action can be cancelled
 * @param throwable the throwable the triggered this error feedback
 * @param params the map of parameters containing metadata about this feedback instance
 * @param errorMessage the error message e.g. from the throwable
 * @param errorDescription additional description that the user can enter from the UI
 * @param appVersion the full version of the IDE
 * @param callback the callback to be invoked upon successful transmission
 * @param errorCallback the callback to be invoked upon failed transmission
 * @param feedbackSender the instance of the feedback sender; defaults to an http sender
 */
class GoogleFeedbackTask(
    project: Project?,
    title: String,
    canBeCancelled: Boolean,
    private val throwable: Throwable?,
    private val params: Map<String, String>,
    private val errorMessage: String,
    private val errorDescription: String,
    private val appVersion: String,
    private val callback: (String) -> Unit,
    private val errorCallback: (Exception) -> Unit,
    private val feedbackSender: FeedbackSender = DEFAULT_FEEDBACK_SENDER
) : Task.Backgroundable(project, title, canBeCancelled) {

    companion object {
        @VisibleForTesting
        val CONTAINER_TOOLS_PRODUCT = "Container Tools for IntelliJ"
        @VisibleForTesting
        val CONTAINER_TOOLS_PACKAGE_NAME = "com.google.container.tools"
        private val DEFAULT_FEEDBACK_SENDER = NetworkFeedbackSender()
    }

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        try {
            val token = feedbackSender.sendFeedback(
                CONTAINER_TOOLS_PRODUCT,
                CONTAINER_TOOLS_PACKAGE_NAME,
                throwable,
                errorMessage,
                errorDescription,
                appVersion,
                params
            )
            callback(token)
        } catch (ex: Exception) {
            errorCallback(ex)
        }
    }

    /** Interface for sending feedback crash reports.  */
    interface FeedbackSender {

        /**
         * Sends the feedback report.
         *
         * @param feedbackProduct the name of the plugin
         * @param feedbackPackageName the root package of the plugin
         * @param cause the throwable the initiated this crash report
         * @param errorMessage the error message
         * @param errorDescription the error description
         * @param applicationVersion the version of the IDE
         * @param keyValues the map of metadata to send along with the report
         * @throws IOException if the transmission fails
         *
         */
        @Throws(IOException::class)
        fun sendFeedback(
            feedbackProduct: String,
            feedbackPackageName: String,
            cause: Throwable?,
            errorMessage: String,
            errorDescription: String,
            applicationVersion: String,
            keyValues: Map<String, String>
        ): String
    }

    private class ProxyHttpConnectionFactory : AnonymousFeedback.HttpConnectionFactory() {

        @Throws(IOException::class)
        override fun openHttpConnection(path: String): HttpURLConnection {
            return HttpConfigurable.getInstance().openHttpConnection(path)
        }
    }

    /**
     * [FeedbackSender] implementation that sends the feedback over the network.
     */
    private class NetworkFeedbackSender : FeedbackSender {
        companion object {
            private val connectionFactory = ProxyHttpConnectionFactory()
        }

        @Throws(IOException::class)
        override fun sendFeedback(
            feedbackProduct: String,
            feedbackPackageName: String,
            cause: Throwable?,
            errorMessage: String,
            errorDescription: String,
            applicationVersion: String,
            keyValues: Map<String, String>
        ): String {
            return AnonymousFeedback.sendFeedback(
                CONTAINER_TOOLS_PRODUCT,
                CONTAINER_TOOLS_PACKAGE_NAME,
                connectionFactory,
                cause,
                keyValues,
                errorMessage,
                errorDescription,
                applicationVersion
            )
        }
    }
}
