/*
 * Copyright 2015 Christian Basler
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

package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.BitmessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

/**
 * @author Christian Basler
 */
public class CleanupJob extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupJob.class);
    private final BitmessageContext ctx;

    public CleanupJob(BitmessageContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        try {
            ctx.cleanup();
        } catch (Throwable t) {
            LOG.error("Problem while cleaning inventory", t);
        }
    }
}
