package xyz.phanta.sor.api.util;

public class ThreadThrottler {

    private static final int SAMPLE_COUNT = 512;

    private static final ThreadLocal<ThreadThrottler> perThread = new ThreadLocal<>();

    private long getAverageExecutionTime() {
        long sum = 0;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            sum += samples[i];
        }
        return sum / SAMPLE_COUNT;
    }

    private final long[] samples = new long[SAMPLE_COUNT];
    private int sampleIndex = 0;
    private long lastSample = -1L;

    private void throttle0(long periodNanos) {
        long now = System.nanoTime();
        long sleepTime = periodNanos - getAverageExecutionTime();
        if (lastSample != -1L) {
            samples[sampleIndex] = now - lastSample;
            sampleIndex = (sampleIndex + 1) % SAMPLE_COUNT;
        }
        if (sleepTime > 0) {
            int remainder = (int)(sleepTime % 1000000);
            try {
                Thread.sleep((sleepTime - remainder) / 1000000, remainder);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        lastSample = now;
    }

    public static void throttleMillis(long periodMillis) {
        throttleNanos(periodMillis * 1000000);
    }

    public static void throttleNanos(long periodNanos) {
        ThreadThrottler throttler = perThread.get();
        if (throttler == null) {
            throttler = new ThreadThrottler();
            perThread.set(throttler);
        }
        throttler.throttle0(periodNanos);
    }

}
