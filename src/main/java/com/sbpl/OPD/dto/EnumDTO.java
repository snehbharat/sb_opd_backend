package com.sbpl.OPD.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
public class EnumDTO {
    private String name;
    private String packageName;
    private List<String> values;

}