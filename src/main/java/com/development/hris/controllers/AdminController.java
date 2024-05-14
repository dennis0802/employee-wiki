package com.development.hris.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.development.hris.service.ControllerUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@Slf4j
public class AdminController {
    private final ControllerUtilities controllerUtilities;

    @GetMapping("/adminMenu")
    public String adminMenu(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = controllerUtilities.getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", controllerUtilities.getYear());
        model.addAttribute("company", "TempCompany");
        return "adminMenu";
    }

    // some elements to consider
    // hr email
    // resources list
}
