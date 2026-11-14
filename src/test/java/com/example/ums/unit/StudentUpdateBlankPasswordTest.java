package com.example.ums.unit;

import com.example.ums.controller.StudentController;
import com.example.ums.model.Student;
import com.example.ums.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentUpdateBlankPasswordTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void updateStudent_whenPasswordIsBlank_retainsExistingPassword() {
        StudentController studentController = new StudentController(studentRepository, passwordEncoder);

        Student existingStudent = new Student(
                "jane_doe",
                "original_secure_hash",
                "Jane Doe",
                "jane@test.com",
                "CSE-02",
                "CSE",
                3
        );
        existingStudent.setId(25L);

        Student updatePayload = new Student();
        updatePayload.setName("Jane Smith");
        updatePayload.setEmail("jane.smith@test.com");
        updatePayload.setDepartment("CSE");
        updatePayload.setSemester(4);
        updatePayload.setStudentId("CSE-02");
        updatePayload.setPassword("   ");

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
