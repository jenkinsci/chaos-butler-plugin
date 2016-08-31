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
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ChaosButlerOptOutNodeProperty extends NodeProperty<Node> {

    private final boolean optOut;

    @DataBoundConstructor
    public ChaosButlerOptOutNodeProperty(boolean optOut) {
        this.optOut = optOut;
    }

    public boolean isOptOut() {
        return optOut;
    }

    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.ChaosButlerOptOutNodeProperty_DisplayName();
        }

        @Override
        public NodeProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (formData.isNullObject()) {
                return null;
            }

            NodeProperty<?> property = super.newInstance(req, formData);
            return property instanceof ChaosButlerOptOutNodeProperty && ((ChaosButlerOptOutNodeProperty) property)
                    .isOptOut() ? property : null;
        }

        @Override
        public boolean isApplicableAsGlobal() {
            return false;
        }
    }

}
