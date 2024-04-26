// package com.development.hris.controllers;

// import com.development.hris.entities.Event;

// import java.time.LocalDateTime;

// import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.PostMapping;
// import jakarta.transaction.Transactional;
// import org.springframework.format.annotation.DateTimeFormat;
// import org.springframework.format.annotation.DateTimeFormat.ISO;

// import com.fasterxml.jackson.databind.annotation.JsonSerialize;
// import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

// import com.development.hris.entities.EventRepository;

// import lombok.RequiredArgsConstructor;

// @RestController
// @RequiredArgsConstructor
// public class TestController {

//     // Something in the calendar controller is making this null
//     private final EventRepository er;

//     @GetMapping("/test2")
//     public String test2(){
//         System.out.println(er);
//         return "index";
//     }

//     @GetMapping("/events")
//     public String events(/*@RequestParam("start") @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime start, @RequestParam("end") @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime end*/){
//         System.out.println("ER: " + er);
//         return "test";
//         //return er.findBetween(start, end);
//     }

//     @PostMapping("/api/events/create")
//     @JsonSerialize(using = LocalDateTimeSerializer.class)
//     @Transactional
//     private Event createEvent(@RequestBody EventCreateParams params) {
//         Event e = new Event();
//         e.setStart(params.start);
//         e.setEnd(params.end);
//         e.setText(params.text);
//         er.save(e);

//         return e;
//     }

//     public static class EventCreateParams {
//         public LocalDateTime start;
//         public LocalDateTime end;
//         public String text;
//     }
// }
