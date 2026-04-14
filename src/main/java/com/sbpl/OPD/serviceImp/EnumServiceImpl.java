package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.EnumDTO;
import com.sbpl.OPD.service.EnumService;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnumServiceImpl implements EnumService {

    @Override
    public List<EnumDTO> getAllEnums() {
        Set<Class<? extends Enum>> enumClasses = findAllEnumsInPackage("com.sbpl.OPD");
        return enumClasses.stream()
                .map(this::convertEnumToDTO)
                .sorted(Comparator.comparing(EnumDTO::getName))
                .collect(Collectors.toList());
    }

    @Override
    public List<EnumDTO> getEnumsByPackage(String packageName) {
        Set<Class<? extends Enum>> enumClasses = findAllEnumsInPackage(packageName);
        return enumClasses.stream()
                .map(this::convertEnumToDTO)
                .sorted(Comparator.comparing(EnumDTO::getName))
                .collect(Collectors.toList());
    }

    private Set<Class<? extends Enum>> findAllEnumsInPackage(String packageName) {
        try {
            Reflections reflections = new Reflections(packageName);
            return reflections.getSubTypesOf(Enum.class);
        } catch (Exception e) {
            // Fallback to manually registered enums if reflection fails
            return getManuallyRegisteredEnums();
        }
    }

    private Set<Class<? extends Enum>> getManuallyRegisteredEnums() {
        Set<Class<? extends Enum>> enumClasses = new HashSet<>();
        // Add all known enums here
        enumClasses.add(com.sbpl.OPD.enums.AppointmentStatus.class);
        enumClasses.add(com.sbpl.OPD.enums.BillStatus.class);
        enumClasses.add(com.sbpl.OPD.enums.InvoiceStatus.class);
        enumClasses.add(com.sbpl.OPD.enums.ScheduleStatus.class);
        enumClasses.add(com.sbpl.OPD.enums.ScheduleType.class);
        enumClasses.add(com.sbpl.OPD.Auth.enums.UserRole.class);
        return enumClasses;
    }

    private EnumDTO convertEnumToDTO(Class<? extends Enum> enumClass) {
        String[] values = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toArray(String[]::new);

        return new EnumDTO(
                enumClass.getSimpleName(),
                enumClass.getPackage().getName(),
                Arrays.asList(values)
        );
    }
}