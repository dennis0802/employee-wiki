package com.development.hris.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Date;

import com.development.hris.entities.SiteUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartUpComponent {
    private final UserService userService;

    @EventListener(ApplicationReadyEvent.class)
    public void firstTimeStartUp()
    {
        if(userService.findByUsername("root.user") == null){
            SiteUser rootUser = new SiteUser("root.user", "$2a$10$EyRZKoP7X28jLUJrDMlsAOqbr8C7d51Gyk6X9.r9T37O9T6UzlFvu", 
                                            "temp@temp.com", "temp@temp.com", "ADMIN", "111-111-1111", 
                                            "TEMP", "Root", "User", "Application Administrator", 3, "", new Date());
            userService.saveUser(rootUser);
            log.info("Root user has been created.");
        }
    }
}
