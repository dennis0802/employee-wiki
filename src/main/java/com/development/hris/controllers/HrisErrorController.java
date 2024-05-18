package com.development.hris.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.development.hris.service.ControllerUtilities;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class HrisErrorController implements ErrorController  {

    private final ControllerUtilities controllerUtilities;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        controllerUtilities.prepareBaseModel(model, null, null);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
        
            if(statusCode == HttpStatus.BAD_REQUEST.value()){
                log.info("Encountered code 400. Bad request.");
                return "error-400_500";
            }
            else if(statusCode == HttpStatus.FORBIDDEN.value()){
                log.info("Encountered code 403. Insufficient authentication to access resource.");
                return "error-403";
            }
            else if(statusCode == HttpStatus.NOT_FOUND.value()) {
                log.info("Encountered code 404. Resource not found.");
                return "error-404";
            }
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.info("Encountered code 500. Server was unable to process request.");
                return "error-400_500";
            }
        }
        return "error";
    }
}