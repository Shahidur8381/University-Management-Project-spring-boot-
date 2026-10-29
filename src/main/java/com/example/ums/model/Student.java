package com.example.ums.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student extends BaseUser {

    private String studentId;
    private String department;
    private Integer semester;

    public Student() {
        setRole("ROLE_STUDENT");
    }

    public Student(String username, String password, String name, String email, String studentId, String department, Integer semester) {
        super(username, password, "ROLE_STUDENT", name, email);
        this.studentId = studentId;
        this.department = department;
        this.semester = semester;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }
}
