package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.InvoiceDTO;
import com.sbpl.OPD.enums.InvoiceStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Invoice;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.repository.InvoiceRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Implementation of invoice service operations.
 * Handles invoice generation, retrieval, and management.
 */
@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BaseResponse baseResponse;

    private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    @Override
    public ResponseEntity<?> createInvoice(InvoiceDTO invoiceDTO) {
        logger.info("Creating invoice for appointment [appointmentId={}]", invoiceDTO.getAppointmentId());

        try {
            // Validate appointment exists
            Appointment appointment = appointmentRepository.findById(invoiceDTO.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Validate patient exists
            Customer patient = customerRepository.findById(invoiceDTO.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            // Validate generator exists
            User generator = userRepository.findById(invoiceDTO.getGeneratedById())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice generator not found"));

            // Validate company if provided
            CompanyProfile company = null;
            if (invoiceDTO.getCompanyId() != null) {
                company = companyProfileRepository.findById(invoiceDTO.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
            }

            // Validate branch if provided
            Branch branch = null;
            if (invoiceDTO.getBranchId() != null) {
                branch = branchRepository.findById(invoiceDTO.getBranchId())
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            }

            // Create invoice record
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setAppointment(appointment);
            invoice.setPatient(patient);
            invoice.setGeneratedBy(generator);
            invoice.setCompany(company);
            invoice.setBranch(branch);
            invoice.setTotalAmount(invoiceDTO.getTotalAmount() != null ? invoiceDTO.getTotalAmount() : BigDecimal.ZERO);
            invoice.setTaxAmount(invoiceDTO.getTaxAmount() != null ? invoiceDTO.getTaxAmount() : BigDecimal.ZERO);
            invoice.setDiscountAmount(invoiceDTO.getDiscountAmount() != null ? invoiceDTO.getDiscountAmount() : BigDecimal.ZERO);
            
            // Calculate net amount
            BigDecimal netAmount = invoice.getTotalAmount()
                    .add(invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO)
                    .subtract(invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO);
            invoice.setNetAmount(netAmount);
            
            invoice.setStatus(invoiceDTO.getStatus() != null ? invoiceDTO.getStatus() : InvoiceStatus.DRAFT);
            invoice.setDueDate(invoiceDTO.getDueDate());
            invoice.setIssueDate(invoiceDTO.getIssueDate() != null ? invoiceDTO.getIssueDate() : LocalDateTime.now());
            invoice.setNotes(invoiceDTO.getNotes());

            Invoice savedInvoice = invoiceRepository.save(invoice);

//            logger.info("Invoice created successfully [invoiceId={}, invoiceNumber={}]",
//                       savedInvoice.getId(), savedInvoice.getInvoiceNumber());
            return baseResponse.successResponse("Invoice created successfully", convertToDTO(savedInvoice));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating invoice", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong while creating invoice. Please try again later.");
        }
    }

    @Override
    public ResponseEntity<?> getInvoiceById(Long id) {
        logger.info("Request received to fetch invoice [invoiceId={}]", id);

        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            logger.info("Invoice fetched successfully [invoiceId={}]", id);
            return baseResponse.successResponse("Invoice fetched successfully", convertToDTO(invoice));

        } catch (IllegalArgumentException e) {
            logger.warn("Invoice not found [invoiceId={}]", id);
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching invoice [invoiceId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch invoice at the moment");
        }
    }

    @Override
    public ResponseEntity<?> getInvoicesByAppointment(Long appointmentId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching invoices for appointment [appointmentId={}, pageNo={}, pageSize={}]", 
                   appointmentId, pageNo, pageSize);

        try {
            PageRequest pageRequest = com.sbpl.OPD.utils.DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<Invoice> page = invoiceRepository.findByAppointmentId(appointmentId, pageRequest);

            var dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = com.sbpl.OPD.utils.DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse("Invoices fetched successfully", response);

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching invoices for appointmentId={}", appointmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch invoices");
        }
    }

    @Override
    public ResponseEntity<?> getInvoicesByPatient(Long patientId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching invoices for patient [patientId={}, pageNo={}, pageSize={}]", 
                   patientId, pageNo, pageSize);

        try {
            PageRequest pageRequest = com.sbpl.OPD.utils.DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<Invoice> page = invoiceRepository.findByPatientId(patientId, pageRequest);

            var dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = com.sbpl.OPD.utils.DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse("Patient invoices fetched successfully", response);

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching invoices for patientId={}", patientId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch invoices");
        }
    }

    @Override
    public ResponseEntity<?> getInvoicesByGeneratedBy(Long generatedById, Integer pageNo, Integer pageSize) {
        logger.info("Fetching invoices generated by user [generatedById={}, pageNo={}, pageSize={}]", 
                   generatedById, pageNo, pageSize);

        try {
            PageRequest pageRequest = com.sbpl.OPD.utils.DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<Invoice> page = invoiceRepository.findByGeneratedById(generatedById, pageRequest);

            var dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = com.sbpl.OPD.utils.DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse("User-generated invoices fetched successfully", response);

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching invoices for generatedById={}", generatedById, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch invoices");
        }
    }

    @Override
    public ResponseEntity<?> updateInvoice(Long id, InvoiceDTO invoiceDTO) {
        logger.info("Updating invoice [invoiceId={}]", id);

        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            // Update fields if provided
            if (invoiceDTO.getTotalAmount() != null) invoice.setTotalAmount(invoiceDTO.getTotalAmount());
            if (invoiceDTO.getTaxAmount() != null) invoice.setTaxAmount(invoiceDTO.getTaxAmount());
            if (invoiceDTO.getDiscountAmount() != null) invoice.setDiscountAmount(invoiceDTO.getDiscountAmount());
            if (invoiceDTO.getStatus() != null) invoice.setStatus(invoiceDTO.getStatus());
            if (invoiceDTO.getDueDate() != null) invoice.setDueDate(invoiceDTO.getDueDate());
            if (invoiceDTO.getIssueDate() != null) invoice.setIssueDate(invoiceDTO.getIssueDate());
            if (invoiceDTO.getNotes() != null) invoice.setNotes(invoiceDTO.getNotes());

            // Recalculate net amount
            BigDecimal netAmount = invoice.getTotalAmount()
                    .add(invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO)
                    .subtract(invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO);
            invoice.setNetAmount(netAmount);

//            invoice.setUpdatedAt(LocalDateTime.now());
            Invoice updatedInvoice = invoiceRepository.save(invoice);

            logger.info("Invoice updated successfully [invoiceId={}]", id);
            return baseResponse.successResponse("Invoice updated successfully", convertToDTO(updatedInvoice));

        } catch (IllegalArgumentException e) {
            logger.warn("Update failed: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating invoice [invoiceId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update invoice");
        }
    }

    @Override
    public ResponseEntity<?> deleteInvoice(Long id) {
        logger.info("Request received to delete invoice [invoiceId={}]", id);

        try {
            if (!invoiceRepository.existsById(id)) {
                logger.warn("Invoice not found for deletion [invoiceId={}]", id);
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Invoice not found");
            }

            invoiceRepository.deleteById(id);

            logger.info("Invoice deleted successfully [invoiceId={}]", id);
            return baseResponse.successResponse("Invoice deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting invoice [invoiceId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete invoice");
        }
    }

    @Override
    public ResponseEntity<?> updateInvoiceStatus(Long id, InvoiceStatus status) {
        logger.info("Updating invoice status [invoiceId={}, status={}]", id, status);

        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            invoice.setStatus(status);
//            invoice.setUpdatedAt(LocalDateTime.now());
            Invoice updatedInvoice = invoiceRepository.save(invoice);

            logger.info("Invoice status updated successfully [invoiceId={}, status={}]", id, status);
            return baseResponse.successResponse("Invoice status updated successfully", convertToDTO(updatedInvoice));

        } catch (IllegalArgumentException e) {
            logger.warn("Status update failed: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating invoice status [invoiceId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update invoice status");
        }
    }

    @Override
    public ResponseEntity<?> createMyInvoice(InvoiceDTO invoiceDTO) {
        User currentUser = com.sbpl.OPD.utils.DbUtill.getCurrentUser();
        invoiceDTO.setGeneratedById(currentUser.getId());
        return createInvoice(invoiceDTO);
    }

    @Override
    public ResponseEntity<?> getMyInvoices(Integer pageNo, Integer pageSize) {
        User currentUser = com.sbpl.OPD.utils.DbUtill.getCurrentUser();
        return getInvoicesByGeneratedBy(currentUser.getId(), pageNo, pageSize);
    }

    @Override
    public ResponseEntity<?> getMyInvoicesByAppointment(Long appointmentId) {
        User currentUser = com.sbpl.OPD.utils.DbUtill.getCurrentUser();
        return getInvoicesByAppointment(appointmentId, 0, 10);
    }

    @Override
    public ResponseEntity<?> raiseInvoiceFromAppointment(Long appointmentId) {
        logger.info("Raising invoice from appointment [appointmentId={}]", appointmentId);

        try {
            // Get appointment details
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Create invoice based on appointment
            InvoiceDTO invoiceDTO = new InvoiceDTO();
            invoiceDTO.setAppointmentId(appointmentId);
            invoiceDTO.setPatientId(appointment.getPatient().getId());
            invoiceDTO.setTotalAmount(BigDecimal.valueOf(100.00)); // Default amount, can be customized
            invoiceDTO.setStatus(InvoiceStatus.DRAFT);
            invoiceDTO.setNotes("Invoice raised from appointment #" + appointmentId);

            // Set company and branch from appointment if available
            if (appointment.getPatient().getCompany() != null) {
                invoiceDTO.setCompanyId(appointment.getPatient().getCompany().getId());
            }
            if (appointment.getPatient().getBranch() != null) {
                invoiceDTO.setBranchId(appointment.getPatient().getBranch().getId());
            }

            return createInvoice(invoiceDTO);

        } catch (IllegalArgumentException e) {
            logger.warn("Failed to raise invoice from appointment: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error raising invoice from appointment [appointmentId={}]", appointmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to raise invoice from appointment");
        }
    }

    private String generateInvoiceNumber() {
        // Generate a unique invoice number
        String prefix = "INV-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = (int) (invoiceRepository.count() + 1);
        return prefix + "-" + String.format("%05d", count);
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
//        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setAppointmentId(invoice.getAppointment().getId());
        dto.setPatientId(invoice.getPatient().getId());
        dto.setGeneratedById(invoice.getGeneratedBy().getId());
        dto.setCompanyId(invoice.getCompany() != null ? invoice.getCompany().getId() : null);
        dto.setBranchId(invoice.getBranch() != null ? invoice.getBranch().getId() : null);
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setDiscountAmount(invoice.getDiscountAmount());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setStatus(invoice.getStatus());
        dto.setDueDate(invoice.getDueDate());
        dto.setIssueDate(invoice.getIssueDate());
        dto.setNotes(invoice.getNotes());
//        dto.setCreatedAt(invoice.getCreatedAt());
//        dto.setUpdatedAt(invoice.getUpdatedAt());
        return dto;
    }
}