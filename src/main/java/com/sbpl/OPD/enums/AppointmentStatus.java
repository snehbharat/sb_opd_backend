package com.sbpl.OPD.enums;

import lombok.Getter;

@Getter
public enum AppointmentStatus {
    CONFIRMED(1),
    REQUESTED(2),
    RESCHEDULED(3),
    COMPLETED(4),
    CANCELLED(5),
    NO_SHOW(6);

    private final int priority;

    AppointmentStatus(int priority) {
        this.priority = priority;
    }

}