package com.example.ums.config;

import com.example.ums.model.Admin;
import com.example.ums.model.Student;
import com.example.ums.model.Teacher;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminRepository adminRepository,
                           TeacherRepository teacherRepository,
                           StudentRepository studentRepository,
                           PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "System Admin",
                    "admin@university.edu",
                    "Administration"
            );
            adminRepository.save(admin);
        }

        if (teacherRepository.count() == 0) {
            Teacher teacher = new Teacher(
                    "teacher",
                    passwordEncoder.encode("teacher123"),
                    "Nabil Faiyaz Sadi",
                    "sadi@cse.kuet.ac.bd",
                    "Computer Science and Engineering",
                    "Lecturer"
            );
            teacherRepository.save(teacher);
        }

        if (studentRepository.count() == 0) {
            Student student = new Student(
                    "student",
                    passwordEncoder.encode("student123"),
                    "Md. Shahidur Rahman",
                    "hello@shahidur.dev",
                    "2207103",
                    "Computer Science and Engineering",
                    6
            );
            studentRepository.save(student);
        }
    }
}
