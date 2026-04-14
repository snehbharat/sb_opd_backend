package com.sbpl.OPD.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvParserUtil {

    public static List<String[]> parseCsvFile(MultipartFile file) throws IOException, CsvValidationException {
        List<String[]> records = new ArrayList<>();
        
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] values;
            boolean isFirstRow = true;
            
            while ((values = csvReader.readNext()) != null) {
                // Skip header row
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // Only add non-empty rows
                if (values.length > 0 && Arrays.stream(values).anyMatch(s -> s != null && !s.trim().isEmpty())) {
                    records.add(values);
                }
            }
        }
        
        return records;
    }
}