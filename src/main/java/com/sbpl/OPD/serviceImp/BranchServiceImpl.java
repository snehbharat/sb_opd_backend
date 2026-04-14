package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.branch.BranchRequestDto;
import com.sbpl.OPD.dto.branch.BranchResponseDto;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.BranchService;
import com.sbpl.OPD.utils.DbUtill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of Branch service.
 *
 * Handles branch creation, update, retrieval, and deletion.
 * Branches are managed under clinic entities in the hierarchical structure.
 *
 * @author Rahul Kumar
 */
@Service
@Slf4j
public class BranchServiceImpl implements BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CompanyProfileRepository clinicRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Override
    public ResponseEntity<?> createBranch(BranchRequestDto dto) {
        log.info("Branch creation request received [name={}, clinicId={}]", 
                dto.getBranchName(), dto.getClinicId());

        try {
            CompanyProfile clinic = clinicRepository.findById(dto.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Clinic not found"));

            Optional<Branch> existingBranch = branchRepository.findByBranchNameAndClinic_Id(
                    dto.getBranchName(), dto.getClinicId());
            if (existingBranch.isPresent()) {
                return baseResponse.errorResponse(
                        HttpStatus.CONFLICT,
                        "Branch with name '" + dto.getBranchName() + "' already exists for this clinic"
                );
            }

            Branch branch = new Branch();
            branch.setBranchName(dto.getBranchName());
            branch.setAddress(dto.getAddress());
            branch.setClinic(clinic);
            branch.setEmail(dto.getEmail());
            branch.setPhoneNumber(dto.getPhoneNumber());
            branch.setEstablishedDate(dto.getEstablishedDate());

            Branch savedBranch = branchRepository.save(branch);

            log.info("Branch created successfully [id={}, name={}]", 
                    savedBranch.getId(), savedBranch.getBranchName());

            return baseResponse.successResponse(
                    "Branch created successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error creating branch [name={}]", dto.getBranchName(), e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create branch"
            );
        }
    }

    @Override
    public ResponseEntity<?> updateBranch(Long branchId, BranchRequestDto dto) {
        log.info("Branch update request received [id={}, name={}]", 
                branchId, dto.getBranchName());

        try {

            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            CompanyProfile clinic = clinicRepository.findById(dto.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Clinic not found"));

            if (!branch.getBranchName().equals(dto.getBranchName())) {
                Optional<Branch> existingBranch = branchRepository.findByBranchNameAndClinic_Id(
                        dto.getBranchName(), dto.getClinicId());
                if (existingBranch.isPresent() && !existingBranch.get().getId().equals(branchId)) {
                    return baseResponse.errorResponse(
                            HttpStatus.CONFLICT,
                            "Branch with name '" + dto.getBranchName() + "' already exists for this clinic"
                    );
                }
            }

            branch.setBranchName(dto.getBranchName());
            branch.setAddress(dto.getAddress());
            branch.setClinic(clinic);

            branch.setEstablishedDate(dto.getEstablishedDate());
            branch.setPhoneNumber(dto.getPhoneNumber());
            branch.setEmail(dto.getEmail());

            Branch updatedBranch = branchRepository.save(branch);

            log.info("Branch updated successfully [id={}, name={}]", 
                    updatedBranch.getId(), updatedBranch.getBranchName());

            return baseResponse.successResponse(
                    "Branch updated successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error updating branch [id={}]", branchId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update branch"
            );
        }
    }

    @Override
    public ResponseEntity<?> getBranchById(Long branchId) {
        log.info("Fetching branch [id={}]", branchId);

        try {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            return baseResponse.successResponse(
                    "Branch fetched successfully",
                    convertToResponseDto(branch)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching branch [id={}]", branchId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch branch"
            );
        }
    }

    @Override
    public ResponseEntity<?> getBranchByIdWithAccessControl(Long branchId) {
        log.info("Fetching branch with access control [id={}]", branchId);

        try {
            User currentUser = DbUtill.getCurrentUser();
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            // Validate access based on user role
            if (!isBranchAccessibleToUser(branch, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access this branch");
            }

            return baseResponse.successResponse(
                    "Branch fetched successfully",
                    convertToResponseDto(branch)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching branch with access control [id={}]", branchId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch branch"
            );
        }
    }

    @Override
    public ResponseEntity<?> getBranchesByClinic(Long clinicId) {
        log.info("Fetching branches for clinic [id={}]", clinicId);

        try {
            CompanyProfile clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new IllegalArgumentException("Clinic not found"));

            List<Branch> branches = branchRepository.findByClinic_Id(clinicId);

            branches.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));

            List<BranchResponseDto> dtoList = branches.stream()
                    .map(this::convertToResponseDto)
                    .toList();

            return baseResponse.successResponse(
                    "Branches fetched successfully",
                    dtoList
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching branches for clinic [id={}]", clinicId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch branches for clinic"
            );
        }
    }

    @Override
    public ResponseEntity<?> getBranchesByCompanyId(Long companyId, Integer pageNo, Integer pageSize) {
        log.info("Fetching branches for company [id={}, pageNo={}, pageSize={}]", companyId, pageNo, pageSize);

        try {
            User currentUser = DbUtill.getCurrentUser();
            
            // Validate that user can access this company
            if (!canUserAccessCompany(companyId, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access branches for this company");
            }

            CompanyProfile company = clinicRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));

            Pageable pageable = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<Branch> branchPage = branchRepository.findByClinic(company, pageable);

            List<BranchResponseDto> dtoList = branchPage.getContent().stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(branchPage, dtoList);

            return baseResponse.successResponse(
                    "Branches fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching branches for company [id={}]", companyId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch branches for company"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllBranches(Integer pageNo, Integer pageSize) {
        log.info("Fetching all branches with access control [pageNo={}, pageSize={}]", pageNo, pageSize);

        try {
            User currentUser = DbUtill.getCurrentUser();
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            
            Page<Branch> branchPage;
            
            if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                if (currentUser.getCompany() != null) {
                    branchPage = branchRepository.findByClinic(currentUser.getCompany(), pageable);
                } else {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
                }
            } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                if (currentUser.getBranch() != null) {
                    Branch userBranch = currentUser.getBranch();
                    branchPage = branchRepository.findByIdAndClinic_id(userBranch.getId(), userBranch.getClinic().getId(), pageable);
                } else {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
                }
            } else if (currentUser.getRole() == UserRole.DOCTOR || 
                      currentUser.getRole() == UserRole.RECEPTIONIST || 
                      currentUser.getRole() == UserRole.STAFF ||
                      currentUser.getRole() == UserRole.BILLING_STAFF) {
                if (currentUser.getBranch() != null) {
                    Branch userBranch = currentUser.getBranch();
                    branchPage = branchRepository.findByIdAndClinic_id(userBranch.getId(), userBranch.getClinic().getId(), pageable);
                } else {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
                }
            } else {
                branchPage = branchRepository.findAllByOrderByCreatedAtDesc(pageable);
            }

            List<BranchResponseDto> dtoList = branchPage.getContent().stream()
                    .filter(branch -> isBranchAccessibleToUser(branch, currentUser))
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(branchPage, dtoList);

            return baseResponse.successResponse(
                    "Branches fetched successfully",
                    response
            );

        } catch (Exception e) {
            log.error("Error fetching branches with access control", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch branches"
            );
        }
    }

    @Override
    public ResponseEntity<?> deleteBranch(Long branchId) {
        log.info("Delete request received [id={}]", branchId);

        try {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            branchRepository.delete(branch);

            log.info("Branch deleted successfully [id={}]", branchId);

            return baseResponse.successResponse("Branch deleted successfully");

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error deleting branch [id={}]", branchId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete branch"
            );
        }
    }

    private BranchResponseDto convertToResponseDto(Branch branch) {
        CompanyProfile clinic = branch.getClinic();
        return BranchResponseDto.builder()
                .id(branch.getId())
                .branchName(branch.getBranchName())
                .establishedDate(branch.getEstablishedDate())
                .email(branch.getEmail())
                .phoneNumber(branch.getPhoneNumber())
                .address(branch.getAddress())
                .clinicId(clinic != null ? clinic.getId() : null)
                .clinicName(clinic != null ? clinic.getCompanyName() : null)
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
    
    /**
     * Check if a user can access a specific branch based on their role
     */
    private boolean isBranchAccessibleToUser(Branch branch, User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            // Super Admin can access all branches
            return true;
        }
        
        if (user.getRole() == UserRole.SAAS_ADMIN || user.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
            // SaaS Admin can access branches in their company
            return user.getCompany() != null && 
                   branch.getClinic() != null && 
                   user.getCompany().getId().equals(branch.getClinic().getId());
        }
        
        if (user.getRole() == UserRole.BRANCH_MANAGER) {
            // Branch Manager can only access their assigned branch
            return user.getBranch() != null && 
                   user.getBranch().getId().equals(branch.getId());
        }
        
        if (user.getRole() == UserRole.DOCTOR || 
            user.getRole() == UserRole.RECEPTIONIST || 
            user.getRole() == UserRole.BILLING_STAFF) {
            // Lower roles can only access their assigned branch
            return user.getBranch() != null && 
                   user.getBranch().getId().equals(branch.getId());
        }
        
        return false;
    }
    
    /**
     * Check if a user can access a specific company
     */
    private boolean canUserAccessCompany(Long companyId, User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            // Super Admin can access all companies
            return true;
        }
        
        if (user.getRole() == UserRole.SAAS_ADMIN || user.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
            // SaaS Admin can access their assigned company
            return user.getCompany() != null && 
                   user.getCompany().getId().equals(companyId);
        }
        
        // Other roles cannot access companies directly
        return false;
    }
}