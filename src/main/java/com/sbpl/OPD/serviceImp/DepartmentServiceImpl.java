package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.department.DepartmentRequestDto;
import com.sbpl.OPD.dto.department.DepartmentResponseDto;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Department;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.DepartmentRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.DepartmentService;
import com.sbpl.OPD.utils.DbUtill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of Department service.
 *
 * Handles department creation, update, retrieval, and deletion.
 * Departments are managed under branch entities in the hierarchical structure.
 *
 * @author Rahul Kumar
 */
@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Override
    @Transactional
    public ResponseEntity<?> createDepartment(DepartmentRequestDto dto) {
        log.info("Department creation request received [name={}, branchId={}]", 
                dto.getDepartmentName(), dto.getBranchId());

        try {
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            Optional<Department> existingDepartment = departmentRepository.findByDepartmentNameAndBranch_Id(
                    dto.getDepartmentName(), dto.getBranchId());
            if (existingDepartment.isPresent()) {
                return baseResponse.errorResponse(
                        HttpStatus.CONFLICT,
                        "Department with name '" + dto.getDepartmentName() + "' already exists for this branch"
                );
            }

            Department department = new Department();
            department.setDepartmentName(dto.getDepartmentName());
            department.setBranch(branch);
            department.setDescription(dto.getDescription());

            Department savedDepartment = departmentRepository.save(department);

            log.info("Department created successfully [id={}, name={}]", 
                    savedDepartment.getId(), savedDepartment.getDepartmentName());

            return baseResponse.successResponse(
                    "Department created successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error creating department [name={}]", dto.getDepartmentName(), e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create department"
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateDepartment(Long departmentId, DepartmentRequestDto dto) {
        log.info("Department update request received [id={}, name={}]", 
                departmentId, dto.getDepartmentName());

        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            if (!department.getDepartmentName().equals(dto.getDepartmentName())) {
                Optional<Department> existingDepartment = departmentRepository.findByDepartmentNameAndBranch_Id(
                        dto.getDepartmentName(), dto.getBranchId());
                if (existingDepartment.isPresent() && !existingDepartment.get().getId().equals(departmentId)) {
                    return baseResponse.errorResponse(
                            HttpStatus.CONFLICT,
                            "Department with name '" + dto.getDepartmentName() + "' already exists for this branch"
                    );
                }
            }

            department.setDepartmentName(dto.getDepartmentName());
            department.setBranch(branch);
            department.setDescription(dto.getDescription());
            Department updatedDepartment = departmentRepository.saveAndFlush(department);

            log.info("Department updated successfully [id={}, name={}]", 
                    updatedDepartment.getId(), updatedDepartment.getDepartmentName());

            return baseResponse.successResponse(
                    "Department updated successfully",
                    convertToResponseDto(updatedDepartment)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error updating department [id={}]", departmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update department"
            );
        }
    }

    @Override
    public ResponseEntity<?> getDepartmentById(Long departmentId) {
        log.info("Fetching department [id={}]", departmentId);

        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            return baseResponse.successResponse(
                    "Department fetched successfully",
                    convertToResponseDto(department)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching department [id={}]", departmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch department"
            );
        }
    }

    @Override
    public ResponseEntity<?> getDepartmentsByBranch(Long branchId) {
        log.info("Fetching departments for branch [id={}]", branchId);

        try {
            List<Department> departments = departmentRepository.findByBranch_IdOrderByCreatedAtDesc(branchId);

            List<DepartmentResponseDto> dtoList = departments.stream()
                    .map(this::convertToResponseDto)
                    .toList();

            return baseResponse.successResponse(
                    "Departments fetched successfully",
                    dtoList
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching departments for branch [id={}]", branchId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch departments for branch"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllDepartments(Integer pageNo, Integer pageSize) {
        log.info("Fetching all departments [pageNo={}, pageSize={}]", pageNo, pageSize);

        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo,pageSize);

            Page<Department> departmentPage = departmentRepository.findAllByOrderByCreatedAtDesc(pageRequest);

            List<DepartmentResponseDto> dtoList = departmentPage.getContent().stream()
                    .map(this::convertToResponseDto)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(departmentPage, dtoList);

            return baseResponse.successResponse(
                    "Departments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            log.error("Error fetching departments", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch departments"
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteDepartment(Long departmentId) {
        log.info("Delete request received [id={}]", departmentId);

        try {

            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            departmentRepository.delete(department);

            log.info("Department deleted successfully [id={}]", departmentId);

            return baseResponse.successResponse("Department deleted successfully");

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Error deleting department [id={}]", departmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete department"
            );
        }
    }

    private DepartmentResponseDto convertToResponseDto(Department department) {
        Branch branch = department.getBranch();
        CompanyProfile clinic = (branch != null) ? branch.getClinic() : null;
        return DepartmentResponseDto.builder()
                .id(department.getId())
                .departmentName(department.getDepartmentName())
                .description(department.getDescription())
                .branchId(branch != null ? branch.getId() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .clinicId(clinic != null ? clinic.getId() : null)
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}