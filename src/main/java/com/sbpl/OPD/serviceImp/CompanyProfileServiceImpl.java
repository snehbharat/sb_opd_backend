package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.Auth.service.UserService;
import com.sbpl.OPD.dto.company.request.CompanyProfileRequestDto;
import com.sbpl.OPD.dto.company.request.CompanyProfileUpdateDto;
import com.sbpl.OPD.dto.company.response.CompanyProfileResponseDto;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.CompanyProfileService;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.S3BucketStorageUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of Company Profile service.
 *
 * Handles healthcare company (tenant) creation,
 * update, retrieval, and deletion.
 *
 * One company can manage multiple clinics,
 * branches, departments, and doctors.
 *
 * @author Rahul Kumar
 */
@Service
public class CompanyProfileServiceImpl implements CompanyProfileService {

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private S3BucketStorageUtility s3BucketStorageUtility;

    private static final Logger logger =
            LoggerFactory.getLogger(CompanyProfileServiceImpl.class);

    @Override
    public ResponseEntity<?> createCompany(CompanyProfileRequestDto dto) {

        logger.info("Company creation request received [email={}, gstin={}]",
                dto.getCompanyEmail(), dto.getGstinNumber());

        try {

            // Get logged-in user
            User adminUser = userRepository.findById(DbUtill.getLoggedInUserId())
                    .orElse(null);
            if (adminUser ==null){
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND,"Admin user not found");
            }

            System.out.println("i m from looged user "+ adminUser);

            // Role validation
            if (adminUser.getRole() != UserRole.SUPER_ADMIN &&
                    adminUser.getRole() != UserRole.SAAS_ADMIN) {

                return baseResponse.errorResponse(
                        HttpStatus.FORBIDDEN,
                        "Only SUPER_ADMIN and ADMIN users can create company profiles"
                );
            }

            // Check if the user already has a company
            Optional<CompanyProfile> existingCompany = companyProfileRepository.findByUser(adminUser);
            if (existingCompany.isPresent()) {
                return baseResponse.errorResponse(
                        HttpStatus.CONFLICT,
                        "Admin user already has a company. Please update the existing company instead of creating a new one."
                );
            }

            // Email duplicate check
            if (companyProfileRepository.existsByEmail(dto.getCompanyEmail())) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Company email already exists"
                );
            }

            // GSTIN duplicate check
            if (dto.getGstinNumber() != null &&
                    companyProfileRepository.existsByGstinNumber(dto.getGstinNumber())) {

                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "GSTIN already registered"
                );
            }

            CompanyProfile company = getCompany(dto, adminUser);

            MultipartFile logo = dto.getCompanyLogo();

            if (logo != null && !logo.isEmpty()) {

                // Validate image type
                String contentType = logo.getContentType();

                if (contentType == null ||
                        !(contentType.equals("image/png") ||
                                contentType.equals("image/jpeg") ||
                                contentType.equals("image/jpg"))) {

                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Only PNG or JPG images are allowed"
                    );
                }

                // Optional: size validation (5MB max)
                if (logo.getSize() > 5 * 1024 * 1024) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Logo size must be less than 5MB"
                    );
                }

                String originalFileName = logo.getOriginalFilename();
                byte[] fileBytes = logo.getBytes();

                assert originalFileName != null;
                String s3Url = s3BucketStorageUtility
                        .uploadClinicLogo(originalFileName, fileBytes, contentType);

                company.setCompanyLogoUrl(s3Url);
            }

            CompanyProfile savedCompany = companyProfileRepository.save(company);

            userService.updateCompanyOnUser(adminUser.getId(), savedCompany.getId());

            logger.info("Company created successfully [companyId={}]", savedCompany.getId());

            return baseResponse.successResponse("Company created successfully");

        } catch (IllegalArgumentException e) {

            logger.warn("Company creation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {

            logger.error("Unexpected error while creating company", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create company at the moment"
            );
        }
    }

    @Override
    public ResponseEntity<?> updateCompany(Long companyId,
                                           CompanyProfileUpdateDto dto) {

        logger.info("Update request received for company [companyId={}]", companyId);

        try {
            User currentUser = userRepository.findById(DbUtill.getLoggedInUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (currentUser.getRole() != UserRole.SUPER_ADMIN &&
                    currentUser.getRole() != UserRole.SAAS_ADMIN) {

                return baseResponse.errorResponse(
                        HttpStatus.FORBIDDEN,
                        "Only SUPER_ADMIN and ADMIN users can update company profiles"
                );
            }

            CompanyProfile company = companyProfileRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));

            boolean isUpdated = false;

            if (dto.getCompanyName() != null) {
                company.setCompanyName(dto.getCompanyName());
                isUpdated = true;
            }

            if (dto.getAddress() != null) {
                company.setAddress(dto.getAddress());
                isUpdated = true;
            }

            if (dto.getDlNo() != null) {
                company.setDlNo(dto.getDlNo());
                isUpdated = true;
            }

            if (dto.getRegisteredOffice() != null) {
                company.setRegisteredOffice(dto.getRegisteredOffice());
                isUpdated = true;
            }

            if (dto.getCompanyPhone() != null) {
                company.setCompanyPhone(dto.getCompanyPhone());
                isUpdated = true;
            }

            if (dto.getCompanyAlternateNo() != null) {
                company.setCompanyAlternateNo(dto.getCompanyAlternateNo());
                isUpdated = true;
            }

            MultipartFile logo = dto.getCompanyLogo();

            if (logo != null && !logo.isEmpty()) {

                String contentType = logo.getContentType();

                if (contentType == null ||
                        !(contentType.equals("image/png") ||
                                contentType.equals("image/jpeg") ||
                                contentType.equals("image/jpg"))) {

                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Only PNG or JPG images are allowed"
                    );
                }

                if (logo.getSize() > 5 * 1024 * 1024) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Logo size must be less than 5MB"
                    );
                }

                if (company.getCompanyLogoUrl() != null) {
                    String oldFileName = extractFileNameFromUrl(company.getCompanyLogoUrl());
                    if (oldFileName != null) {
                        s3BucketStorageUtility.deleteFile(oldFileName);
                    }
                }

                String originalFileName = logo.getOriginalFilename();
                byte[] fileBytes = logo.getBytes();

                String s3Url = s3BucketStorageUtility
                        .uploadClinicLogo(originalFileName, fileBytes, contentType);

                company.setCompanyLogoUrl(s3Url);
                isUpdated = true;
            }

            if (!isUpdated) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "No fields provided for update"
                );
            }

            company.setUpdatedAt(new Date());

            companyProfileRepository.save(company);

            logger.info("Company updated successfully [companyId={}]", companyId);

            return baseResponse.successResponse("Company updated successfully");

        } catch (IllegalArgumentException e) {

            logger.warn("Update failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {

            logger.error("Error updating company [companyId={}]", companyId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update company"
            );
        }
    }

    @Override
    public ResponseEntity<?> getCompanyById(Long companyId) {

        logger.info("Fetching company [companyId={}]", companyId);

        try {
            CompanyProfile company = companyProfileRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));

            // Check if user has permission to view this company
            User currentUser = userRepository.findById(DbUtill.getLoggedInUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Check if user is associated with a company
            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(
                        HttpStatus.FORBIDDEN,
                        "User is not associated with any company"
                );
            }

            // Verify that the requested company ID matches the user's company
            if (!currentUser.getCompany().getId().equals(companyId)) {
                return baseResponse.errorResponse(
                        HttpStatus.FORBIDDEN,
                        "User does not have permission to access this company"
                );
            }

            return baseResponse.successResponse(
                    "Company fetched successfully",
                    convertToResponseDto(company)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching company [companyId={}]", companyId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch company"
            );
        }
    }



    @Override
    public ResponseEntity<?> getCompanyByEmail(String email) {
        logger.info("Fetching company by email [email={}]", email);

        try {
            Optional<CompanyProfile> companyOpt = companyProfileRepository.findByEmail(email);
            
            if (companyOpt.isEmpty()) {
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Company not found with email: " + email
                );
            }

            return baseResponse.successResponse(
                    "Company fetched successfully",
                    convertToResponseDto(companyOpt.get())
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching company by email [email={}]", email, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch company by email"
            );
        }
    }

    @Override
    public ResponseEntity<?> getCompanyByGstin(String gstin) {
        logger.info("Fetching company by GSTIN [gstin={}]", gstin);

        try {
            Optional<CompanyProfile> companyOpt = companyProfileRepository.findByGstinNumber(gstin);
            
            if (companyOpt.isEmpty()) {
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Company not found with GSTIN: " + gstin
                );
            }

            return baseResponse.successResponse(
                    "Company fetched successfully",
                    convertToResponseDto(companyOpt.get())
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching company by GSTIN [gstin={}]", gstin, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch company by GSTIN"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllCompanies(Integer pageNo, Integer pageSize) {

        logger.info("Fetching all companies [pageNo={}, pageSize={}]", pageNo, pageSize);

        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithoutSort(pageNo, pageSize);

            Page<CompanyProfile> page =
                    companyProfileRepository.findAll(pageRequest);

            List<CompanyProfileResponseDto> dtoList =
                    page.getContent().stream()
                            .map(this::convertToResponseDto)
                            .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Companies fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching companies", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch companies"
            );
        }
    }

    @Override
    public ResponseEntity<?> deleteCompany(Long companyId) {

        logger.info("Delete request received [companyId={}]", companyId);

        try {

            CompanyProfile company = companyProfileRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));

            if (company.getCompanyLogoUrl() != null) {
                String fileName = extractFileNameFromUrl(company.getCompanyLogoUrl());
                if (fileName != null) {
                    s3BucketStorageUtility.deleteFile(fileName);
                }
            }

            company.setActive(false);
            company.setUpdatedAt(new Date());
            companyProfileRepository.save(company);

            return baseResponse.successResponse("Company deactivated successfully");

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error deleting company [companyId={}]", companyId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete company"
            );
        }
    }

    private CompanyProfileResponseDto convertToResponseDto(CompanyProfile company) {

        CompanyProfileResponseDto dto = new CompanyProfileResponseDto();
        dto.setCompanyId(company.getId());
        dto.setCompanyName(company.getCompanyName());
        dto.setCompanyLogoUrl(company.getCompanyLogoUrl());
        dto.setCompanyUrl(company.getCompanyUrl());
        dto.setAddress(company.getAddress());
        dto.setGstinNumber(company.getGstinNumber());
        dto.setCinNumber(company.getCinNumber());
        dto.setDlNo(company.getDlNo());
        dto.setRegisteredOffice(company.getRegisteredOffice());
        dto.setCompanyPhone(company.getCompanyPhone());
        dto.setCompanyEmail(company.getCompanyEmail());
        dto.setCompanyAlternateNo(company.getCompanyAlternateNo());
        dto.setAdminUserId(company.getUser().getId());
        dto.setActive(company.getActive());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setUpdatedAt(company.getUpdatedAt());

        // Set base64 logo data if logo URL exists
        if (company.getCompanyLogoUrl() != null) {
            String fileName = extractFileNameFromUrl(company.getCompanyLogoUrl());
            if (fileName != null) {
                String base64Data = s3BucketStorageUtility.getFileAsBase64(fileName);
                dto.setLogoBase64(base64Data);
            }
        }

        return dto;
    }

    private static CompanyProfile getCompany(CompanyProfileRequestDto dto, User adminUser) {
        CompanyProfile company = new CompanyProfile();
        company.setCompanyName(dto.getCompanyName());
//        company.setCompanyLogoUrl(dto.getCompanyLogo());
//        company.setCompanyUrl(dto.getCompanyUrl());
        company.setAddress(dto.getAddress());
        company.setGstinNumber(dto.getGstinNumber());
        company.setCinNumber(dto.getCinNumber());
        company.setDlNo(dto.getDlNo());
        company.setRegisteredOffice(dto.getRegisteredOffice());
        company.setCompanyPhone(dto.getCompanyPhone());
        company.setCompanyEmail(dto.getCompanyEmail());
        company.setCompanyAlternateNo(dto.getCompanyAlternateNo());
        company.setUser(adminUser);
        company.setCreatedAt(new Date());
        company.setActive(true);
        return company;
    }

    // Helper method to detect if file is an image
    private boolean isImageFile(byte[] fileContent) {
        if (fileContent.length < 4) return false;
        // Check for common image file signatures
        // JPEG: FF D8 FF
        if (fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xD8 && fileContent[2] == (byte) 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if (fileContent[0] == (byte) 0x89 && fileContent[1] == (byte) 0x50 && fileContent[2] == (byte) 0x4E && fileContent[3] == (byte) 0x47) {
            return true;
        }
        // GIF: 47 49 46 38
        if (fileContent[0] == (byte) 0x47 && fileContent[1] == (byte) 0x49 && fileContent[2] == (byte) 0x46 && fileContent[3] == (byte) 0x38) {
            return true;
        }
        // BMP: 42 4D
        if (fileContent[0] == (byte) 0x42 && fileContent[1] == (byte) 0x4D) {
            return true;
        }
        return false;
    }

    // Helper method to get content type for images
    private String getImageContentType(byte[] fileContent) {
        if (fileContent.length < 4) return "image/png";
        // JPEG: FF D8 FF
        if (fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xD8 && fileContent[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        // PNG: 89 50 4E 47
        if (fileContent[0] == (byte) 0x89 && fileContent[1] == (byte) 0x50 && fileContent[2] == (byte) 0x4E && fileContent[3] == (byte) 0x47) {
            return "image/png";
        }
        // GIF: 47 49 46 38
        if (fileContent[0] == (byte) 0x47 && fileContent[1] == (byte) 0x49 && fileContent[2] == (byte) 0x46 && fileContent[3] == (byte) 0x38) {
            return "image/gif";
        }
        // BMP: 42 4D
        if (fileContent[0] == (byte) 0x42 && fileContent[1] == (byte) 0x4D) {
            return "image/bmp";
        }
        return "image/png";
    }

    // Extract file name from S3 URL
    private String extractFileNameFromUrl(String url) {
        if (url == null) return null;
        try {
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
                return url.substring(lastSlashIndex + 1);
            }
        } catch (Exception e) {
            logger.warn("Error extracting filename from URL: {}", url, e);
        }
        return null;
    }
}