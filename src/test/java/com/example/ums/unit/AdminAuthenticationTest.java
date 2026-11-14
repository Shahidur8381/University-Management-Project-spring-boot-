package com.example.ums.unit;

import com.example.ums.model.Admin;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import com.example.ums.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_whenAdminExists_returnsAdminRole() {
        Admin admin = new Admin(
                "admin1",
                "encoded_admin_pass",
                "Admin User",
                "admin1@test.com",
                "Admin Dept"
        );
        when(adminRepository.findByUsername("admin1")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin1");

        assertNotNull(userDetails);
        assertEquals("admin1", userDetails.getUsername());
        assertEquals("encoded_admin_pass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));
        verify(adminRepository).findByUsername("admin1");
        verifyNoInteractions(teacherRepository, studentRepository);
    }
}
