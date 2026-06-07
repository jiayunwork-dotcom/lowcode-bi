package com.lowcode.bi.common.enums;

import java.time.Duration;

public enum AlertCheckInterval {
    EVERY_5_MINUTES("EVERY_5_MINUTES", Duration.ofMinutes(5), "0 */5 * * * ?"),
    EVERY_15_MINUTES("EVERY_15_MINUTES", Duration.ofMinutes(15), "0 */15 * * * ?"),
    EVERY_30_MINUTES("EVERY_30_MINUTES", Duration.ofMinutes(30), "0 */30 * * * ?"),
    EVERY_HOUR("EVERY_HOUR", Duration.ofHours(1), "0 0 * * * ?"),
    EVERY_6_HOURS("EVERY_6_HOURS", Duration.ofHours(6), "0 0 */6 * * ?"),
    EVERY_12_HOURS("EVERY_12_HOURS", Duration.ofHours(12), "0 0 */12 * * ?"),
    EVERY_DAY("EVERY_DAY", Duration.ofDays(1), "0 0 0 * * ?");

    private final String value;
    private final Duration duration;
    private final String cronExpression;

    AlertCheckInterval(String value, Duration duration, String cronExpression) {
        this.value = value;
        this.duration = duration;
        this.cronExpression = cronExpression;
    }

    public String getValue() {
        return value;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}
