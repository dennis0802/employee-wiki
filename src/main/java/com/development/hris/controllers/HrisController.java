package com.development.hris.controllers;

import com.development.hris.entities.EventRepository;
import com.development.hris.entities.SiteUser;
import com.development.hris.service.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HrisController {
    
    private final UserService userService;

    @GetMapping({"/", "/index"})
    public String getIndex(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        return "index";
    }

    // Org chart
    @GetMapping("/orgChart")
    public String getOrgChart(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("orgChartPath", "/uploads/test.png");
        return "orgChart";
    }

    // Global Address List
    @GetMapping("/gal")
    public String getGal(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        List<SiteUser> allUsers = userService.getAllUsers();
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("allUsers", allUsers);
        return "gal";
    }

    // Calendar
    // https://code.daypilot.org/58614/using-javascript-html5-monthly-calendar-in-spring-boot-java
    @GetMapping("/calendar")
    public String getCalendar(Model model, @AuthenticationPrincipal UserDetails userDetails, final HttpServletRequest request){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        return "calendar";
    }

    // Login
    @GetMapping("/login")
    public String login(Model model, @AuthenticationPrincipal UserDetails userDetails){
        if(userDetails != null){
            return "redirect:/index";
        }
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        return "loginPage";
    }

    @PostMapping("/login")
    public String postLogin(){
        return "redirect:/index";
    }

    // Logout
	@GetMapping("logout")
	public String logout(@AuthenticationPrincipal UserDetails userDetails, Model model){
        String username = userDetails.getUsername();
        String role = getRole(username);

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
		return "logout";
	}

    @GetMapping("/test")
    public String test(){
        Calendar calendar = Calendar.getInstance();
        userService.editUser("rootUser", "Root", "User", "Application Administrator", calendar.getTime());
        return "redirect:/index";
    }

	@PostMapping("logout")
	public String logoutPost(){
		return "redirect:/login";
	}

    private String getRole(String username){
        SiteUser user = userService.findByUsername(username);

        if(user != null){
            return user.getRole();
        }
        return "";
    }

    private int getYear(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        return calendar.get(Calendar.YEAR);
    }
}
