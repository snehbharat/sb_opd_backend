package com.sbpl.OPD.serviceImp.prescription;

import com.sbpl.OPD.dto.prescription.AdviceDto;
import com.sbpl.OPD.dto.prescription.MedicineDto;
import com.sbpl.OPD.dto.prescription.NoteDto;
import com.sbpl.OPD.dto.prescription.PrescriptionCreateRequestDto;
import com.sbpl.OPD.dto.prescription.PrescriptionResponse;
import com.sbpl.OPD.dto.prescription.TestDto;
import com.sbpl.OPD.model.prescription.MedicineCatalog;
import com.sbpl.OPD.model.prescription.Prescription;
import com.sbpl.OPD.model.prescription.PrescriptionAdvice;
import com.sbpl.OPD.model.prescription.PrescriptionMedicine;
import com.sbpl.OPD.model.prescription.PrescriptionNote;
import com.sbpl.OPD.model.prescription.PrescriptionTest;
import com.sbpl.OPD.model.prescription.PrescriptionVersion;
import com.sbpl.OPD.model.prescription.TestCatalog;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.repository.prescription.PrescriptionRepo;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.prescription.PrescriptionService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * This Is a Prescription Service Implementation class.
 *
 * @author Kousik Manik
 */
@Service
public class PrescriptionServiceImpl implements PrescriptionService {

  @Autowired
  private PrescriptionRepo prescriptionRepo;

  @Autowired
  private CustomerRepository patientDetailsRepository;

  @Autowired
  private DoctorRepository doctorRepository;

  @Autowired
  private BranchRepository clinicDetailsRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private BaseResponse baseResponse;

  public static final Logger logger = LoggerFactory.getLogger(PrescriptionServiceImpl.class);

  /**
   * Creates a new prescription with its first version and related medical details
   * such as medicines, tests, advice, and notes.
   * The prescription is created as an aggregate where {@link Prescription} is the
   * root entity and {@link PrescriptionVersion} is initialized as version 1.
   * All related child entities are saved using cascading.
   * The operation is transactional and ensures atomicity. Either the entire
   * prescription is saved successfully or nothing is persisted.
   *
   * @param request the prescription creation request
   * @return a {@link ResponseEntity} containing {@link PrescriptionResponse}
   *         on success or an error response on failure
   */
  @Transactional
  @Override
  public ResponseEntity<?> createPrescription(PrescriptionCreateRequestDto request) {

    logger.info(
        "Creating prescription [clinicId={}, doctorId={}, patientId={}]",
        request.getClinicId(),
        request.getDoctorId(),
        request.getPatientId()
    );

    try {

      if (!doctorRepository.existsById(request.getDoctorId())) {
        logger.info("Doctor Not Found With Id = {}", request.getDoctorId());
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid DoctorId");
      }

      if (!clinicDetailsRepository.existsById(request.getClinicId())) {
        logger.info("Clinic Not Found With Id = {}", request.getClinicId());
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid ClinicId");
      }

      boolean optionalPatientDetails = patientDetailsRepository.existsById(request.getPatientId());

      if (!optionalPatientDetails) {
        logger.info("Patient Not Found With Id = {}", request.getPatientId());
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid PatientId");
      }

      // ------------------------------------------------------------------
      // 1. Create Parent Entity (Aggregate Root)
      // ------------------------------------------------------------------
      Prescription prescription = new Prescription();
      prescription.setClinicId(request.getClinicId());
      prescription.setPatientId(request.getPatientId());
      prescription.setDoctorId(request.getDoctorId());
      prescription.setDeleted(false);

      // ------------------------------------------------------------------
      // 2. Create First Version (Medical Record)
      // ------------------------------------------------------------------
      PrescriptionVersion version = new PrescriptionVersion();
      version.setVersionNo(1); // First version is always 1
      version.setDoctorId(request.getDoctorId());
      version.setDiagnosis(request.getDiagnosis());

      // Link version to prescription
      prescription.addVersion(version);

      // ------------------------------------------------------------------
      // 3. Map Medicines
      // ------------------------------------------------------------------
      if (request.getMedicines() != null && !request.getMedicines().isEmpty()) {
        logger.info("Mapping {} medicines", request.getMedicines().size());

        for (MedicineDto dto : request.getMedicines()) {
          PrescriptionMedicine pm = new PrescriptionMedicine();
          pm.setMedicineName(dto.getName());
          pm.setDosage(dto.getDosage());
          pm.setDurationDays(dto.getDurationDays());
          pm.setTiming(dto.getTiming());
          pm.setInstructions(dto.getInstructions());

          if (dto.getMedicineId() != null) {
            MedicineCatalog ref =
                entityManager.getReference(MedicineCatalog.class, dto.getMedicineId());
            pm.setMedicine(ref);
          }

          pm.setVersion(version);
          version.getMedicines().add(pm);
        }
      }

      // ------------------------------------------------------------------
      // 4. Map Tests
      // ------------------------------------------------------------------
      if (request.getTests() != null && !request.getTests().isEmpty()) {
        logger.info("Mapping {} tests", request.getTests().size());

        for (TestDto dto : request.getTests()) {
          PrescriptionTest pt = new PrescriptionTest();
          pt.setTestName(dto.getName());
          pt.setInstructions(dto.getInstructions());

          if (dto.getTestId() != null) {
            TestCatalog ref =
                entityManager.getReference(TestCatalog.class, dto.getTestId());
            pt.setTest(ref);
          }

          pt.setVersion(version);
          version.getTests().add(pt);
        }
      }

      // ------------------------------------------------------------------
      // 5. Map Advice
      // ------------------------------------------------------------------
      if (request.getAdvice() != null && !request.getAdvice().isEmpty()) {
        logger.info("Mapping {} advice entries", request.getAdvice().size());

        for (AdviceDto advice : request.getAdvice()) {
          PrescriptionAdvice pa = new PrescriptionAdvice();
          pa.setAdvice(advice.getAdvice());
          pa.setVersion(version);
          version.getAdvice().add(pa);
        }
      }

      // ------------------------------------------------------------------
      // 6. Map Notes
      // ------------------------------------------------------------------
      if (request.getNotes() != null && !request.getNotes().isEmpty()) {
        logger.info("Mapping {} notes", request.getNotes().size());

        for (NoteDto dto : request.getNotes()) {
          PrescriptionNote pn = new PrescriptionNote();
          pn.setNoteType(dto.getNoteType());
          pn.setContent(dto.getContent());
          pn.setVersion(version);
          version.getNotes().add(pn);
        }
      }

      // ------------------------------------------------------------------
      // 7. Persist Aggregate Root
      // ------------------------------------------------------------------
      Prescription savedPrescription = prescriptionRepo.save(prescription);

      logger.info(
          "Prescription created successfully [prescriptionId={}, version=1]",
          savedPrescription.getId()
      );

        logger.info("Document Is Not Linking For prescription Request Id = {}", prescription.getId());
        return baseResponse.successResponse("Prescription Created Successfully", mapToResponse(savedPrescription));


    } catch (Exception e) {

      logger.error(
          "Failed to create prescription [clinicId={}, doctorId={}, patientId={}]",
          request.getClinicId(),
          request.getDoctorId(),
          request.getPatientId(),
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to create prescription"
      );
    }
  }



  /**
   * Maps the persisted {@link Prescription} entity to a {@link PrescriptionResponse}.
   * This response contains only summary information and does not expose
   * internal entity details.
   *
   * @param saved the persisted prescription entity
   * @return the prescription response DTO
   */
  private PrescriptionResponse mapToResponse(Prescription saved) {

    PrescriptionVersion latestVersion = saved.getVersions().getFirst();

    PrescriptionResponse response = new PrescriptionResponse();
    response.setRequestId(saved.getRequestId());
    response.setPrescriptionId(saved.getId());
    response.setVersionNo(latestVersion.getVersionNo());
    response.setCreatedAt(saved.getCreatedAt());
    response.setStatus("CREATED");

    logger.info(
        "Mapped prescription response [prescriptionId={}, version={}]",
        saved.getId(),
        latestVersion.getVersionNo()
    );

    return response;
  }

}
