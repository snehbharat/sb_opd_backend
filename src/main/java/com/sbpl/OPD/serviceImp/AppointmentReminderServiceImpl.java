package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.AppointmentReminderService;
import com.sbpl.OPD.service.Msg91SmsService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentReminderServiceImpl implements AppointmentReminderService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private Msg91SmsService msg91SmsService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.enable:true}")
    private boolean mailEnabled;

    private static final Logger logger = LoggerFactory.getLogger(AppointmentReminderServiceImpl.class);

    /**
     * Scheduled method to send appointment reminders every hour for appointments in the next hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 milliseconds)
    public void scheduleHourlyAppointmentReminders() {
        logger.info("Starting hourly appointment reminder job at {}", LocalDateTime.now());
        sendAppointmentReminders(60); // Send reminders for appointments in the next hour
    }

    /**
     * Scheduled method to send daily appointment reminders for appointments in the next day
     */
    @Scheduled(cron = "0 0 9 * * ?") // Run daily at 9 AM
    public void scheduleDailyAppointmentReminders() {
        logger.info("Starting daily appointment reminder job at {}", LocalDateTime.now());
        sendAppointmentReminders(1440); // Send reminders for appointments in the next 24 hours
    }

    @Override
    public List<Appointment> findAppointmentsWithinTimeWindow(int minutesBefore) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(minutesBefore);

        // Find confirmed appointments within the time window
        return appointmentRepository.findByStatusAndAppointmentDateBetween(com.sbpl.OPD.enums.AppointmentStatus.CONFIRMED, now, endTime);
    }

    @Override
    public List<Appointment> findAppointmentsWithinTimeWindow(int minutesBefore, String status) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(minutesBefore);

        com.sbpl.OPD.enums.AppointmentStatus appointmentStatus;
        try {
            appointmentStatus = com.sbpl.OPD.enums.AppointmentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid appointment status provided: {}. Using CONFIRMED as default.", status);
            appointmentStatus = com.sbpl.OPD.enums.AppointmentStatus.CONFIRMED;
        }
        return appointmentRepository.findByStatusAndAppointmentDateBetween(appointmentStatus, now, endTime);
    }

    @Override
    public ResponseEntity<?> sendAppointmentReminders(int minutesBefore) {
        try {
            List<Appointment> appointments = findAppointmentsWithinTimeWindow(minutesBefore);

            if (appointments.isEmpty()) {
                logger.info("No appointments found for reminder notification within {} minutes", minutesBefore);
                return baseResponse.successResponse("No appointments found for reminder notifications");
            }

            int successCount = 0;
            int failureCount = 0;

            for (Appointment appointment : appointments) {
                try {
                    sendReminderNotification(appointment);
                    successCount++;
                    Thread.sleep(100); // Small delay to avoid overwhelming the mail server
                } catch (Exception e) {
                    logger.error("Failed to send reminder for appointment ID: {}", appointment.getId(), e);
                    failureCount++;
                }
            }

            String message = String.format(
                    "Appointment reminder job completed. Sent: %d, Failed: %d, Total: %d",
                    successCount, failureCount, appointments.size()
            );

            logger.info(message);
            return baseResponse.successResponse(message,
                    java.util.Map.of("sent", successCount, "failed", failureCount, "total", appointments.size()));

        } catch (Exception e) {
            logger.error("Error occurred while sending appointment reminders", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send appointment reminders: " + e.getMessage());
        }
    }


    @Override
    public ResponseEntity<?> sendDailyAppointmentReminders() {
        return sendAppointmentReminders(1440); // Reminders for appointments in next 24 hours
    }

    /**
     * Send reminder notification (email and SMS) for a specific appointment
     */
    private void sendReminderNotification(Appointment appointment) throws MessagingException {
        Customer patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();

        if (patient == null) {
            logger.warn("Appointment {} has no patient assigned", appointment.getId());
            return;
        }

        // Send email reminder if patient has an email
        if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
            sendEmailReminder(appointment, patient, doctor);
        }

        // Send SMS reminder if patient has a phone number and SMS service is enabled
        if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
            sendSmsReminder(appointment, patient, doctor);
        } else {
            logger.info("No phone number available for patient {}, skipping SMS reminder",
                    patient.getFirstName() + " " + patient.getLastName());
        }
    }

    /**
     * Send email reminder to the patient
     */
    private void sendEmailReminder(Appointment appointment, Customer patient, Doctor doctor) throws MessagingException {
        if (!mailEnabled) {
            logger.info("Mail sending is disabled via configuration.");
            return;
        }
        
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(patient.getEmail());
        helper.setFrom(fromEmail);
        helper.setSubject("Appointment Reminder - HMS");

        String emailBody = buildAppointmentReminderEmail(appointment, patient, doctor);
        helper.setText(emailBody, true);

        mailSender.send(mimeMessage);

        logger.info("Email reminder sent successfully to patient {} ({})",
                patient.getFirstName() + " " + patient.getLastName(), patient.getEmail());
    }

    /**
     * Build the HTML email template for appointment reminders
     */
    private String buildAppointmentReminderEmail(Appointment appointment, Customer patient, Doctor doctor) {
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
                <body style="font-family: Arial, sans-serif; background-color:#f4f6f8; padding:20px;">
                    <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                
                        <div style="text-align: center; margin-bottom: 20px;">
                            <h1 style="color: #007bff;">Appointment Reminder</h1>
                        </div>
                
                        <p>Hello <strong>%s %s</strong>,</p>
                
                        <p>This is a friendly reminder about your upcoming appointment at <strong>%s</strong> (<strong>%s</strong>):</p>
                
                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <table style="width: 100%; border-collapse: collapse;">
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
                                    <td style="padding: 10px; vertical-align: top;"><strong>Appointment ID:</strong></td>
                                    <td style="padding: 10px; vertical-align: top;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; vertical-align: top;"><strong>Location:</strong></td>
                                    <td style="padding: 10px; vertical-align: top;">%s (%s)</td>
                                </tr>
                            </table>
                        </div>
                
                        <p>Please arrive 10-15 minutes early to complete any necessary paperwork.</p>
                
                        <p>If you need to reschedule or cancel this appointment, please contact us as soon as possible.</p>
                
                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; color: #666; font-size: 12px;">
                            <p>This is an automated reminder from %s. Please do not reply to this email.</p>
                            <p>If you have any questions, please contact our office directly.</p>
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
                appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId(),
                companyName,
                branchName,
                companyName
        );
    }

    /**
     * Send SMS reminder to the patient using MSG91
     */
    private void sendSmsReminder(Appointment appointment, Customer patient, Doctor doctor) {
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

            ResponseEntity<?> smsResponse = msg91SmsService.sendAppointmentReminderSms(
                    patientName,
                    patient.getPhoneNumber(),
                    doctorName,
                    appointmentDateTime,
                    appointmentNo,
                    companyName
            );

            if (smsResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS reminder sent successfully to patient {} at {}",
                        patientName, patient.getPhoneNumber());
            } else {
                logger.error("Failed to send SMS reminder to patient {}: {}",
                        patientName, smsResponse.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending SMS reminder to patient {}: {}",
                    patient.getFirstName() + " " + patient.getLastName(), e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<?> sendAppointmentCompletionSms(
            String patientName,
            String phoneNumber,
            String doctorName,
            String appointmentDateTime,
            String appointmentNo,
            String companyName) {

        if (!msg91SmsService.isSmsServiceEnabled()) {
            logger.info("SMS service is disabled. Skipping appointment completion SMS to {}", patientName);
            return baseResponse.successResponse("SMS service is disabled");
        }

        if (!msg91SmsService.isValidPhoneNumber(phoneNumber)) {
            logger.warn("Invalid phone number for patient {}: {}", patientName, phoneNumber);
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid phone number format");
        }

        // Create appointment completion message
        String message = String.format(
                "Hello %s, your appointment with Dr. %s on %s has been marked as completed. Appointment ID: %s. Thank you for visiting %s",
                patientName,
                doctorName != null ? doctorName : "Doctor",
                appointmentDateTime,
                appointmentNo,
                companyName
        );

        logger.info("Sending appointment completion SMS to {} at {}", patientName, phoneNumber);
        return msg91SmsService.sendSms(phoneNumber, message);
    }

    @Override
    public ResponseEntity<?> sendAppointmentCreationSms(
            String patientName,
            String phoneNumber,
            String doctorName,
            String appointmentDateTime,
            String appointmentNo,
            String companyName) {

        if (!msg91SmsService.isSmsServiceEnabled()) {
            logger.info("SMS service is disabled. Skipping appointment creation SMS to {}", patientName);
            return baseResponse.successResponse("SMS service is disabled");
        }

        if (!msg91SmsService.isValidPhoneNumber(phoneNumber)) {
            logger.warn("Invalid phone number for patient {}: {}", patientName, phoneNumber);
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid phone number format");
        }

        // Create appointment confirmation message
        String message = String.format(
                "Hello %s, your appointment with Dr. %s on %s has been confirmed. Appointment ID: %s. Arrive 15 mins early. - %s",
                patientName,
                doctorName != null ? doctorName : "Doctor",
                appointmentDateTime,
                appointmentNo,
                companyName
        );

        logger.info("Sending appointment creation SMS to {} at {}", patientName, phoneNumber);
        return msg91SmsService.sendSms(phoneNumber, message);
    }

    @Override
    public JavaMailSender getMailSender() {
        return mailSender;
    }

    @Override
    public String getFromEmail() {
        return fromEmail;
    }

    @Override
    public Msg91SmsService getMsg91SmsService() {
        return msg91SmsService;
    }
}