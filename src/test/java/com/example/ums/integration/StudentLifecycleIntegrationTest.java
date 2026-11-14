package com.example.ums.integration;

import com.example.ums.model.Student;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class StudentLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Student lifecycle update, auth profile verification, and deletion workflow")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void studentLifecycleWorkflow_updatesAttributes_andDeletesRecord() throws Exception {
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

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        mockMvc.perform(delete("/api/students/" + studentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/" + studentId))
                .andExpect(status().isNotFound());
    }
}
