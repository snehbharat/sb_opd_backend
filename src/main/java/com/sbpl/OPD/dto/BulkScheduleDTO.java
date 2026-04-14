package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.enums.ScheduleType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * DTO for bulk schedule creation operations.
 * 
 * This DTO allows creating multiple schedules for a doctor with common properties
 * that can be overridden for specific days if needed.
 * 
 * @author HMS Team
 */
@Getter
@Setter
public class BulkScheduleDTO {
    
    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be a positive number")
    private Long doctorId;

    private Long branchId;

    // Common schedule properties (can be overridden per day)
    private LocalTime commonStartTime;
    private LocalTime commonEndTime;
    private Set<String> commonBreakTimes; // Format: "HH:mm-HH:mm"
    private LocalTime commonLunchStartTime;
    private LocalTime commonLunchEndTime;
    private Integer commonMaxDailyAppointments = 20;
    private Integer commonAppointmentDurationMinutes = 30;
    private Boolean commonIsAvailable = true;
    private ScheduleType commonScheduleType = ScheduleType.REGULAR;
    private String commonSpecialNote;
    private ScheduleStatus commonStatus = ScheduleStatus.ACTIVE;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    // Specific day configurations (optional - if not provided, common settings will be used)
    private List<DayScheduleConfig> dayConfigs;

    /**
     * Configuration for specific days of the week
     */
    @Getter
    @Setter
    public static class DayScheduleConfig {
        @NotNull(message = "Day of week is required")
        private DayOfWeek dayOfWeek;
        
        private LocalTime startTime;
        private LocalTime endTime;
        private Set<String> breakTimes;
        private LocalTime lunchStartTime;
        private LocalTime lunchEndTime;
        private Integer maxDailyAppointments;
        private Integer appointmentDurationMinutes;
        private Boolean isAvailable;
        private ScheduleType scheduleType;
        private String specialNote;
        private ScheduleStatus status;
    }
}