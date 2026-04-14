package com.sbpl.OPD.Auth.service;

import com.sbpl.OPD.Auth.dto.LoginRequestDto;
import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.dto.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;


import java.util.List;

public interface UserService extends UserDetailsService {

    ResponseEntity<?> loginViaUsernameAndPassword(LoginRequestDto requestDto);

    User updateCompanyOnUser(Long userId, Long companyId);

    ResponseEntity<?> createUser1(UserDTO userDTO);

    ResponseEntity<?> createUser(UserDTO userDTO);
    ResponseEntity<?> updateUser(Long id, UserDTO userDTO);
    ResponseEntity<?> getUserById(Long id);
    List<UserDTO> getAllUsers();
    void deleteUser(Long id);
    UserDTO findByUsername(String username);
    ResponseEntity<?> getUsersByRole(UserRole role);
    ResponseEntity<?> getAllUsersWithCompanyAndRole(Long companyId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> getAllStaffByCompany(Long companyId, Integer pageNo, Integer pageSize);
    // Removed assignPermissions method for pure RBAC
}