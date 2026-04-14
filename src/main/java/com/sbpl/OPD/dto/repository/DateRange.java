package com.sbpl.OPD.dto.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DateRange {

  LocalDateTime start;
  LocalDateTime end;

  public boolean isAllTime() {
    return start == null || end == null;
  }
}
