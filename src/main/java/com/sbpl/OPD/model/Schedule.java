package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.enums.ScheduleType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Represents a doctor's schedule in the Healthcare Management System (HMS).
 * 
 * This entity manages doctor availability, working hours, breaks, and schedule types
 * to facilitate appointment booking within defined time slots.
 * 
 * Supports multiple schedule types (REGULAR, SPECIAL, ON_LEAVE) and allows
 * for flexible working hour configurations per day of week.
 * 
 * @author Rahul Kumar
 */
@Entity
@Table(name = "schedules",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_schedule_doctor_day", columnList = "doctor_id, day_of_week"),
        @Index(name = "idx_schedule_start_date", columnList = "start_date"),
        @Index(name = "idx_schedule_end_date", columnList = "end_date"),
        @Index(name = "idx_schedule_status", columnList = "status")
    })
@Getter
@Setter
public class Schedule extends BaseEntity {

    @NotNull(message = "Doctor is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @NotNull(message = "Day of week is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @ElementCollection
    @CollectionTable(name = "schedule_breaks", joinColumns = @JoinColumn(name = "schedule_id"), 
                     schema = "sb_opd")
    @Column(name = "break_time")
    private Set<String> breakTimes; // Format: "HH:mm-HH:mm"

    @Column(name = "lunch_start_time")
    private LocalTime lunchStartTime;

    @Column(name = "lunch_end_time")
    private LocalTime lunchEndTime;

    @Column(name = "max_daily_appointments")
    private Integer maxDailyAppointments = 20;

    @Column(name = "appointment_duration_minutes")
    private Integer appointmentDurationMinutes = 30;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    public boolean isAvailable() {
        return isAvailable != null ? isAvailable : false;
    }

    @Column(name = "schedule_type")
    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType = ScheduleType.REGULAR;

    @Column(name = "special_note", length = 500)
    private String specialNote;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    public boolean isWithinWorkingHours(LocalTime time) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    public boolean hasBreakAtTime(LocalTime time) {
        if (breakTimes == null || breakTimes.isEmpty()) {
            return false;
        }
        
        for (String breakTime : breakTimes) {
            String[] times = breakTime.split("-");
            if (times.length == 2) {
                LocalTime breakStart = LocalTime.parse(times[0]);
                LocalTime breakEnd = LocalTime.parse(times[1]);
                
                if ((time.isAfter(breakStart) && time.isBefore(breakEnd)) ||
                    time.equals(breakStart) || time.equals(breakEnd)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOnLunchBreak(LocalTime time) {
        if (lunchStartTime == null || lunchEndTime == null) {
            return false;
        }
        return time.isAfter(lunchStartTime) && time.isBefore(lunchEndTime);
    }
}