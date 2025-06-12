package com.best.deskclock.timer;

public record DurationObject(int hour, int minute, int second) {
    public long toMillis() {
        return (((hour * 60L) + minute) * 60 + second) * 1000;
    }
}
