package com.development.hris.entities;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String author;
    private String title;
    private String content;
    private String imageLocation;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date postDate;

    public News(String author, String title, String content, String imageLocation){
        this.author = author;
        this.title = title;
        this.content = content;
        this.imageLocation = imageLocation;
    }


}
