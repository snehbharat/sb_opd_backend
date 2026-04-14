package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.service.AppointmentNotificationService;
import com.sbpl.OPD.service.AppointmentReminderService;
import com.sbpl.OPD.service.Msg91SmsService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementation of appointment notification service
 */
@Service
public class AppointmentNotificationServiceImpl implements AppointmentNotificationService {

    @Autowired
    private AppointmentReminderService appointmentReminderService;

    @Autowired
    private Msg91SmsService msg91SmsService;

    @Value("${spring.mail.enable}")
    private boolean mailEnabled;

    private static final Logger logger = LoggerFactory.getLogger(AppointmentNotificationServiceImpl.class);

    @Override
    public void sendCreationNotification(Appointment appointment) {
        try {
            Customer patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();

            if (patient == null) {
                logger.warn("Appointment {} has no patient assigned", appointment.getId());
                return;
            }

            // Send email creation notification if patient has an email
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                sendEmailCreationNotification(appointment, patient, doctor);
            }

            // Send SMS creation notification if patient has a phone number and SMS service is enabled
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                sendSmsCreationNotification(appointment, patient, doctor);
            } else {
                logger.info("No phone number available for patient {}, skipping SMS creation notification",
                        patient.getFirstName() + " " + patient.getLastName());
            }
        } catch (Exception e) {
            logger.error("Error sending creation notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendConfirmationNotification(Appointment appointment) {
        try {
            Customer patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();

            if (patient == null) {
                logger.warn("Appointment {} has no patient assigned", appointment.getId());
                return;
            }

            // Send email confirmation notification if patient has an email
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                sendEmailConfirmationNotification(appointment, patient, doctor);
            }

            // Send SMS confirmation notification if patient has a phone number and SMS service is enabled
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                sendSmsConfirmationNotification(appointment, patient, doctor);
            } else {
                logger.info("No phone number available for patient {}, skipping SMS confirmation notification",
                        patient.getFirstName() + " " + patient.getLastName());
            }
        } catch (Exception e) {
            logger.error("Error sending confirmation notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendRescheduleNotification(Appointment appointment) {
        try {
            Customer patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();

            if (patient == null) {
                logger.warn("Appointment {} has no patient assigned", appointment.getId());
                return;
            }

            // Send email reschedule notification if patient has an email
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                sendEmailRescheduleNotification(appointment, patient, doctor);
            }

            // Send SMS reschedule notification if patient has a phone number and SMS service is enabled
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                sendSmsRescheduleNotification(appointment, patient, doctor);
            } else {
                logger.info("No phone number available for patient {}, skipping SMS reschedule notification",
                        patient.getFirstName() + " " + patient.getLastName());
            }
        } catch (Exception e) {
            logger.error("Error sending reschedule notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendCancellationNotification(Appointment appointment) {
        try {
            Customer patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();

            if (patient == null) {
                logger.warn("Appointment {} has no patient assigned", appointment.getId());
                return;
            }

            // Send email cancellation notification if patient has an email
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                sendEmailCancellationNotification(appointment, patient, doctor);
            }

            // Send SMS cancellation notification if patient has a phone number and SMS service is enabled
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                sendSmsCancellationNotification(appointment, patient, doctor);
            } else {
                logger.info("No phone number available for patient {}, skipping SMS cancellation notification",
                        patient.getFirstName() + " " + patient.getLastName());
            }
        } catch (Exception e) {
            logger.error("Error sending cancellation notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendCompletionNotification(Appointment appointment) {
        try {
            Customer patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();

            if (patient == null) {
                logger.warn("Appointment {} has no patient assigned", appointment.getId());
                return;
            }

            // Send email completion notification if patient has an email
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                sendEmailCompletionNotification(appointment, patient, doctor);
            }

            // Send SMS completion notification if patient has a phone number and SMS service is enabled
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                sendSmsCompletionNotification(appointment, patient, doctor);
            } else {
                logger.info("No phone number available for patient {}, skipping SMS completion notification",
                        patient.getFirstName() + " " + patient.getLastName());
            }
        } catch (Exception e) {
            logger.error("Error sending completion notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendNoShowNotification(Appointment appointment) {
        try {
            Customer patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();

            if (patient == null) {
                logger.warn("Appointment {} has no patient assigned", appointment.getId());
                return;
            }

            // Send email no-show notification if patient has an email
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                sendEmailNoShowNotification(appointment, patient, doctor);
            }

            // Send SMS no-show notification if patient has a phone number and SMS service is enabled
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                sendSmsNoShowNotification(appointment, patient, doctor);
            } else {
                logger.info("No phone number available for patient {}, skipping SMS no-show notification",
                        patient.getFirstName() + " " + patient.getLastName());
            }
        } catch (Exception e) {
            logger.error("Error sending no-show notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendEmailNoShowNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            if (!mailEnabled) {
                logger.info("Mail sending is disabled via configuration.");
                return;
            }
            
            JavaMailSender mailSender = appointmentReminderService.getMailSender();
            String fromEmail = appointmentReminderService.getFromEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(patient.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Appointment No-Show Notice - HMS");

            String emailBody = buildAppointmentNoShowEmail(appointment, patient, doctor);
            helper.setText(emailBody, true);

            mailSender.send(mimeMessage);
            logger.info("No-show email notification sent successfully for appointment ID: {}", appointment.getId());

        } catch (Exception e) {
            logger.error("Error sending no-show email notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public void sendSmsNoShowNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            // For now, we'll log the SMS content - in production this would integrate with an SMS service
            String smsContent = buildAppointmentNoShowSms(appointment, patient, doctor);
            logger.info("No-show SMS notification for appointment ID: {} - Content: {}", appointment.getId(), smsContent);
            
            // In production, this would call an SMS service API
            // smsService.sendSms(patient.getPhoneNumber(), smsContent);
            
        } catch (Exception e) {
            logger.error("Error sending no-show SMS notification for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Override
    public String buildAppointmentNoShowEmail(Appointment appointment, Customer patient, Doctor doctor) {
        StringBuilder emailBuilder = new StringBuilder();
        emailBuilder.append("<html><body style='font-family: Arial, sans-serif; margin: 20px;'>");
        
        emailBuilder.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px;'>");
        emailBuilder.append("<h2 style='color: #dc3545;'>Appointment No-Show Notice</h2>");
        
        emailBuilder.append("<p>Dear <strong>").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append("</strong>,</p>");
        
        emailBuilder.append("<p>We noticed that you did not attend your scheduled appointment:</p>");
        
        emailBuilder.append("<div style='background-color: #fff; border: 1px solid #dee2e6; padding: 15px; margin: 15px 0; border-radius: 5px;'>");
        emailBuilder.append("<p><strong>Doctor:</strong> ").append(doctor.getDoctorName()).append("</p>");
        emailBuilder.append("<p><strong>Date & Time:</strong> ").append(appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a"))).append("</p>");
        emailBuilder.append("<p><strong>Reason:</strong> ").append(appointment.getReason()).append("</p>");
        if (appointment.getNoShowReason() != null && !appointment.getNoShowReason().isEmpty()) {
            emailBuilder.append("<p><strong>No-Show Reason:</strong> ").append(appointment.getNoShowReason()).append("</p>");
        }
        emailBuilder.append("<p><strong>Appointment Number:</strong> ").append(appointment.getAppointmentNumber()).append("</p>");
        emailBuilder.append("</div>");
        
        emailBuilder.append("<p style='color: #6c757d;'>Please note that repeated no-shows may affect your ability to book future appointments.</p>");
        
        emailBuilder.append("<p>If you need to reschedule or have any questions, please contact our office.</p>");
        
        emailBuilder.append("<p>Thank you for your understanding.</p>");
        
        emailBuilder.append("<p>Best regards,<br/>HMS Team</p>");
        
        emailBuilder.append("</div>");
        emailBuilder.append("</body></html>");
        
        return emailBuilder.toString();
    }

    @Override
    public String buildAppointmentNoShowSms(Appointment appointment, Customer patient, Doctor doctor) {
        StringBuilder smsBuilder = new StringBuilder();
        smsBuilder.append("HMS No-Show Notice: Your appointment with Dr. ").append(doctor.getDoctorName())
                .append(" on ").append(appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .append(" was marked as no-show. If you need to reschedule, please contact our office.");
        
        if (appointment.getNoShowReason() != null && !appointment.getNoShowReason().isEmpty()) {
            smsBuilder.append(" Reason: ").append(appointment.getNoShowReason());
        }
        
        return smsBuilder.toString();
    }

    @Override
    public void sendEmailCreationNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {

            if (!mailEnabled) {
                logger.info("Mail sending is disabled via configuration.");
                return;
            }
            JavaMailSender mailSender = appointmentReminderService.getMailSender();
            String fromEmail = appointmentReminderService.getFromEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(patient.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Appointment Confirmation - HMS");

            String emailBody = buildAppointmentCreationEmail(appointment, patient, doctor);
            helper.setText(emailBody, true);

            mailSender.send(mimeMessage);

            logger.info("Email creation notification sent successfully to patient {} ({})",
                    patient.getFirstName() + " " + patient.getLastName(), patient.getEmail());
        } catch (MessagingException e) {
            logger.error("Error sending creation email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailConfirmationNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {

            if (!mailEnabled) {
                logger.info("Mail sending is disabled via configuration.");
                return;
            }
            JavaMailSender mailSender = appointmentReminderService.getMailSender();
            String fromEmail = appointmentReminderService.getFromEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(patient.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Appointment Confirmed - HMS");

            String emailBody = buildAppointmentConfirmationEmail(appointment, patient, doctor);
            helper.setText(emailBody, true);

            mailSender.send(mimeMessage);

            logger.info("Email confirmation notification sent successfully to patient {} ({})",
                    patient.getFirstName() + " " + patient.getLastName(), patient.getEmail());
        } catch (MessagingException e) {
            logger.error("Error sending confirmation email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailRescheduleNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {

            if (!mailEnabled) {
                logger.info("Mail sending is disabled via configuration.");
                return;
            }
            JavaMailSender mailSender = appointmentReminderService.getMailSender();
            String fromEmail = appointmentReminderService.getFromEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(patient.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Appointment Rescheduled - HMS");

            String emailBody = buildAppointmentRescheduleEmail(appointment, patient, doctor);
            helper.setText(emailBody, true);

            mailSender.send(mimeMessage);

            logger.info("Email reschedule notification sent successfully to patient {} ({})",
                    patient.getFirstName() + " " + patient.getLastName(), patient.getEmail());
        } catch (MessagingException e) {
            logger.error("Error sending reschedule email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailCancellationNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {

            if (!mailEnabled) {
                logger.info("Mail sending is disabled via configuration.");
                return;
            }

            JavaMailSender mailSender = appointmentReminderService.getMailSender();
            String fromEmail = appointmentReminderService.getFromEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(patient.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Appointment Cancelled - HMS");

            String emailBody = buildAppointmentCancellationEmail(appointment, patient, doctor);
            helper.setText(emailBody, true);

            mailSender.send(mimeMessage);

            logger.info("Email cancellation notification sent successfully to patient {} ({})",
                    patient.getFirstName() + " " + patient.getLastName(), patient.getEmail());
        } catch (MessagingException e) {
            logger.error("Error sending cancellation email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailCompletionNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {

            if (!mailEnabled) {
                logger.info("Mail sending is disabled via configuration.");
                return;
            }
            JavaMailSender mailSender = appointmentReminderService.getMailSender();
            String fromEmail = appointmentReminderService.getFromEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(patient.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Appointment Completed - HMS");

            String emailBody = buildAppointmentCompletionEmail(appointment, patient, doctor);
            helper.setText(emailBody, true);

            mailSender.send(mimeMessage);

            logger.info("Email completion notification sent successfully to patient {} ({})",
                    patient.getFirstName() + " " + patient.getLastName(), patient.getEmail());
        } catch (MessagingException e) {
            logger.error("Error sending completion email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsCreationNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

            // Get company information
            String companyName = appointment.getCompany() != null ?
                    appointment.getCompany().getCompanyName() :
                    (patient.getCompany() != null ? patient.getCompany().getCompanyName() : "Healthcare Center");

            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = doctor != null ? doctor.getDoctorName() : "Doctor";
            String appointmentDateTime = appointment.getAppointmentDate().format(formatter);
            String appointmentNo = appointment.getAppointmentNumber() != null ?
                    appointment.getAppointmentNumber() : appointment.getId().toString();

            // Create appointment confirmation message
            String message = String.format(
                    "Hello %s, your appointment with Dr. %s on %s has been confirmed. Appointment ID: %s. Arrive 15 mins early. - %s",
                    patientName,
                    doctorName != null ? doctorName : "Doctor",
                    appointmentDateTime,
                    appointmentNo,
                    companyName
            );

            ResponseEntity<?> smsResponse = appointmentReminderService.sendAppointmentCreationSms(
                    patientName,
                    patient.getPhoneNumber(),
                    doctorName,
                    appointmentDateTime,
                    appointmentNo,
                    companyName
            );

            if (smsResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS creation notification sent successfully to patient {} at {}",
                        patientName, patient.getPhoneNumber());
            } else {
                logger.error("Failed to send SMS creation notification to patient {}: {}",
                        patientName, smsResponse.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS creation notification to patient {}: {}",
                    patient.getFirstName() + " " + patient.getLastName(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsConfirmationNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

            // Get company information
            String companyName = Optional.ofNullable(appointment.getBranch())
                .map(Branch::getBranchName)
                .orElse("Healthcare Center");

            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = doctor != null ? doctor.getDoctorName() : "Doctor";
            String appointmentDateTime = appointment.getAppointmentDate().format(formatter);
            String appointmentNo = appointment.getAppointmentNumber() != null ?
                    appointment.getAppointmentNumber() : appointment.getId().toString();

            // Create appointment confirmation message
            String message = String.format(
                    "Hello %s, your appointment with Dr. %s on %s has been confirmed. Appointment ID: %s. Arrive 15 mins early. - %s",
                    patientName,
                    doctorName != null ? doctorName : "Doctor",
                    appointmentDateTime,
                    appointmentNo,
                    companyName
            );

            ResponseEntity<?> smsResponse = msg91SmsService.sendAppointmentRescheduleSms(
                patientName,
                patient.getPhoneNumber(),
                doctorName,
                appointmentDateTime,
                appointmentNo,
                companyName,
                false,
                true,
                false,
                false
            );

            if (smsResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS confirmation notification sent successfully to patient {} at {}",
                        patientName, patient.getPhoneNumber());
            } else {
                logger.error("Failed to send SMS confirmation notification to patient {}: {}",
                        patientName, smsResponse.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS confirmation notification to patient {}: {}",
                    patient.getFirstName() + " " + patient.getLastName(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsRescheduleNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

            // Get company information
            String companyName = Optional.ofNullable(appointment.getBranch())
                .map(Branch::getBranchName)
                .orElse("Healthcare Center");

            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = doctor != null ? doctor.getDoctorName() : "Doctor";
            String originalDateTime = appointment.getRescheduledFrom() != null ?
                    appointment.getRescheduledFrom().format(formatter) : "Unknown";
            String newDateTime = appointment.getAppointmentDate().format(formatter);
            String appointmentNo = appointment.getAppointmentNumber() != null ?
                    appointment.getAppointmentNumber() : appointment.getId().toString();

            ResponseEntity<?> smsResponse = msg91SmsService.sendAppointmentRescheduleSms(
                patientName,
                patient.getPhoneNumber(),
                doctorName,
                newDateTime, // Using the new date/time
                appointmentNo,
                companyName,
                true,
                false,
                false,
                false
            );

            if (smsResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS reschedule notification sent successfully to patient {} at {}",
                        patientName, patient.getPhoneNumber());
            } else {
                logger.error("Failed to send SMS reschedule notification to patient {}: {}",
                        patientName, smsResponse.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS reschedule notification to patient {}: {}",
                    patient.getFirstName() + " " + patient.getLastName(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsCancellationNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

            // Get company information
            String companyName = Optional.ofNullable(appointment.getBranch())
                .map(Branch::getBranchName)
                .orElse("Healthcare Center");

            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = doctor != null ? doctor.getDoctorName() : "Doctor";
            String appointmentDateTime = appointment.getAppointmentDate().format(formatter);
            String appointmentNo = appointment.getAppointmentNumber() != null ?
                    appointment.getAppointmentNumber() : appointment.getId().toString();
            String cancellationReason = appointment.getCancellationReason() != null ?
                    appointment.getCancellationReason() : "No reason provided";

            // Create appointment cancellation message
            String message = String.format(
                    "Hello %s, your appointment with Dr. %s on %s (ID: %s) has been cancelled. Reason: %s. - %s",
                    patientName,
                    doctorName != null ? doctorName : "Doctor",
                    appointmentDateTime,
                    appointmentNo,
                    cancellationReason,
                    companyName
            );

            ResponseEntity<?> smsResponse = msg91SmsService.sendAppointmentRescheduleSms(
                patientName,
                patient.getPhoneNumber(),
                doctorName,
                appointmentDateTime,
                appointmentNo,
                companyName,
                false,
                false,
                true,
                false
            );

            if (smsResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS cancellation notification sent successfully to patient {} at {}",
                        patientName, patient.getPhoneNumber());
            } else {
                logger.error("Failed to send SMS cancellation notification to patient {}: {}",
                        patientName, smsResponse.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS cancellation notification to patient {}: {}",
                    patient.getFirstName() + " " + patient.getLastName(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsCompletionNotification(Appointment appointment, Customer patient, Doctor doctor) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

            // Get company information
            String companyName = Optional.ofNullable(appointment.getBranch())
                .map(Branch::getBranchName)
                .orElse("Healthcare Center");

            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = doctor != null ? doctor.getDoctorName() : "Doctor";
            String appointmentDateTime = appointment.getAppointmentDate().format(formatter);
            String appointmentNo = appointment.getAppointmentNumber() != null ?
                    appointment.getAppointmentNumber() : appointment.getId().toString();

            ResponseEntity<?> smsResponse = msg91SmsService.sendAppointmentRescheduleSms(
                patientName,
                patient.getPhoneNumber(),
                doctorName,
                appointmentDateTime,
                appointmentNo,
                companyName,
                false,
                false,
                false,
                true
            );

            if (smsResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS completion notification sent successfully to patient {} at {}",
                        patientName, patient.getPhoneNumber());
            } else {
                logger.error("Failed to send SMS completion notification to patient {}: {}",
                        patientName, smsResponse.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS completion notification to patient {}: {}",
                    patient.getFirstName() + " " + patient.getLastName(), e.getMessage(), e);
        }
    }

    @Override
    public String buildAppointmentCreationEmail(Appointment appointment, Customer patient, Doctor doctor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        // Get company information from the appointment
        String companyName = appointment.getCompany() != null ?
                appointment.getCompany().getCompanyName() :
                (patient.getCompany() != null ? patient.getCompany().getCompanyName() : "Healthcare Center");

        // Get branch information from the patient
        String branchName = patient.getBranch() != null ?
                patient.getBranch().getBranchName() :
                "Main Branch";
        return """
                <html>
                <body style="margin:0; padding:0; font-family: Arial, sans-serif; background-color:#eef2f7;">
                
                    <div style="max-width:620px; width:100%%; margin:40px auto; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 6px 18px rgba(0,0,0,0.08);">
                
                        <!-- Header -->
                        <div style="padding:25px; text-align:center; background:linear-gradient(90deg,#1e3c72,#2a5298);">
                            <h2 style="color:#ffffff; margin:0; font-weight:600;">Appointment Confirmed</h2>
                        </div>
                
                        <!-- Content -->
                        <div style="padding:30px;">
                
                            <p style="font-size:16px; margin-bottom:10px;">
                                Hello <strong>%s %s</strong>,
                            </p>
                
                            <p style="color:#555; line-height:1.6;">
                                Your appointment at <strong>%s</strong> (<strong>%s</strong>) 
                                is currently <strong>%s</strong>.
                            </p>
                
                            <!-- Info Cards -->
                            <div style="margin-top:25px;">
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Doctor:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Date & Time:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Reason:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Status:</strong><br>
                                    <span style="color:#28a745; font-weight:bold;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Appointment ID:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px;">
                                    <strong>Location:</strong><br>
                                    <span style="color:#333;">%s (%s)</span>
                                </div>
                
                            </div>
                
                            <p style="margin-top:30px; font-size:14px; color:#555;">
                                Please arrive at least 15 minutes before your scheduled appointment time. 
                                If you need to reschedule or cancel, please contact our office.
                            </p>
                
                        </div>
                
                        <!-- Footer -->
                        <div style="background:#f5f7fa; padding:18px; text-align:center; font-size:12px; color:#777;">
                            This is an automated confirmation from %s.<br>
                            Please do not reply to this email.
                        </div>
                
                    </div>
                
                </body>
                </html>
                """.formatted(
                patient.getFirstName(),
                patient.getLastName(),
                companyName,
                branchName,
                appointment.getStatus(),  // Add the actual appointment status here
                doctor != null ? doctor.getDoctorName() : "Unassigned",
                appointment.getAppointmentDate().format(formatter),
                appointment.getReason(),
                appointment.getStatus(),
                appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId(),
                companyName,
                branchName,
                companyName
        );
    }

    @Override
    public String buildAppointmentConfirmationEmail(Appointment appointment, Customer patient, Doctor doctor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        // Get company information from the appointment
        String companyName = appointment.getCompany() != null ?
                appointment.getCompany().getCompanyName() :
                (patient.getCompany() != null ? patient.getCompany().getCompanyName() : "Healthcare Center");

        // Get branch information from the patient
        String branchName = patient.getBranch() != null ?
                patient.getBranch().getBranchName() :
                "Main Branch";

        return """
                <html>
                <body style="margin:0; padding:0; font-family: Arial, sans-serif; background-color:#eef2f7;">
                
                    <div style="max-width:620px; width:100%%; margin:40px auto; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 6px 18px rgba(0,0,0,0.08);">
                
                        <!-- Header -->
                        <div style="padding:25px; text-align:center; background:linear-gradient(90deg,#1e3c72,#2a5298);">
                            <h2 style="color:#ffffff; margin:0; font-weight:600;">Appointment Confirmed</h2>
                        </div>
                
                        <!-- Content -->
                        <div style="padding:30px;">
                
                            <p style="font-size:16px; margin-bottom:10px;">
                                Hello <strong>%s %s</strong>,
                            </p>
                
                            <p style="color:#555; line-height:1.6;">
                                Your appointment at <strong>%s</strong> (<strong>%s</strong>) 
                                is now <strong>%s</strong>.
                            </p>
                
                            <!-- Info Cards -->
                            <div style="margin-top:25px;">
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Doctor:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Date & Time:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Reason:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Status:</strong><br>
                                    <span style="color:#28a745; font-weight:bold;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Appointment ID:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px;">
                                    <strong>Location:</strong><br>
                                    <span style="color:#333;">%s (%s)</span>
                                </div>
                
                            </div>
                
                            <p style="margin-top:30px; font-size:14px; color:#555;">
                                Please arrive at least 15 minutes before your scheduled appointment time. 
                                If you need to reschedule or cancel, please contact our office.
                            </p>
                
                        </div>
                
                        <!-- Footer -->
                        <div style="background:#f5f7fa; padding:18px; text-align:center; font-size:12px; color:#777;">
                            This is an automated confirmation from %s.<br>
                            Please do not reply to this email.
                        </div>
                
                    </div>
                
                </body>
                </html>
                """.formatted(
                patient.getFirstName(),
                patient.getLastName(),
                companyName,
                branchName,
                appointment.getStatus(),
                doctor != null ? doctor.getDoctorName() : "Unassigned",
                appointment.getAppointmentDate().format(formatter),
                appointment.getReason(),
                appointment.getStatus(),
                appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId(),
                companyName,
                branchName,
                companyName
        );
    }

    @Override
    public String buildAppointmentRescheduleEmail(Appointment appointment, Customer patient, Doctor doctor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        // Get company information from the appointment
        String companyName = appointment.getCompany() != null ?
                appointment.getCompany().getCompanyName() :
                (patient.getCompany() != null ? patient.getCompany().getCompanyName() : "Healthcare Center");

        // Get branch information from the patient
        String branchName = patient.getBranch() != null ?
                patient.getBranch().getBranchName() :
                "Main Branch";

        String originalDateTime = appointment.getRescheduledFrom() != null ?
                appointment.getRescheduledFrom().format(formatter) : "Unknown";

        return """
                <html>
                <body style="margin:0; padding:0; font-family: Arial, sans-serif; background-color:#eef2f7;">
                
                    <div style="max-width:620px; width:100%%; margin:40px auto; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 6px 18px rgba(0,0,0,0.08);">
                
                        <!-- Header -->
                        <div style="padding:25px; text-align:center; background:linear-gradient(90deg,#1e3c72,#2a5298);">
                            <h2 style="color:#ffffff; margin:0; font-weight:600;">Appointment Rescheduled</h2>
                        </div>
                
                        <!-- Content -->
                        <div style="padding:30px;">
                
                            <p style="font-size:16px; margin-bottom:10px;">
                                Hello <strong>%s %s</strong>,
                            </p>
                
                            <p style="color:#555; line-height:1.6;">
                                Your appointment at <strong>%s</strong> (<strong>%s</strong>) 
                                has been rescheduled from <strong>%s</strong> to <strong>%s</strong>.
                            </p>
                
                            <!-- Info Cards -->
                            <div style="margin-top:25px;">
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Doctor:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Previous Date & Time:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>New Date & Time:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Reason:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Status:</strong><br>
                                    <span style="color:#ffc107; font-weight:bold;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Appointment ID:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px;">
                                    <strong>Location:</strong><br>
                                    <span style="color:#333;">%s (%s)</span>
                                </div>
                
                            </div>
                
                            <p style="margin-top:30px; font-size:14px; color:#555;">
                                Please update your calendar accordingly. If you need to reschedule again or have any concerns, 
                                please contact our office.
                            </p>
                
                        </div>
                
                        <!-- Footer -->
                        <div style="background:#f5f7fa; padding:18px; text-align:center; font-size:12px; color:#777;">
                            This is an automated reschedule notification from %s.<br>
                            Please do not reply to this email.
                        </div>
                
                    </div>
                
                </body>
                </html>
                """.formatted(
                patient.getFirstName(),
                patient.getLastName(),
                companyName,
                branchName,
                originalDateTime,
                appointment.getAppointmentDate().format(formatter),
                doctor != null ? doctor.getDoctorName() : "Unassigned",
                originalDateTime,
                appointment.getAppointmentDate().format(formatter),
                appointment.getReason(),
                appointment.getStatus(),
                appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId(),
                companyName,
                branchName,
                companyName
        );
    }

    @Override
    public String buildAppointmentCancellationEmail(Appointment appointment, Customer patient, Doctor doctor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        // Get company information from the appointment
        String companyName = appointment.getCompany() != null ?
                appointment.getCompany().getCompanyName() :
                (patient.getCompany() != null ? patient.getCompany().getCompanyName() : "Healthcare Center");

        // Get branch information from the patient
        String branchName = patient.getBranch() != null ?
                patient.getBranch().getBranchName() :
                "Main Branch";

        String cancellationReason = appointment.getCancellationReason() != null ?
                appointment.getCancellationReason() : "Not specified";

        return """
                <html>
                <body style="margin:0; padding:0; font-family: Arial, sans-serif; background-color:#eef2f7;">
                
                    <div style="max-width:620px; width:100%%; margin:40px auto; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 6px 18px rgba(0,0,0,0.08);">
                
                        <!-- Header -->
                        <div style="padding:25px; text-align:center; background:linear-gradient(90deg,#dc3545,#c82333);">
                            <h2 style="color:#ffffff; margin:0; font-weight:600;">Appointment Cancelled</h2>
                        </div>
                
                        <!-- Content -->
                        <div style="padding:30px;">
                
                            <p style="font-size:16px; margin-bottom:10px;">
                                Hello <strong>%s %s</strong>,
                            </p>
                
                            <p style="color:#555; line-height:1.6;">
                                Your appointment at <strong>%s</strong> (<strong>%s</strong>) 
                                has been cancelled.
                            </p>
                
                            <!-- Info Cards -->
                            <div style="margin-top:25px;">
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Doctor:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Date & Time:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Reason:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Status:</strong><br>
                                    <span style="color:#dc3545; font-weight:bold;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Cancellation Reason:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px; margin-bottom:10px;">
                                    <strong>Appointment ID:</strong><br>
                                    <span style="color:#333;">%s</span>
                                </div>
                
                                <div style="padding:15px; border:1px solid #e3e6ea; border-radius:8px;">
                                    <strong>Location:</strong><br>
                                    <span style="color:#333;">%s (%s)</span>
                                </div>
                
                            </div>
                
                            <p style="margin-top:30px; font-size:14px; color:#555;">
                                We apologize for any inconvenience. If you need to schedule a new appointment, 
                                please contact our office.
                            </p>
                
                        </div>
                
                        <!-- Footer -->
                        <div style="background:#f5f7fa; padding:18px; text-align:center; font-size:12px; color:#777;">
                            This is an automated cancellation notification from %s.<br>
                            Please do not reply to this email.
                        </div>
                
                    </div>
                
                </body>
                </html>
                """.formatted(
                patient.getFirstName(),
                patient.getLastName(),
                companyName,
                branchName,
                doctor != null ? doctor.getDoctorName() : "Unassigned",
                appointment.getAppointmentDate().format(formatter),
                appointment.getReason(),
                appointment.getStatus(),
                cancellationReason,
                appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId(),
                companyName,
                branchName,
                companyName
        );
    }

    @Override
    public String buildAppointmentCompletionEmail(Appointment appointment, Customer patient, Doctor doctor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        // Get company information from the appointment
        String companyName = appointment.getCompany() != null ?
            appointment.getCompany().getCompanyName() :
            (patient.getCompany() != null ? patient.getCompany().getCompanyName() : "Healthcare Center");

        // Get branch information from the patient
        String branchName = patient.getBranch() != null ?
            patient.getBranch().getBranchName() :
            "Main Branch";

        String consultationNotes = appointment.getConsultationNotes() != null ?
            appointment.getConsultationNotes() : "No consultation notes provided";

        return """
            <html>
            <body style="font-family: Arial, sans-serif; background-color:#f4f6f8; padding:20px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h1 style="color: #28a745;">Appointment Completed</h1>
                    </div>

                    <p>Hello <strong>%s %s</strong>,</p>

                    <p>Your appointment at <strong>%s</strong> (<strong>%s</strong>) has been marked as completed:</p>

                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Doctor:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Date & Time:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Reason:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Status:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">
                                    <span style="color: #28a745; font-weight: bold;">COMPLETED</span>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Consultation Notes:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Appointment ID:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; vertical-align: top;"><strong>Location:</strong></td>
                                <td style="padding: 10px; vertical-align: top;">%s (%s)</td>
                            </tr>
                        </table>
                    </div>

                    <p>If you have any questions about your consultation or need to schedule a follow-up, please contact our office.</p>

                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; color: #666; font-size: 12px;">
                        <p>This is an automated completion notification from %s. Please do not reply to this email.</p>
                        <p>If you have any questions, please contact our office directly.</p>
                    </div>

            </body>
            </html>
            """.formatted(
            patient.getFirstName(),
            patient.getLastName(),
            companyName,
            branchName,
            doctor != null ? doctor.getDoctorName() : "Unassigned",
            appointment.getAppointmentDate().format(formatter),
            appointment.getReason(),
            consultationNotes,
            appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId(),
            companyName,
            branchName,
            companyName
        );
    }
}