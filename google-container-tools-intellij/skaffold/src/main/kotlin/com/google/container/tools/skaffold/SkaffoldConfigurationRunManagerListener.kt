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

package com.google.container.tools.skaffold

import com.google.container.tools.core.analytics.UsageTrackerProvider
import com.google.container.tools.skaffold.metrics.SKAFFOLD_DEV_CONFIGURATION_ADDED
import com.google.container.tools.skaffold.metrics.SKAFFOLD_SINGLE_RUN_CONFIGURATION_ADDED
import com.google.container.tools.skaffold.run.SkaffoldDevConfiguration
import com.google.container.tools.skaffold.run.SkaffoldSingleRunConfiguration
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project

/**
 * Project component that connects itself to project message bus when any project opens and uses
 * [RunManagerListener] events to catch additions of new Skaffold run configurations to send
 * client side pings.
 */
class SkaffoldConfigurationRunManagerListener(val project: Project) : ProjectComponent {
    override fun projectOpened() {
        project.messageBus.connect().subscribe(RunManagerListener.TOPIC,
            object : RunManagerListener {
                override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
                    if (settings.configuration is SkaffoldDevConfiguration) {
                        UsageTrackerProvider.instance.usageTracker.trackEvent(
                            SKAFFOLD_DEV_CONFIGURATION_ADDED
                        ).ping()
                    }
                    if (settings.configuration is SkaffoldSingleRunConfiguration) {
                        UsageTrackerProvider.instance.usageTracker.trackEvent(
                            SKAFFOLD_SINGLE_RUN_CONFIGURATION_ADDED
                        ).ping()
                    }
                }
            })
    }
}
