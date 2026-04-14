package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.EnumDTO;
import com.sbpl.OPD.service.EnumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enums")
public class EnumController {

    @Autowired
    private EnumService enumService;

    @GetMapping
    public ResponseEntity<List<EnumDTO>> getAllEnums() {
        List<EnumDTO> enums = enumService.getAllEnums();
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/package/{packageName}")
    public ResponseEntity<List<EnumDTO>> getEnumsByPackage(@PathVariable String packageName) {
        List<EnumDTO> enums = enumService.getEnumsByPackage(packageName);
        return ResponseEntity.ok(enums);
    }
}