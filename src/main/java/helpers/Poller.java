package helpers;

import java.util.Timer;
import java.util.TimerTask;




public class Poller extends  TimerTask {
    private IPollerTask task;
    private static Poller instance = null;
    private long intervalInMillis = 0;
    private long jitterInMillis = 0;

    private boolean isStopped = false;

    private Poller(IPollerTask task, long intervalInMillis, long jitterInMillis) {
        this.task = task;
        this.intervalInMillis = intervalInMillis;
        this.jitterInMillis = jitterInMillis;
    }

    public void stop() {
        this.isStopped = true;
    }

    @Override
    public void run() {
        try {
            boolean shouldRunNextTime = this.task.run();
            if (shouldRunNextTime && !this.isStopped) {
                Timer timer = new Timer();
                timer.schedule(
                        Poller.getInstance(),
                        this.intervalInMillis - ((long) Math.random() * this.jitterInMillis)
                );
            }
        } catch (Exception e) {
            // do not run next time;
        }
    }


    public static Poller init(IPollerTask task, long interval, long jitter) {
        if (Poller.instance == null) {
            Poller.instance = new Poller(task, interval, jitter);
        }

        return Poller.instance;
    }

    public static Poller getInstance() {
        return Poller.instance;
    }

}
