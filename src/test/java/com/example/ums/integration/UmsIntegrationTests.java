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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
public class UmsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Integration Test 1: Student registration, database persistence, and retrieval without password leak
    @Test
    @DisplayName("Integration Test 1: Student registration, database persistence, and retrieval workflow")
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

        // 1. Perform POST request to register student
        MvcResult result = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integ_student_01"))
                .andExpect(jsonPath("$.name").value("Alice Wonderland"))
                .andExpect(jsonPath("$.email").value("alice@university.edu"))
                .andExpect(jsonPath("$.studentId").value("INTEG-2026-001"))
                .andExpect(jsonPath("$.password").doesNotExist()) // Password must never leak
                .andReturn();

        Student createdStudent = objectMapper.readValue(result.getResponse().getContentAsString(), Student.class);
        Long generatedId = createdStudent.getId();
        assertNotNull(generatedId);

        // 2. Verify actual persistence in database
        Optional<Student> fromDb = studentRepository.findById(generatedId);
        assertTrue(fromDb.isPresent());
        assertEquals("integ_student_01", fromDb.get().getUsername());
        assertEquals("ROLE_STUDENT", fromDb.get().getRole());
        assertNotEquals("SecurePassword123!", fromDb.get().getPassword()); // Password must be hashed

        // 3. Verify subsequent GET request
        mockMvc.perform(get("/api/students/" + generatedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integ_student_01"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // Integration Test 2: Role-Based Access Control (RBAC) across endpoints and roles
    @Test
    @DisplayName("Integration Test 2: RBAC security workflow across Admin, Teacher, Student and unauthenticated requests")
    void roleBasedAccessControlWorkflow_enforcesEndpointSecurityAcrossRoles() throws Exception {
        // 1. Unauthenticated request to protected endpoints is rejected with 401
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());

        // 2. Admin has full access to admins, teachers, and students
        mockMvc.perform(get("/api/admins")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        // 3. Teacher can manage students, but forbidden from admin endpoints
        mockMvc.perform(get("/api/students")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teacher").roles("TEACHER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admins")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teacher").roles("TEACHER")))
                .andExpect(status().isForbidden());

        // 4. Student can view teacher list (GET /api/teachers), but forbidden from admins and mutating students
        mockMvc.perform(get("/api/teachers")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admins")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/students")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // Integration Test 3: Complete Entity Lifecycle (Create -> Update -> Auth Check -> Delete -> 404 Verify)
    @Test
    @DisplayName("Integration Test 3: Student lifecycle update, auth profile verification, and deletion workflow")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void studentLifecycleWorkflow_updatesAttributes_andDeletesRecord() throws Exception {
        // 1. Create a student
        String initialJson = """
                {
                    "username": "lifecycle_student",
                    "password": "Password123",
                    "name": "Bob Builder",
                    "email": "bob@university.edu",
                    "studentId": "LC-101",
                    "department": "Civil",
                    "semester": 1
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialJson))
                .andExpect(status().isCreated())
                .andReturn();

        Student student = objectMapper.readValue(createResult.getResponse().getContentAsString(), Student.class);
        Long studentId = student.getId();

        // 2. Update student attributes via PUT
        String updateJson = """
                {
                    "username": "lifecycle_student",
                    "name": "Robert Builder",
                    "email": "robert.builder@university.edu",
                    "studentId": "LC-101-MOD",
                    "department": "Structural Engineering",
                    "semester": 3
                }
                """;

        mockMvc.perform(put("/api/students/" + studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Robert Builder"))
                .andExpect(jsonPath("$.department").value("Structural Engineering"))
                .andExpect(jsonPath("$.semester").value(3));

        // 3. Verify security auth profile endpoint (/api/me)
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        // 4. Delete the student via DELETE
        mockMvc.perform(delete("/api/students/" + studentId))
                .andExpect(status().isNoContent());

        // 5. Confirm deletion: GET returns 404 Not Found
        mockMvc.perform(get("/api/students/" + studentId))
                .andExpect(status().isNotFound());
    }
}
