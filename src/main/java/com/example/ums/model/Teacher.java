package com.example.ums.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "teachers")
public class Teacher extends BaseUser {

    private String department;
    private String designation;

    public Teacher() {
        setRole("ROLE_TEACHER");
    }

    public Teacher(String username, String password, String name, String email, String department, String designation) {
        super(username, password, "ROLE_TEACHER", name, email);
        this.department = department;
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
