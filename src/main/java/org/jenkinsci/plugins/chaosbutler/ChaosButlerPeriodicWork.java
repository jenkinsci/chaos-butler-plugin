/*
 * Copyright 2016 CloudBees, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.chaosbutler;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Extension
public class ChaosButlerPeriodicWork extends AsyncPeriodicWork {
    public ChaosButlerPeriodicWork() {
        super("Chaos Butler");
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.SECONDS.toMillis(10);
    }

    @Override
    protected Level getNormalLoggingLevel() {
        return Level.FINEST;
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        ChaosButlerGlobalConfiguration.get().wake(listener);
    }
}
