package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * DTO for creating week template schedules.
 * 
 * This DTO allows creating identical schedules for all working days of a week
 * with a single configuration setup. Perfect for "one doctor, entire week, one heat" scenarios.
 * 
 * @author HMS Team
 */
@Getter
@Setter
public class WeekTemplateDTO {
    
    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be a positive number")
    private Long doctorId;

    private Long branchId;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    private Set<String> breakTimes; // Format: "HH:mm-HH:mm"
    
    private LocalTime lunchStartTime;
    
    private LocalTime lunchEndTime;
    
    private Integer maxDailyAppointments = 20;
    
    private Integer appointmentDurationMinutes = 30;
    
    private Boolean isAvailable = true;
    
    private ScheduleType scheduleType = ScheduleType.REGULAR;
    
    private String specialNote;
    
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean monday = true;
    private Boolean tuesday = true;
    private Boolean wednesday = true;
    private Boolean thursday = true;
    private Boolean friday = true;
    private Boolean saturday = false;
    private Boolean sunday = false;
}