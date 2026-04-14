package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
public class ScheduleDTO {
    private Long id;
    
    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be a positive number")
    private Long doctorId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private Set<String> breakTimes; // Format: "HH:mm-HH:mm"

    private LocalTime lunchStartTime;

    private LocalTime lunchEndTime;

    private Integer maxDailyAppointments = 20;

    private Integer appointmentDurationMinutes = 30;

    private Boolean isAvailable = true;

    private ScheduleType scheduleType = ScheduleType.REGULAR;

    private String specialNote;

    private LocalDate startDate;

    private LocalDate endDate;

    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    private String doctorName;
    private Long branchId;
    private String branchName;
    private Long companyId;
    private String companyName;
}