package com.sbpl.OPD.repository;

import com.sbpl.OPD.enums.InvoiceStatus;
import com.sbpl.OPD.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing invoice data.
 * Provides database operations for invoice records.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Page<Invoice> findByAppointmentId(Long appointmentId, Pageable pageable);
    Page<Invoice> findByPatientId(Long patientId, Pageable pageable);
    Page<Invoice> findByGeneratedById(Long generatedById, Pageable pageable);
    List<Invoice> findByAppointmentId(Long appointmentId);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    Page<Invoice> findByCompanyAndBranch(Long companyId, Long branchId, Pageable pageable);
}