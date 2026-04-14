package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.InvoiceDTO;
import com.sbpl.OPD.enums.InvoiceStatus;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for invoice operations.
 * Handles invoice generation, retrieval, and management.
 */
public interface InvoiceService {
    ResponseEntity<?> createInvoice(InvoiceDTO invoiceDTO);
    ResponseEntity<?> getInvoiceById(Long id);
    ResponseEntity<?> getInvoicesByAppointment(Long appointmentId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> getInvoicesByPatient(Long patientId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> getInvoicesByGeneratedBy(Long generatedById, Integer pageNo, Integer pageSize);
    ResponseEntity<?> updateInvoice(Long id, InvoiceDTO invoiceDTO);
    ResponseEntity<?> deleteInvoice(Long id);
    ResponseEntity<?> updateInvoiceStatus(Long id, InvoiceStatus status);
    
    // Employee self-service methods
    ResponseEntity<?> createMyInvoice(InvoiceDTO invoiceDTO);
    ResponseEntity<?> getMyInvoices(Integer pageNo, Integer pageSize);
    ResponseEntity<?> getMyInvoicesByAppointment(Long appointmentId);
    ResponseEntity<?> raiseInvoiceFromAppointment(Long appointmentId);
}