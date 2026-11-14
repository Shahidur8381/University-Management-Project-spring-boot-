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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentUpdatePasswordTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void updateStudent_whenNewPasswordProvided_updatesStudentAndHashesPassword() {
        StudentController studentController = new StudentController(studentRepository, passwordEncoder);

        Student existingStudent = new Student(
                "john_doe",
                "old_hashed_pass",
                "John Doe",
                "john@test.com",
                "CSE-01",
                "CSE",
                1
        );
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
}
