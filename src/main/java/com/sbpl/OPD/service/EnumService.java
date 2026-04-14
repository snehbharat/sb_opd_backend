package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.EnumDTO;

import java.util.List;

public interface EnumService {
    List<EnumDTO> getAllEnums();
    List<EnumDTO> getEnumsByPackage(String packageName);
}