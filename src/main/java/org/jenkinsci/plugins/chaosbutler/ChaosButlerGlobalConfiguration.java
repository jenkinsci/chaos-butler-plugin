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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.model.queue.WorkUnit;
import hudson.util.ListBoxModel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
public class ChaosButlerGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(ChaosButlerGlobalConfiguration.class.getName());

    private transient Random entropy = new Random();
    private long interval;
    private long nextWake;
    private long nextStart;
    private transient List<Map.Entry<Date, Node>> recentVictims = new ArrayList<>();

    public ChaosButlerGlobalConfiguration() {
        load();
    }

    public static ChaosButlerGlobalConfiguration get() {
        return Jenkins.getActiveInstance().getInjector().getInstance(ChaosButlerGlobalConfiguration.class);
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        if (interval < 0L) {
            interval = 0L;
        }
        if (this.interval != interval) {
            synchronized (this) {
                if (this.interval != interval) {
                    this.interval = interval;
                    if (interval > 0L) {
                        long now = System.currentTimeMillis();
                        nextWake = now + entropy.nextLong() % this.interval;
                        nextStart = now + this.interval;
                    } else {
                        nextWake = 0L;
                    }
                    save();
                }
            }
        }
    }

    public long getNextWake() {
        return nextWake;
    }

    public Map<Date, Node> getRecentVictims() {
        Map<Date, Node> result = new TreeMap<>();
        for (Map.Entry<Date, Node> entry : recentVictims) {
            result.put((Date) entry.getKey().clone(), entry.getValue());
        }
        return result;
    }

    public void wake(@CheckForNull TaskListener listener) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            if (interval == 0 || nextWake > now) {
                return;
            }
            nextWake = nextStart + entropy.nextLong() % this.interval;
            nextStart = Math.max(nextStart, now) + interval;
            save();
        }
        LOGGER.log(Level.INFO, "The Chaos Butler is looking for a victim...");
        if (listener != null) {
            listener.getLogger().printf("[%tc] The Chaos Butler is looking for a victim...%n", new Date());
        }
        List<Node> candidates = new ArrayList<>();
        candidates.add(Jenkins.getActiveInstance());
        candidates.addAll(Jenkins.getActiveInstance().getNodes());
        for (Iterator<Node> iterator = candidates.iterator(); iterator.hasNext(); ) {
            Node n = iterator.next();
            ChaosButlerOptOutNodeProperty nodeOptOpt =
                    n.getNodeProperties().get(ChaosButlerOptOutNodeProperty.class);
            if (nodeOptOpt != null && nodeOptOpt.isOptOut()) {
                // node is opt-out, ignore it
                iterator.remove();
            } else {
                Computer computer = n.toComputer();
                if (computer == null || computer.isOffline()) {
                    // node is off-line already, ignore it
                    iterator.remove();
                    continue;
                }
                for (Executor e : computer.getExecutors()) {
                    WorkUnit workUnit = e.getCurrentWorkUnit();
                    if (workUnit != null) {
                        if (workUnit.context.task instanceof Job) {
                            Job<?, ?> task = (Job<?, ?>) workUnit.context.task;
                            ChaosButlerOptOutJobProperty jobOptOut =
                                    task.getProperty(ChaosButlerOptOutJobProperty.class);
                            if (jobOptOut != null && jobOptOut.isOptOut()) {
                                // node is running a job that is opt-out, ignore it
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            LOGGER.log(Level.INFO, "The Chaos Butler cannot find a victim!");
            if (listener != null) {
                listener.getLogger().printf("[%tc] The Chaos Butler cannot find a victim!%n", new Date());
            }
            return;
        }
        int index = entropy.nextInt(candidates.size());
        Node victim = candidates.get(index);
        String displayName = victim.getDisplayName();
        LOGGER.log(Level.INFO, "The Chaos Butler has selected {0} as a victim...", displayName);
        if (listener != null) {
            listener.getLogger().printf("[%tc] The Chaos Butler has selected %s as a victim...%n", new Date(),
                    displayName);
        }
        Computer computer = victim.toComputer();
        if (victim instanceof Slave) {
            computer.disconnect(new ChaosButlerOfflineCause());
        } else if (computer != null){
            // cannot disconnect on master, so simulate by aborting all running jobs
            for (Executor e : computer.getExecutors()) {
                if (e.getCurrentWorkUnit() != null) {
                    e.interrupt(Result.ABORTED, new ChaosButlerInterruptionCause());
                }
            }
        }
        recentVictims.add(new AbstractMap.SimpleImmutableEntry<Date, Node>(new Date(), victim));
        if (recentVictims.size() > 10) {
            recentVictims.remove(0);
        }
        LOGGER.log(Level.INFO, "The Chaos Butler has killed {0}. Chaos reigns once more!", displayName);
        if (listener != null) {
            listener.getLogger()
                    .printf("[%tc] The Chaos Butler has killed %s. Chaos reigns once more!%n", new Date(), displayName);
        }
    }

    @Override
    public String getDisplayName() {
        return Messages.ChaosButlerGlobalConfiguration_DisplayName();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        long i = 0;
        try {
            i = Integer.parseInt(json.getString("interval"));
        } catch (NumberFormatException e) {
            // fall through
        }
        try {
            setInterval(i);
            return true;
        } catch (RuntimeException e) {
            throw new FormException(e, "interval");
        }
    }

    public ListBoxModel doFillIntervalItems() {
        ListBoxModel result = new ListBoxModel();
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_Off(), "0");
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_1m(), Long.toString(TimeUnit.MINUTES.toMillis(1)));
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_15m(), Long.toString(TimeUnit.MINUTES.toMillis(15)));
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_1h(), Long.toString(TimeUnit.HOURS.toMillis(1)));
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_8h(), Long.toString(TimeUnit.HOURS.toMillis(8)));
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_1d(), Long.toString(TimeUnit.DAYS.toMillis(1)));
        result.add(Messages.ChaosButlerGlobalConfiguration_Interval_7d(), Long.toString(TimeUnit.DAYS.toMillis(7)));
        return result;
    }

}
