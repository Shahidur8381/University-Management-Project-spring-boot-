package com.example.ums.unit;

import com.example.ums.controller.AdminController;
import com.example.ums.controller.StudentController;
import com.example.ums.model.Admin;
import com.example.ums.model.Student;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import com.example.ums.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UmsUnitTests {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    // Unit Test 1: Authentication lookup for Admin
    @Test
    @DisplayName("Unit Test 1: loadUserByUsername when admin exists returns UserDetails with ROLE_ADMIN")
    void loadUserByUsername_whenAdminExists_returnsUserDetailsWithAdminRole() {
        Admin admin = new Admin("admin1", "encoded_admin_pass", "Admin User", "admin1@test.com", "Admin Dept");
        when(adminRepository.findByUsername("admin1")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin1");

        assertNotNull(userDetails);
        assertEquals("admin1", userDetails.getUsername());
        assertEquals("encoded_admin_pass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(adminRepository).findByUsername("admin1");
        verifyNoInteractions(teacherRepository, studentRepository);
    }

    // Unit Test 2: Fallback lookup for Student when Admin and Teacher are not found
    @Test
    @DisplayName("Unit Test 2: loadUserByUsername resolves student in fallback order and returns ROLE_STUDENT")
    void loadUserByUsername_whenUserFoundInFallbackStudentRepo_returnsStudentUserDetails() {
        when(adminRepository.findByUsername("student1")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername("student1")).thenReturn(Optional.empty());
        Student student = new Student("student1", "encoded_student_pass", "Student User", "student1@test.com", "S-101", "CSE", 2);
        when(studentRepository.findByUsername("student1")).thenReturn(Optional.of(student));

        UserDetails userDetails = userDetailsService.loadUserByUsername("student1");

        assertNotNull(userDetails);
        assertEquals("student1", userDetails.getUsername());
        assertEquals("encoded_student_pass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        verify(adminRepository).findByUsername("student1");
        verify(teacherRepository).findByUsername("student1");
        verify(studentRepository).findByUsername("student1");
    }

    // Unit Test 3: Edge Case - User not found in any repository throws UsernameNotFoundException
    @Test
    @DisplayName("Unit Test 3: loadUserByUsername throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_whenUserDoesNotExistInAnyRepo_throwsUsernameNotFoundException() {
        when(adminRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());
        when(studentRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown_user")
        );

        assertTrue(exception.getMessage().contains("User not found with username: unknown_user"));
    }

    // Unit Test 4: AdminController createAdmin enforces ROLE_ADMIN and hashes raw password
    @Test
    @DisplayName("Unit Test 4: createAdmin enforces ROLE_ADMIN and hashes the raw password")
    void createAdmin_enforcesAdminRoleAndHashesPassword() {
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

    // Unit Test 5: StudentController updateStudent updates entity fields and hashes newly provided password
    @Test
    @DisplayName("Unit Test 5: updateStudent updates fields and hashes newly provided password")
    void updateStudent_whenNewPasswordProvided_hashesNewPasswordAndUpdatesFields() {
        StudentController studentController = new StudentController(studentRepository, passwordEncoder);

        Student existingStudent = new Student("john_doe", "old_hashed_pass", "John Doe", "john@test.com", "CSE-01", "CSE", 1);
        existingStudent.setId(15L);

        Student updatePayload = new Student();
        updatePayload.setName("Johnathan Doe");
        updatePayload.setEmail("johnathan@test.com");
        updatePayload.setDepartment("Software Engineering");
        updatePayload.setSemester(2);
        updatePayload.setStudentId("SE-01");
        updatePayload.setPassword("new_strong_pass");

        when(studentRepository.findById(15L)).thenReturn(Optional.of(existingStudent));
        when(passwordEncoder.encode("new_strong_pass")).thenReturn("new_bcrypt_hash");
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Student> response = studentController.updateStudent(15L, updatePayload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Johnathan Doe", response.getBody().getName());
        assertEquals("johnathan@test.com", response.getBody().getEmail());
        assertEquals("Software Engineering", response.getBody().getDepartment());
        assertEquals(2, response.getBody().getSemester());
        assertEquals("SE-01", response.getBody().getStudentId());
        assertEquals("new_bcrypt_hash", response.getBody().getPassword());
        verify(passwordEncoder).encode("new_strong_pass");
        verify(studentRepository).save(existingStudent);
    }

    // Unit Test 6: Edge Case - StudentController updateStudent retains existing password when new password is blank
    @Test
    @DisplayName("Unit Test 6: updateStudent retains existing password hash when new password is blank")
    void updateStudent_whenPasswordIsBlankOrNull_retainsExistingPasswordHash() {
        StudentController studentController = new StudentController(studentRepository, passwordEncoder);

        Student existingStudent = new Student("jane_doe", "original_secure_hash", "Jane Doe", "jane@test.com", "CSE-02", "CSE", 3);
        existingStudent.setId(25L);

        Student updatePayload = new Student();
        updatePayload.setName("Jane Smith");
        updatePayload.setEmail("jane.smith@test.com");
        updatePayload.setDepartment("CSE");
        updatePayload.setSemester(4);
        updatePayload.setStudentId("CSE-02");
        updatePayload.setPassword("   "); // blank password

        when(studentRepository.findById(25L)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Student> response = studentController.updateStudent(25L, updatePayload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Jane Smith", response.getBody().getName());
        assertEquals("original_secure_hash", response.getBody().getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(studentRepository).save(existingStudent);
    }
}
