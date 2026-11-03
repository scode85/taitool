package vn.chamanh;
/** Virtual monotonic time: pending waits/countdowns freeze while paused or locked. */
public final class RunClock {
 private long excluded,pauseAt;private boolean paused;
 public void setPaused(boolean value,long real){if(value==paused)return;if(value)pauseAt=real;else excluded+=Math.max(0,real-pauseAt);paused=value;}
 public long now(long real){return (paused?pauseAt:real)-excluded;}
}
