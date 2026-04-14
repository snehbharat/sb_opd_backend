package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for invoice PDF operations.
 * Handles invoice PDF upload to S3 storage.
 *
 * @author Rahul Kumar
 */
public interface InvoicePdfService {
    /**
     * Upload invoice PDF from MultipartFile
     * 
     * @param file MultipartFile containing the PDF
     * @param billNo Bill number for reference
     * @return ResponseEntity with upload result
     */

    ResponseEntity<?> uploadInvoicePdf(MultipartFile file, String billNo);
}
