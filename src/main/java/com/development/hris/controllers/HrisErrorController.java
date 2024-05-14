package com.development.hris.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class HrisErrorController implements ErrorController  {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
        
            if(statusCode == HttpStatus.FORBIDDEN.value()){
                log.info("Encountered code 403. Insufficient authentication to access resource.");
                return "error-403";
            }
            else if(statusCode == HttpStatus.NOT_FOUND.value()) {
                log.info("Encountered code 404. Resource not found.");
                return "error-404";
            }
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.info("Encountered code 500. Server was unable to process request.");
                return "error-500";
            }
        }
        return "error";
    }
}