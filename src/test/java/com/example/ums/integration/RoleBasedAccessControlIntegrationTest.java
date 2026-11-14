package com.example.ums.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RoleBasedAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("RBAC security across Admin, Teacher, Student, and unauthenticated requests")
    void roleBasedAccessControlWorkflow_enforcesEndpointSecurityAcrossRoles() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admins").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admins").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/teachers").with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admins").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/students")
                        .with(user("student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
