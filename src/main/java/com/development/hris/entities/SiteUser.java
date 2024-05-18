package com.development.hris.entities;

import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SiteUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String jobTitle;
    @NaturalId(mutable = true)
    private String email;
    @NaturalId(mutable = true)
    private String alternateEmail;
    private String password;
    private String confirmedPassword;
    private String role;
    private Date joinDate;
    private boolean isEnabled = false;
    private String phoneNum;
    private String workLocation;
    private int entitledDays;
    private String managedBy;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<Event>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollData> pay = new ArrayList<PayrollData>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeOffRequest> timeOff = new ArrayList<TimeOffRequest>();

    public SiteUser(String username, String password, String email, String alternateEmail, String role, String phoneNum, String workLocation, String firstName, 
                    String lastName, String jobTitle, int entitledDays, String managedBy, Date joinDate){
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.isEnabled = false;
        this.phoneNum = phoneNum;
        this.jobTitle = jobTitle;
        this.workLocation = workLocation;
        this.entitledDays = entitledDays;
        this.managedBy = managedBy;
        this.joinDate = joinDate;
    }
}
