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
import hudson.Util;
import hudson.model.Node;
import hudson.model.RootAction;
import java.util.Date;
import java.util.Map;

@Extension
public class ChaosButlerRootAction implements RootAction {
    @Override
    public String getIconFileName() {
        return ChaosButlerGlobalConfiguration.get().getInterval() > 0
                ? "/plugin/chaos-butler/images/24x24/chaos-butler.png"
                : null;
    }

    @Override
    public String getDisplayName() {
        return Messages.ChaosButlerGlobalConfiguration_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "chaos-butler";
    }

    public String getNext() {
        ChaosButlerGlobalConfiguration config = ChaosButlerGlobalConfiguration.get();
        return config.getInterval() > 0 ? Util
                .getTimeSpanString(Math.max(0L, config.getNextWake() - System.currentTimeMillis())) : null;
    }

    public Map<Date, Node> getRecentVictims() {
        return ChaosButlerGlobalConfiguration.get().getRecentVictims();
    }

}
