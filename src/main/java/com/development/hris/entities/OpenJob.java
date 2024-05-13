package com.development.hris.entities;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class OpenJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String position;
    private String link;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date postDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date deadline;

    private boolean isActive;

    public OpenJob(String position, String link, Date postDate, Date deadline, boolean isActive){
        this.position = position;
        this.link = link;
        this.postDate = postDate;
        this.deadline = deadline;
        this.isActive = isActive;
    }
}
