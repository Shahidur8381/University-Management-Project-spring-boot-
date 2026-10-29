package com.example.ums.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends BaseUser {

    private String department;

    public Admin() {
        setRole("ROLE_ADMIN");
    }

    public Admin(String username, String password, String name, String email, String department) {
        super(username, password, "ROLE_ADMIN", name, email);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
