package com.sbpl.OPD.dto.Doctor.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO to represent a doctor's weekly schedule availability.
 * Contains boolean flags for each day of the week indicating 
 * whether the doctor has an active schedule on that day.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleDaysDTO {
    
    /**
     * Whether the doctor has an active schedule on Monday
     */
    private Boolean monday;
    
    /**
     * Whether the doctor has an active schedule on Tuesday
     */
    private Boolean tuesday;
    
    /**
     * Whether the doctor has an active schedule on Wednesday
     */
    private Boolean wednesday;
    
    /**
     * Whether the doctor has an active schedule on Thursday
     */
    private Boolean thursday;
    
    /**
     * Whether the doctor has an active schedule on Friday
     */
    private Boolean friday;
    
    /**
     * Whether the doctor has an active schedule on Saturday
     */
    private Boolean saturday;
    
    /**
     * Whether the doctor has an active schedule on Sunday
     */
    private Boolean sunday;
}
