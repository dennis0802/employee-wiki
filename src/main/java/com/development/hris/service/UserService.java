package com.development.hris.service;

import com.development.hris.entities.*;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final SiteUserRepository userRepository;

    public void addUser(String username, String password, String email, String altEmail, String role, String phoneNum, String workLocation, String firstName, String lastName, String jobTitle){
        SiteUser user = new SiteUser(username, password, email, altEmail, role, phoneNum, workLocation, firstName, lastName, jobTitle);
        user.setEnabled(true);
        userRepository.save(user);
    }

    public SiteUser findByUsername(String username){
        if(userRepository.findByUsername(username).isPresent()){
            return userRepository.findByUsername(username).get();
        }
        return null;
    }

    public List<SiteUser> getAllUsers(){
        return userRepository.findAll();
    }

    public boolean editUser(String username, String firstName, String lastName, String jobTitle, Date date){
        SiteUser user;
        try {
            user = userRepository.findByUsername(username).get();
        } catch (Exception e) {
            return false;
        }
        
        user.setJoinDate(date);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setJobTitle(jobTitle);
        user.setRole("ADMIN");
        userRepository.save(user);
        return true;
    }
}
