package com.sbpl.OPD.serviceImp.prescription;

import com.sbpl.OPD.dto.prescription.AdviceDto;
import com.sbpl.OPD.dto.prescription.CreateMedicineCatalogRequestDto;
import com.sbpl.OPD.dto.prescription.CreateTestCatalogRequestDto;
import com.sbpl.OPD.dto.prescription.CreateTestTypeRequestDto;
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
import com.sbpl.OPD.model.prescription.TestType;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.repository.prescription.MedicineCatalogRepo;
import com.sbpl.OPD.repository.prescription.PrescriptionRepo;
import com.sbpl.OPD.repository.prescription.TestCatalogRepo;
import com.sbpl.OPD.repository.prescription.TestTypeRepo;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.prescription.PrescriptionService;
import com.sbpl.OPD.utils.DbUtill;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  private MedicineCatalogRepo medicineCatalogRepo;

  @Autowired
  private TestCatalogRepo testCatalogRepo;

  @Autowired
  private TestTypeRepo testTypeRepo;

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

  /**
   * Creates a new medicine entry in the medicine catalog.
   *
   * <p>
   * This method takes {@link CreateMedicineCatalogRequestDto} as input,
   * maps it to {@link MedicineCatalog} entity, and persists it in the database.
   * </p>
   *
   * <p>
   * Default behavior:
   * <ul>
   *     <li>New medicine is marked as <b>active</b></li>
   *     <li>All fields (name, form, strength) are required</li>
   * </ul>
   *
   * @param dto the request DTO containing medicine details
   * @return {@link ResponseEntity} containing success or error response
   */
  @Override
  public ResponseEntity<?> createMedicineCatalog(CreateMedicineCatalogRequestDto dto) {

    logger.info("Creating new medicine catalog entry. name: {}, form: {}, strength: {}",
        dto.getName(), dto.getForm(), dto.getStrength());

    try {

      boolean exists = medicineCatalogRepo
          .existsByNameIgnoreCaseAndFormIgnoreCaseAndStrengthIgnoreCase(
              dto.getName().trim(),
              dto.getForm(),
              dto.getStrength()
          );

      if (exists) {
        logger.warn("Duplicate medicine entry attempt: {}", dto.getName());

        return baseResponse.errorResponse(
            HttpStatus.CONFLICT,
            "Medicine already exists"
        );
      }

      MedicineCatalog medicineCatalog = new MedicineCatalog();
      medicineCatalog.setName(dto.getName().trim());
      medicineCatalog.setForm(dto.getForm());
      medicineCatalog.setStrength(dto.getStrength());

      MedicineCatalog savedMedicine = medicineCatalogRepo.save(medicineCatalog);

      logger.info("Medicine created successfully with id: {}", savedMedicine.getId());

      return baseResponse.successResponse(
          "Medicine created successfully",
          savedMedicine.getId()
      );

    } catch (Exception e) {

      logger.error(
          "Error occurred while creating medicine. name: {}, form: {}, strength: {}",
          dto.getName(),
          dto.getForm(),
          dto.getStrength(),
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to create medicine"
      );
    }
  }


  /**
   * Fetches a paginated list of active medicines from the medicine catalog.
   *
   * <p>
   * The result is sorted by:
   * <ul>
   *     <li><b>createdAtMs</b> in descending order (latest medicines first)</li>
   *     <li><b>name</b> in ascending alphabetical order</li>
   * </ul>
   *
   * <p>
   * This method supports large datasets using pagination to ensure
   * optimal performance and reduced memory usage.
   *
   * @param pageNo   the page number to retrieve (0-based index)
   * @param pageSize the number of records per page
   * @return {@link ResponseEntity} containing paginated medicine data
   *         with success or error response
   */
  @Override
  public ResponseEntity<?> getAllMedicine(Integer pageNo, Integer pageSize) {

    logger.info("Fetching medicine catalog list. pageNo: {}, pageSize: {}", pageNo, pageSize);

    try {

      // Defensive defaults for pagination
      if (pageNo == null || pageNo < 0) {
        pageNo = 0;
      }

      if (pageSize == null || pageSize <= 0) {
        pageSize = 10;
      }

      Pageable pageable = PageRequest.of(
          pageNo,
          pageSize,
          Sort.by(
              Sort.Order.desc("createdAtMs"),
              Sort.Order.asc("name")
          )
      );

      Page<MedicineCatalog> medicineCatalogPage =
          medicineCatalogRepo.findByIsActiveTrue(pageable);

      logger.info(
          "Successfully fetched medicines. pageNo: {}, pageSize: {}, totalElements: {}",
          pageNo,
          pageSize,
          medicineCatalogPage.getTotalElements()
      );

      return baseResponse.successResponse(
          "Medicine list fetched successfully",
          DbUtill.buildPaginatedResponse(
              medicineCatalogPage,
              medicineCatalogPage.getContent()
          )
      );

    } catch (Exception e) {

      logger.error(
          "Error occurred while fetching medicine catalog list. pageNo: {}, pageSize: {}",
          pageNo,
          pageSize,
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch medicine list"
      );
    }
  }

  /**
   * Retrieves a paginated list of active medicines filtered by name.
   *
   * <p>
   * This method performs a case-insensitive partial search on medicine names
   * and returns results in a paginated format.
   * </p>
   *
   * <p>
   * Sorting order:
   * <ul>
   *     <li><b>createdAtMs</b> in descending order (latest first)</li>
   *     <li><b>name</b> in ascending alphabetical order</li>
   * </ul>
   *
   * <p>
   * Default pagination behavior:
   * <ul>
   *     <li>If pageNo is null or negative, defaults to <b>0</b></li>
   *     <li>If pageSize is null or less than or equal to 0, defaults to <b>10</b></li>
   * </ul>
   *
   * @param name     the medicine name (partial or full) used for searching
   * @param pageNo   the page number to retrieve (0-based index)
   * @param pageSize the number of records per page
   * @return {@link ResponseEntity} containing paginated medicine data
   */
  @Override
  public ResponseEntity<?> getMedicineByName(String name, Integer pageNo, Integer pageSize) {

    logger.info(
        "Fetching medicine by name. searchKeyword: '{}', pageNo: {}, pageSize: {}",
        name,
        pageNo,
        pageSize
    );

    try {

      // Validate search input
      if (name == null || name.trim().isEmpty()) {

        logger.warn("Medicine search failed: name parameter is null or empty");

        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Medicine name must not be empty"
        );
      }

      // Defensive defaults for pagination
      if (pageNo == null || pageNo < 0) {
        pageNo = 0;
      }

      if (pageSize == null || pageSize <= 0) {
        pageSize = 10;
      }

      Pageable pageable = PageRequest.of(
          pageNo,
          pageSize,
          Sort.by(
              Sort.Order.desc("createdAtMs"),
              Sort.Order.asc("name")
          )
      );

      Page<MedicineCatalog> medicineCatalogPage =
          medicineCatalogRepo
              .findByIsActiveTrueAndNameContainingIgnoreCase(
                  name.trim(),
                  pageable
              );

      logger.info(
          "Successfully fetched medicines by name. keyword: '{}', totalElements: {}, pageNo: {}, pageSize: {}",
          name,
          medicineCatalogPage.getTotalElements(),
          pageNo,
          pageSize
      );

      return baseResponse.successResponse(
          "Medicine list fetched successfully",
          DbUtill.buildPaginatedResponse(
              medicineCatalogPage,
              medicineCatalogPage.getContent()
          )
      );

    } catch (Exception e) {

      logger.error(
          "Error occurred while fetching medicine by name. keyword: '{}', pageNo: {}, pageSize: {}",
          name,
          pageNo,
          pageSize,
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch medicine list"
      );
    }
  }

  /**
   * Creates a new Test Catalog entry.
   * This method validates duplicate records based on:
   * name + category + typeId.
   * If a duplicate exists, the request is rejected.
   *
   * @param dto the request payload containing test catalog details
   * @return ResponseEntity containing success or error response
   */
  @Override
  public ResponseEntity<?> createTestCatalog(CreateTestCatalogRequestDto dto) {

    logger.info(
        "Creating new test catalog entry. name: {}, category: {}, typeId: {}",
        dto.getName(),
        dto.getCategory(),
        dto.getTypeId()
    );

    try {

      // Check duplicate record
      boolean exists = testCatalogRepo
          .existsByNameIgnoreCaseAndCategoryIgnoreCaseAndTypeId(
              dto.getName().trim(),
              dto.getCategory().trim(),
              dto.getTypeId()
          );

      if (exists) {

        logger.warn(
            "Duplicate test catalog entry attempt detected. name: {}, category: {}, typeId: {}",
            dto.getName(),
            dto.getCategory(),
            dto.getTypeId()
        );

        return baseResponse.errorResponse(
            HttpStatus.CONFLICT,
            "Test already exists with the same name, category, and type"
        );
      }

      // Create entity
      TestCatalog testCatalog = new TestCatalog();

      testCatalog.setName(dto.getName().trim());
      testCatalog.setCategory(dto.getCategory().trim());
      testCatalog.setTypeId(dto.getTypeId());

      // Save entity
      TestCatalog savedTestCatalog = testCatalogRepo.save(testCatalog);

      logger.info(
          "Test catalog created successfully. id: {}, name: {}",
          savedTestCatalog.getId(),
          savedTestCatalog.getName()
      );

      return baseResponse.successResponse(
          "Test catalog created successfully",
          savedTestCatalog.getId()
      );

    } catch (Exception e) {

      logger.error(
          "Error occurred while creating test catalog. name: {}, category: {}, typeId: {}",
          dto.getName(),
          dto.getCategory(),
          dto.getTypeId(),
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to create test catalog. Please try again later"
      );
    }
  }

  /**
   * Retrieves a paginated list of active Test Catalog entries.
   * This method fetches all records where isActive = true,
   * applies pagination with sorting by:
   *   1) createdAtMs (descending)
   *   2) name (ascending)
   * Defensive pagination defaults:
   *   - pageNo defaults to 0 if null or negative
   *   - pageSize defaults to 10 if null or invalid
   *
   * @param pageNo the page number (0-based index)
   * @param pageSize the number of records per page
   * @return ResponseEntity containing paginated list of active test catalog records
   */
  @Override
  public ResponseEntity<?> getAllTest(Long typeId, Integer pageNo, Integer pageSize) {

    logger.info(
        "Fetching test catalog list. pageNo: {}, pageSize: {}",
        pageNo,
        pageSize
    );

    try {

      // Defensive defaults for pagination
      if (pageNo == null || pageNo < 0) {
        pageNo = 0;
      }

      if (pageSize == null || pageSize <= 0) {
        pageSize = 10;
      }

      Pageable pageable = PageRequest.of(
          pageNo,
          pageSize,
          Sort.by(
              Sort.Order.desc("createdAtMs"),
              Sort.Order.asc("name")
          )
      );

      Page<TestCatalog> testCatalogPage = null;

      if (typeId == null) {
        testCatalogPage = testCatalogRepo.findByIsActiveTrue(pageable);
      } else {
        testCatalogPage = testCatalogRepo.findByIsActiveTrueAndTypeId(typeId, pageable);
      }

      logger.info(
          "Successfully fetched test catalog list. pageNo: {}, pageSize: {}, totalElements: {}, totalPages: {}",
          pageNo,
          pageSize,
          testCatalogPage.getTotalElements(),
          testCatalogPage.getTotalPages()
      );

      return baseResponse.successResponse(
          "Test catalog list fetched successfully",
          DbUtill.buildPaginatedResponse(
              testCatalogPage,
              testCatalogPage.getContent()
          )
      );

    } catch (Exception e) {

      logger.error(
          "Error occurred while fetching test catalog list. pageNo: {}, pageSize: {}",
          pageNo,
          pageSize,
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch test catalog list"
      );
    }
  }

  /**
   * Retrieves a paginated list of active Test Catalog records filtered by name,
   * and optionally by typeId.
   * Behavior:
   * - Searches active records (isActive = true)
   * - Performs case-insensitive search using the provided name
   * - If typeId is provided, results are filtered by typeId
   * - Applies pagination and sorting
   * Sorting Order:
   * 1) createdAtMs (descending)
   * 2) name (ascending)
   * Defensive Pagination Defaults:
   * - pageNo defaults to 0 if null or negative
   * - pageSize defaults to 10 if null or invalid
   * Validation:
   * - name must not be null or empty
   *
   * @param typeId   optional type identifier to filter test catalog records
   * @param name     the search keyword for test name (case-insensitive)
   * @param pageNo   the page number (0-based index)
   * @param pageSize the number of records per page
   * @return ResponseEntity containing paginated search results
   */
  @Override
  public ResponseEntity<?> getTestByName(
      Long typeId,
      String name,
      Integer pageNo,
      Integer pageSize
  ) {

    logger.info(
        "Fetching test catalog by name. keyword: '{}', typeId: {}, pageNo: {}, pageSize: {}",
        name,
        typeId,
        pageNo,
        pageSize
    );

    try {

      // Validate search input
      if (name == null || name.trim().isEmpty()) {

        logger.warn(
            "Test catalog search failed: name parameter is null or empty"
        );

        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Test name must not be empty"
        );
      }

      // Defensive defaults for pagination
      if (pageNo == null || pageNo < 0) {
        pageNo = 0;
      }

      if (pageSize == null || pageSize <= 0) {
        pageSize = 10;
      }

      Pageable pageable = PageRequest.of(
          pageNo,
          pageSize,
          Sort.by(
              Sort.Order.desc("createdAtMs"),
              Sort.Order.asc("name")
          )
      );

      Page<TestCatalog> testCatalogPage;

      // Conditional search based on typeId
      if (typeId == null) {

        logger.info(
            "Searching test catalog by name only. keyword: '{}'",
            name
        );

        testCatalogPage = testCatalogRepo
            .findByIsActiveTrueAndNameContainingIgnoreCase(
                name.trim(),
                pageable
            );

      } else {

        logger.info(
            "Searching test catalog by typeId and name. typeId: {}, keyword: '{}'",
            typeId,
            name
        );

        testCatalogPage = testCatalogRepo
            .findByIsActiveTrueAndTypeIdAndNameContainingIgnoreCase(
                typeId,
                name.trim(),
                pageable
            );
      }

      logger.info(
          "Successfully fetched test catalog search results. keyword: '{}', typeId: {}, totalElements: {}, totalPages: {}",
          name,
          typeId,
          testCatalogPage.getTotalElements(),
          testCatalogPage.getTotalPages()
      );

      return baseResponse.successResponse(
          "Test catalog search results fetched successfully",
          DbUtill.buildPaginatedResponse(
              testCatalogPage,
              testCatalogPage.getContent()
          )
      );

    } catch (Exception e) {

      logger.error(
          "Error occurred while searching test catalog. keyword: '{}', typeId: {}, pageNo: {}, pageSize: {}",
          name,
          typeId,
          pageNo,
          pageSize,
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch test catalog search results"
      );
    }
  }

  /**
   * Creates a new Test Type.
   *
   * <p>This method validates whether a test type with the same name
   * already exists (case-insensitive and active). If it exists, a conflict
   * response is returned. Otherwise, the test type is created and saved.</p>
   *
   * @param dto the request DTO containing test type details
   * @return ResponseEntity containing success or error response
   */
  @Override
  public ResponseEntity<?> createTestType(CreateTestTypeRequestDto dto) {

    logger.info("Create TestType request received with name: {}", dto.getName());

    try {

      String testTypeName = dto.getName().trim();

      boolean exists = testTypeRepo
          .existsByNameIgnoreCaseAndIsActiveTrue(testTypeName);

      if (exists) {

        logger.warn("TestType creation failed. TestType already exists with name: {}",
            testTypeName);

        return baseResponse.errorResponse(
            HttpStatus.CONFLICT,
            "Test type already exists with name: " + testTypeName
        );
      }

      TestType testType = new TestType();
      testType.setName(testTypeName);
      testType.setDescription(dto.getDescription());

      testTypeRepo.save(testType);

      logger.info("TestType created successfully with name: {}", testTypeName);

      return baseResponse.successResponse(
          "Test type created successfully",
          testType
      );

    } catch (Exception e) {

      logger.error("Error occurred while creating TestType: {}", dto.getName(), e);

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to create test type. Please try again later."
      );
    }
  }


  /**
   * Retrieves all active Test Types with pagination.
   *
   * <p>This method applies defensive defaults for pagination parameters.
   * Results are sorted by:
   * <ul>
   *   <li>createdAtMs (descending)</li>
   *   <li>name (ascending)</li>
   * </ul>
   * Only active test types are returned.</p>
   *
   * @param pageNo   the page number (default: 0 if null or negative)
   * @param pageSize the number of records per page (default: 10 if null or invalid)
   * @return ResponseEntity containing paginated list of Test Types
   */
  @Override
  public ResponseEntity<?> getAllTestType(Integer pageNo, Integer pageSize) {

    logger.info("Fetching TestTypes with pageNo: {}, pageSize: {}", pageNo, pageSize);

    try {

      // Defensive defaults
      if (pageNo == null || pageNo < 0) {
        logger.warn("Invalid pageNo received: {}. Defaulting to 0", pageNo);
        pageNo = 0;
      }

      if (pageSize == null || pageSize <= 0) {
        logger.warn("Invalid pageSize received: {}. Defaulting to 10", pageSize);
        pageSize = 10;
      }

      Pageable pageable = PageRequest.of(
          pageNo,
          pageSize,
          Sort.by(
              Sort.Order.desc("createdAtMs"),
              Sort.Order.asc("name")
          )
      );

      Page<TestType> testTypePage =
          testTypeRepo.findByIsActiveTrue(pageable);

      logger.info("Successfully fetched {} TestTypes",
          testTypePage.getNumberOfElements());

      return baseResponse.successResponse(
          "Test types fetched successfully",
          DbUtill.buildPaginatedResponse(
              testTypePage,
              testTypePage.getContent()
          )
      );

    } catch (Exception e) {

      logger.error("Error occurred while fetching TestTypes", e);

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch test types. Please try again later."
      );
    }
  }

}
