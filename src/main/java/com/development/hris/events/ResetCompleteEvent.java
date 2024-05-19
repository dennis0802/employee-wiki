package com.development.hris.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetCompleteEvent extends ApplicationEvent{
    private String email;
    private String applicationUrl;

    public ResetCompleteEvent(String email, String applicationUrl){
        super(email);
        this.email = email;
        this.applicationUrl = applicationUrl;
    }
}