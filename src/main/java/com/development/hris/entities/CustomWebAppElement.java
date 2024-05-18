package com.development.hris.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class CustomWebAppElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String content;
    private String contentLink;
    private List<String> contentList;

    public CustomWebAppElement(String description, String content, String contentLink){
        this.description = description;
        this.content = content;
        this.contentLink = contentLink;
    }
}
