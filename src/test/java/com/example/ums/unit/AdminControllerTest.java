package com.example.ums.unit;

import com.example.ums.controller.AdminController;
import com.example.ums.model.Admin;
import com.example.ums.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createAdmin_hashesPasswordAndAssignsAdminRole() {
        AdminController adminController = new AdminController(adminRepository, passwordEncoder);

        Admin inputAdmin = new Admin();
        inputAdmin.setUsername("superadmin");
        inputAdmin.setPassword("raw_password123");
        inputAdmin.setName("Super Admin");
        inputAdmin.setEmail("superadmin@test.com");
        inputAdmin.setDepartment("Headquarters");

        when(passwordEncoder.encode("raw_password123")).thenReturn("hashed_bcrypt_secret");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Admin> response = adminController.createAdmin(inputAdmin);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ROLE_ADMIN", response.getBody().getRole());
        assertEquals("hashed_bcrypt_secret", response.getBody().getPassword());
        verify(passwordEncoder).encode("raw_password123");
        verify(adminRepository).save(any(Admin.class));
    }
}
