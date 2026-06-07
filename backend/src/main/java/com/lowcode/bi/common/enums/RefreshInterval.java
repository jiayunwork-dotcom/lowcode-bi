package com.lowcode.bi.common.enums;

import lombok.Getter;

@Getter
public enum RefreshInterval {
    OFF(0),
    MANUAL(-1),
    THIRTY_SECONDS(30),
    ONE_MINUTE(60),
    FIVE_MINUTES(300),
    TEN_MINUTES(600),
    THIRTY_MINUTES(1800),
    ONE_HOUR(3600),
    ONE_DAY(86400);

    private final int seconds;

    RefreshInterval(int seconds) {
        this.seconds = seconds;
    }
}
