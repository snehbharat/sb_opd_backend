package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.model.Bill;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.InvoicePdfService;
import com.sbpl.OPD.utils.S3BucketStorageUtility;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Implementation of invoice PDF service operations.
 * Handles uploading invoice PDFs to AWS S3 bucket.
 *
 * @author Rahul Kumar
 */
@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private static final Logger logger = LoggerFactory.getLogger(InvoicePdfServiceImpl.class);

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private S3BucketStorageUtility s3BucketStorageUtility;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${invoice.email.from:noreply@hms.com}")
    private String emailFrom;

    @Value("${invoice.email.subject.prefix:Invoice - }")
    private String emailSubjectPrefix;

    @Override
    public ResponseEntity<?> uploadInvoicePdf(MultipartFile file, String billNo) {
        
        logger.info("Invoice PDF upload request received [fileName={}, billNo={}]",
                file != null ? file.getOriginalFilename() : "null", billNo);

        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "File is empty");
            }

            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            byte[] fileBytes = file.getBytes();

            // Validate content type
            if (contentType == null || !contentType.equals("application/pdf")) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "File size must be less than 10MB");
            }

            logger.info("Invoice PDF uploaded successfully to S3 [billNo={}]", billNo);

            // Send email with PDF attachment to customer
            sendInvoiceEmailWithAttachment(billNo, fileBytes, originalFileName);

            return baseResponse.successResponse(
                    "Invoice PDF uploaded successfully and email sent to customer"
            );

        } catch (Exception e) {
            logger.error("Error uploading invoice PDF [billNo={}]", billNo, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to upload invoice PDF: " + e.getMessage()
            );
        }
    }

    /**
     * Send invoice email with PDF attachment to the customer associated with the bill
     *
     * @param billNo Bill number to find associated customer
     * @param pdfContent Byte array of PDF content
     * @param fileName Original file name
     */
    private void sendInvoiceEmailWithAttachment(String billNo, byte[] pdfContent, String fileName) {
        try {
            // Find bill by bill number
            Bill bill = findBillByBillNumber(billNo);
            
            if (bill == null) {
                logger.warn("Bill not found for bill number: {}. Skipping email.", billNo);
                return;
            }

            Customer customer = bill.getPatient();
            
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                logger.warn("Customer or customer email not found for bill number: {}. Skipping email.", billNo);
                return;
            }

            // Create MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            // Set email recipients
            helper.setTo(customer.getEmail());
            helper.setFrom(emailFrom);
            helper.setSubject(emailSubjectPrefix + "Bill No: " + billNo);

            // Build email body
            String emailBody = buildInvoiceEmailBody(customer, bill, billNo);
            helper.setText(emailBody, true);

            // Attach PDF
            ByteArrayResource pdfAttachment = new ByteArrayResource(pdfContent);
            helper.addAttachment(fileName != null ? fileName : "invoice-" + billNo + ".pdf", pdfAttachment);

            // Send email
            mailSender.send(mimeMessage);

            logger.info("Invoice email sent successfully to customer {} ({}) for bill no: {}",
                    customer.getFirstName() + " " + customer.getLastName(),
                    customer.getEmail(),
                    billNo);

        } catch (MessagingException e) {
            logger.error("Error sending invoice email with attachment for bill no {}: {}", billNo, e.getMessage(), e);
            // Don't throw exception - email failure should not fail the entire operation
        } catch (Exception e) {
            logger.error("Unexpected error while sending invoice email for bill no {}: {}", billNo, e.getMessage(), e);
        }
    }

    /**
     * Find bill by bill number
     */
    private Bill findBillByBillNumber(String billNo) {
        try {
            return billRepository.findByBillNumber(billNo).orElse(null);
        } catch (Exception e) {
            logger.error("Error finding bill by bill number {}: {}", billNo, e.getMessage());
            return null;
        }
    }

    /**
     * Build HTML email body for invoice
     */
    private String buildInvoiceEmailBody(Customer customer, Bill bill, String billNo) {
        String customerName = customer.getFirstName() + " " + customer.getLastName();
        
        return """
                <html>
                <body style="font-family: Arial, sans-serif; background-color:#f4f6f8; padding:20px;">
                    <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                        
                        <!-- Header -->
                        <div style="text-align:center; margin-bottom:30px;">
                            <h2 style="color:#1e3c72; margin:0;">Invoice</h2>
                            <p style="color:#666; margin:5px 0;">Bill Number: <strong>%s</strong></p>
                        </div>
                        
                        <!-- Greeting -->
                        <p style="font-size:16px; color:#333;">Dear %s,</p>
                        
                        <p style="font-size:14px; color:#555; line-height:1.6;">
                            Thank you for choosing our services. Please find attached the invoice for your recent transaction.
                        </p>
                        
                        <!-- Invoice Details -->
                        <div style="background:#f9fafb; padding:20px; border-radius:8px; margin:20px 0;">
                            <h3 style="color:#1e3c72; margin-top:0;">Invoice Details</h3>
                            <table style="width:100%%; border-collapse:collapse;">
                                <tr>
                                    <td style="padding:10px 0; color:#555; font-weight:600;">Bill Number:</td>
                                    <td style="padding:10px 0; color:#333; text-align:right;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding:10px 0; color:#555; font-weight:600;">Total Amount:</td>
                                    <td style="padding:10px 0; color:#333; text-align:right;">₹%.2f</td>
                                </tr>
                                <tr>
                                    <td style="padding:10px 0; color:#555; font-weight:600;">Paid Amount:</td>
                                    <td style="padding:10px 0; color:#333; text-align:right;">₹%.2f</td>
                                </tr>
                                <tr>
                                    <td style="padding:10px 0; color:#555; font-weight:600;">Balance Due:</td>
                                    <td style="padding:10px 0; color:#333; text-align:right;">₹%.2f</td>
                                </tr>
                                <tr>
                                    <td style="padding:10px 0; color:#555; font-weight:600;">Status:</td>
                                    <td style="padding:10px 0; color:#333; text-align:right;">%s</td>
                                </tr>
                            </table>
                        </div>
                        
                        <!-- Payment Info -->
                        <div style="background:#fff3cd; padding:15px; border-radius:8px; border-left:4px solid #ffc107; margin:20px 0;">
                            <p style="margin:0; color:#856404; font-size:14px;">
                                <strong>Payment Reminder:</strong> If you have any outstanding balance, please ensure timely payment to avoid any inconvenience.
                            </p>
                        </div>
                        
                        <!-- Contact Info -->
                        <div style="margin-top:30px; padding-top:20px; border-top:1px solid #eee; text-align:center; color:#666; font-size:14px;">
                            <p style="margin:5px 0;">If you have any questions regarding this invoice, please contact our billing department.</p>
                            <p style="margin:5px 0;">Email: <a href="mailto:billing@hms.com" style="color:#1e3c72;">billing@hms.com</a></p>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background:#f5f7fa; padding:18px; text-align:center; font-size:12px; color:#777; margin-top:20px; border-radius:8px;">
                            <p style="margin:0;">This is an automated email from Healthcare Management System.</p>
                            <p style="margin:5px 0;">Please do not reply directly to this email.</p>
                        </div>
                        
                    </div>
                </body>
                </html>
                """.formatted(
                billNo,
                customerName,
                billNo,
                bill.getTotalAmount() != null ? bill.getTotalAmount() : java.math.BigDecimal.ZERO,
                bill.getPaidAmount() != null ? bill.getPaidAmount() : java.math.BigDecimal.ZERO,
                bill.getBalanceAmount() != null ? bill.getBalanceAmount() : java.math.BigDecimal.ZERO,
                bill.getStatus() != null ? bill.getStatus().toString() : "N/A"
        );
    }
}
