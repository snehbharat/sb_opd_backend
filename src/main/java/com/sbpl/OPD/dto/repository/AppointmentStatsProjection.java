package com.sbpl.OPD.dto.repository;

public interface AppointmentStatsProjection {

  Long getTotalAppointments();

  Long getCompleted();

  Long getPendingRequested();

  Long getNoShow();

}
