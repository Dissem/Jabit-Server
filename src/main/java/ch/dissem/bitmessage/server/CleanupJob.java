package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.BitmessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

/**
 * Created by chrigu on 04.10.15.
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
