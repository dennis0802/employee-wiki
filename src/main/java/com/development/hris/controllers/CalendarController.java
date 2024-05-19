package com.development.hris.controllers;

import java.time.LocalDateTime;

import java.util.List;

import com.development.hris.entities.EventRepository;
import com.development.hris.entities.SiteUser;
import com.development.hris.service.UserService;
import com.development.hris.entities.Event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CalendarController {

    @Autowired
    UserService userService;

    @Autowired
    EventRepository er;

    @GetMapping("/api/events")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    Iterable<Event> events(@RequestParam("start") @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime start, @RequestParam("end") @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime end, @AuthenticationPrincipal UserDetails userDetails){
        SiteUser user = userService.findByUsername(userDetails.getUsername());
        
        List<Event> filteredEvents = er.findBetween(start, end);
        for (Event e : er.findBetween(start, end)) {
            if(!user.getEvents().contains(e) && !e.isPublicEvent()){
                filteredEvents.remove(e);
            }
        }

        return filteredEvents;
    }

    @PostMapping("/api/events/create")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Transactional
    Event createEvent(@RequestBody EventCreateParams params, @AuthenticationPrincipal UserDetails userDetails) {
        SiteUser user = userService.findByUsername(userDetails.getUsername());

        Event e = new Event();
        e.setStart(params.start);
        e.setEnd(params.end);

        if(params.text == ""){
            params.text = "Event";
        }

        e.setText(user.getUsername() + ": " + params.text);
        e.setPublicEvent(params.isPublic);
        user.getEvents().add(e);
        er.save(e);

        if(params.isPublic){
            log.info(user.getUsername() + " created event.");
        }
        
        return e;
    }

    @PostMapping("/api/events/move")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Transactional
    Event moveEvent(@RequestBody EventMoveParams params) {
        Event e = er.findById(params.id).get();
        e.setStart(params.start);
        e.setEnd(params.end);
        er.save(e);
        
        return e;
    }

    @PostMapping("/api/events/setColor")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Transactional
    Event setColor(@RequestBody SetColorParams params) {
        Event e = er.findById(params.id).get();
        e.setColor(params.color);
        er.save(e);

        return e;
    }

    @PostMapping("/api/events/delete")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Transactional
    EventDeleteResponse deleteEvent(@RequestBody EventDeleteParams params, @AuthenticationPrincipal UserDetails userDetails) {
        SiteUser user = userService.findByUsername(userDetails.getUsername());
        Event e = null;

        if(er.findById(params.id).isPresent()){
            e = er.findById(params.id).get();
        }
        else{
            return new EventDeleteResponse() {{
                message = "Event does not exist.";
            }};
        }

        if(!user.getEvents().contains(e)){
            log.info("User attempted to delete an event that doesn't belong to them.");
            return new EventDeleteResponse() {{
                message = "Event does not belong to this user.";
            }};
        }

        user.getEvents().remove(e);
        er.delete(e);

        return new EventDeleteResponse() {{
            message = "Deleted";
        }};
    }

    public static class EventDeleteParams {
        public Long id;
    }

    public static class EventDeleteResponse {
        public String message;
    }

    public static class EventCreateParams {
        public LocalDateTime start;
        public LocalDateTime end;
        public boolean isPublic;
        public String text;
    }

    public static class EventMoveParams {
        public Long id;
        public LocalDateTime start;
        public LocalDateTime end;
    }

    public static class SetColorParams {
        public Long id;
        public String color;
    }
}
