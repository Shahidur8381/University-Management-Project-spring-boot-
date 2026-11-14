package com.example.ums.integration;

import com.example.ums.model.Student;
import com.example.ums.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class StudentRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Student registration, database persistence, and retrieval workflow")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void studentRegistrationAndRetrievalWorkflow_persistsDataAndHidesPassword() throws Exception {
        String studentJson = """
                {
                    "username": "integ_student_01",
                    "password": "SecurePassword123!",
                    "name": "Alice Wonderland",
                    "email": "alice@university.edu",
                    "studentId": "INTEG-2026-001",
                    "department": "Computer Science",
                    "semester": 1
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integ_student_01"))
                .andExpect(jsonPath("$.name").value("Alice Wonderland"))
                .andExpect(jsonPath("$.email").value("alice@university.edu"))
                .andExpect(jsonPath("$.studentId").value("INTEG-2026-001"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        Student createdStudent = objectMapper.readValue(result.getResponse().getContentAsString(), Student.class);
        Long generatedId = createdStudent.getId();
        assertNotNull(generatedId);

        Optional<Student> fromDb = studentRepository.findById(generatedId);
        assertTrue(fromDb.isPresent());
        assertEquals("integ_student_01", fromDb.get().getUsername());
        assertEquals("ROLE_STUDENT", fromDb.get().getRole());
        assertNotEquals("SecurePassword123!", fromDb.get().getPassword());

        mockMvc.perform(get("/api/students/" + generatedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integ_student_01"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
