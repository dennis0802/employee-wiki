package com.development.hris.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Getter @Setter
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long jobId;
    
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantApplicationLocation;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date submissionDate;
}
