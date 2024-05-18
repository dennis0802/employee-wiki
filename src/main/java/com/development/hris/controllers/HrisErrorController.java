package com.development.hris.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public String handleError(HttpServletRequest request, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        controllerUtilities.prepareBaseModel(model, role, username);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            model.addAttribute("code", statusCode.intValue());

            if(statusCode == HttpStatus.BAD_REQUEST.value()){
                log.info("Encountered code 400. Bad request.");
            }
            else if(statusCode == HttpStatus.FORBIDDEN.value()){
                log.info("Encountered code 403. Insufficient authentication to access resource.");
            }
            else if(statusCode == HttpStatus.NOT_FOUND.value()) {
                log.info("Encountered code 404. Resource not found.");
            }
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.info("Encountered code 500. Server was unable to process request.");
            }
        }
        return "error";
    }
}