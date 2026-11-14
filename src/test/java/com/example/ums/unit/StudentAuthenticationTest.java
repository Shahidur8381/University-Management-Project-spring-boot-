package com.example.ums.unit;

import com.example.ums.model.Student;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAuthenticationTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_whenStudentExists_returnsStudentRole() {
        when(adminRepository.findByUsername("student1")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername("student1")).thenReturn(Optional.empty());

        Student student = new Student(
                "student1",
                "encoded_student_pass",
                "Student User",
                "student1@test.com",
                "S-101",
                "CSE",
                2
        );
        when(studentRepository.findByUsername("student1")).thenReturn(Optional.of(student));

        UserDetails userDetails = userDetailsService.loadUserByUsername("student1");

        assertNotNull(userDetails);
        assertEquals("student1", userDetails.getUsername());
        assertEquals("encoded_student_pass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_STUDENT")
        ));
        verify(adminRepository).findByUsername("student1");
        verify(teacherRepository).findByUsername("student1");
        verify(studentRepository).findByUsername("student1");
    }
}
