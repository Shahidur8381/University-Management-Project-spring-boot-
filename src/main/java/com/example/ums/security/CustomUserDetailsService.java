package com.example.ums.security;

import com.example.ums.model.BaseUser;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public CustomUserDetailsService(AdminRepository adminRepository,
                                    TeacherRepository teacherRepository,
                                    StudentRepository studentRepository) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<? extends BaseUser> user = adminRepository.findByUsername(username);

        if (user.isEmpty()) {
            user = teacherRepository.findByUsername(username);
        }

        if (user.isEmpty()) {
            user = studentRepository.findByUsername(username);
        }

        BaseUser foundUser = user.orElseThrow(() ->
                new UsernameNotFoundException("User not found with username: " + username)
        );

        return new User(
                foundUser.getUsername(),
                foundUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(foundUser.getRole()))
        );
    }
}
