package com.development.hris.controllers;

import com.development.hris.entities.News;
import com.development.hris.entities.NewsComparator;
import com.development.hris.entities.PayrollData;
import com.development.hris.entities.PayrollDataComparator;
import com.development.hris.entities.SiteUser;
import com.development.hris.entities.TimeOffComparator;
import com.development.hris.entities.TimeOffRequest;
import com.development.hris.entities.WhistleInfo;
import com.development.hris.events.ResetCompleteEvent;
import com.development.hris.service.*;
import com.development.hris.token.PasswordRefreshToken;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.Calendar;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final ControllerUtilities controllerUtilities;
    private final ApplicationEventPublisher publisher;

    @GetMapping({"/", "/index"})
    public String getIndex(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        List<News> news = userService.getAllNews();
        Collections.sort(news, new NewsComparator());
        Collections.reverse(news);

        List<News> toDisplay = new ArrayList<News>(); 
        try{
            for(int i = 0; i < 3; i++){
                toDisplay.add(news.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        Object success= model.asMap().get("success");
        String passedSuccess = "";

        if(success != null){
            passedSuccess = success.toString();
        }
 
        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("success", passedSuccess);
        model.addAttribute("news", toDisplay);
        return "index";
    }

    // Org chart
    @GetMapping("/orgChart")
    public String getOrgChart(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        
        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("orgChartPath", controllerUtilities.getOrgChartLocation());
        return "orgChart";
    }

    // Global Address List
    @GetMapping("/gal")
    public String getGal(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username), searchTerm = params.get("search") != null ? params.get("search") : "";;
        List<SiteUser> allUsers = controllerUtilities.getSiteUsersOrderByLastName();

        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
        
        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<SiteUser> temp = new ArrayList<SiteUser>(allUsers);
            for (SiteUser user : temp) {
                if(!user.getUsername().contains(searchTerm) && !user.getEmail().contains(searchTerm)){
                    allUsers.remove(user);
                }
            }
        }
        int totalPages = (int)Math.ceil(allUsers.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<SiteUser> toDisplay = new ArrayList<SiteUser>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allUsers.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        Object errors = model.asMap().get("errors");
        Object success= model.asMap().get("success");
        List<String> passedErrors = new ArrayList<String>();
        String passedSuccess = "";

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }
        else if(success != null){
            passedSuccess = success.toString();
        }
        
        controllerUtilities.prepareBaseModel(model, role, username);
        controllerUtilities.preparePagingModel(model, passedErrors, passedSuccess, nextPage, prevPage, searchTerm, userService.getAllUsers().size(), allUsers.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "users", toDisplay, true, "newUser", new SiteUser());
        model.addAttribute("allSiteUsers", userService.getAllUsers());
        return "gal";
    }

    // Calendar
    // https://code.daypilot.org/58614/using-javascript-html5-monthly-calendar-in-spring-boot-java
    @GetMapping("/calendar")
    public String getCalendar(Model model, @AuthenticationPrincipal UserDetails userDetails, final HttpServletRequest request){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        controllerUtilities.prepareBaseModel(model, role, username);
        return "calendar";
    }

    // Resources and whistleblower
    @GetMapping("/resources")
    public String getResources(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        Object errors = model.asMap().get("errors");
        Object success= model.asMap().get("success");
        List<String> passedErrors = new ArrayList<String>();
        String passedSuccess = "";

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }
        else if(success != null){
            passedSuccess = success.toString();
        }
        
        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("newSubmission", new WhistleInfo(null, null));
        model.addAttribute("success", passedSuccess);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("resources", controllerUtilities.getResources());
        return "resources";
    }

    @PostMapping("/newSubmission")
    public String submitWhistleInfo(@ModelAttribute WhistleInfo submission, RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();

        if(submission.getSubject().isBlank()){
            errors.add("Subject cannot be blank");
        }

        if(submission.getContent().isBlank()){
            errors.add("Content cannot be blank.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/resources";
        }

        WhistleInfo added = userService.addSubmission(submission);
        log.info("Whistleblower form with id#" + added.getId() + " added.");
        redirectAttributes.addFlashAttribute("success", "Whistleblower form submitted!");
        return "redirect:/resources";
    }

    // Login
    @GetMapping("/login")
    public String login(Model model, @AuthenticationPrincipal UserDetails userDetails){
        if(userDetails != null){
            return "redirect:/index";
        }

        controllerUtilities.prepareBaseModel(model, null, null);
        return "loginPage";
    }

    @PostMapping("/login")
    public String postLogin(RedirectAttributes redirectAttributes, @RequestParam("username") String username){
        redirectAttributes.addFlashAttribute("success", "Welcome back, " + username + "!");
        return "redirect:/index";
    }

    // Logout
	@GetMapping("logout")
	public String logout(@AuthenticationPrincipal UserDetails userDetails, Model model){
        String username = userDetails.getUsername(), role = controllerUtilities.getRole(username);

        controllerUtilities.prepareBaseModel(model, role, username);
		return "logout";
	}

	@PostMapping("logout")
	public String logoutPost(){
		return "redirect:/login";
	}

    // View my pay
    @GetMapping("/viewMyPay")
    public String viewMyPay(@AuthenticationPrincipal UserDetails userDetails, Model model, @RequestParam Map<String, String> params){
        String username = userDetails.getUsername(), role = controllerUtilities.getRole(username), searchTerm = params.get("search") != null ? params.get("search") : "";

        SiteUser user = userService.findByUsername(username);
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE;
        List<PayrollData> myPayData = user.getPay();
        
        Collections.sort(myPayData, new PayrollDataComparator());
        Collections.reverse(myPayData);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<PayrollData> temp = new ArrayList<PayrollData>(myPayData);
            for (PayrollData payrollData : temp) {
                if(!payrollData.getEndDate().toString().contains(searchTerm) && !payrollData.getStartDate().toString().contains(searchTerm)){
                    myPayData.remove(payrollData);
                }
            }
        }
        int totalPages = (int)Math.ceil(myPayData.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;

        List<PayrollData> toDisplay = new ArrayList<PayrollData>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(myPayData.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        controllerUtilities.prepareBaseModel(model, role, username);
        controllerUtilities.preparePagingModel(model, null, null, nextPage, prevPage, searchTerm, user.getPay().size(), myPayData.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "statements", toDisplay, false, null, null);
        return "viewTemplate";
    }

    // View and make requests
    @GetMapping("/viewRequests")
    public String getRequests(@AuthenticationPrincipal UserDetails userDetails, Model model, @RequestParam Map<String, String> params){
        String username = userDetails.getUsername(), role = controllerUtilities.getRole(username), searchTerm = params.get("search") != null ? params.get("search") : "";

        SiteUser user = userService.findByUsername(username);
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE;
        List<TimeOffRequest> myRequests = user.getTimeOff();
        Collections.sort(myRequests, new TimeOffComparator());
        Collections.reverse(myRequests);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<TimeOffRequest> temp = new ArrayList<TimeOffRequest>(myRequests);
            for (TimeOffRequest timeOffRequest : temp) {
                if(!timeOffRequest.getEndDate().toString().contains(searchTerm) && !timeOffRequest.getStartDate().toString().contains(searchTerm)
                   && !timeOffRequest.getReason().contains(searchTerm)){
                    myRequests.remove(timeOffRequest);
                }
            }
        }
        int totalPages = (int)Math.ceil(myRequests.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;

        List<TimeOffRequest> toDisplay = new ArrayList<TimeOffRequest>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(myRequests.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        List<SiteUser> managedUsers = userService.getAllUsers().stream().filter(u -> u.getManagedBy() == username).collect(Collectors.toList());

        Object errors = model.asMap().get("errors");
        Object success= model.asMap().get("success");
        List<String> passedErrors = new ArrayList<String>();
        String passedSuccess = "";

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }
        else if(success != null){
            passedSuccess = success.toString();
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        controllerUtilities.preparePagingModel(model, passedErrors, passedSuccess, nextPage, prevPage, searchTerm, user.getTimeOff().size(), myRequests.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "requests", toDisplay, true, "newRequest", new TimeOffRequest(null, null, null));
        model.addAttribute("user", user);
        model.addAttribute("myUsers", managedUsers);
        return "viewTemplate";
    }

    @PostMapping("/newRequest")
    public String addTimeOffRequest(@ModelAttribute TimeOffRequest newRequest, RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails){
        boolean datesNotUploaded = newRequest.getStartDate() == null || newRequest.getEndDate() == null;
        SiteUser user = userService.findByUsername(userDetails.getUsername());
        List<String> errors = new ArrayList<String>();

        if(datesNotUploaded){
            errors.add("Both the start and end date must be selected.");
        }

        if(!datesNotUploaded && newRequest.getStartDate().after(newRequest.getEndDate())){
            errors.add("The start date must be before the end date.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        user.getTimeOff().add(newRequest);
        long id = userService.addTimeOffRequest(newRequest, user);
        redirectAttributes.addFlashAttribute("success", "Request added!");
        log.info("Time-off request id#" + id + "submitted.");
        return "redirect:/viewRequests";
    }

    @GetMapping("/editRequest")
    public String editRequest(Model model, RedirectAttributes redirectAttributes, @RequestParam(value = "id", defaultValue = "-1") int id, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername(), role = controllerUtilities.getRole(username);
        
        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        Object errors = model.asMap().get("errors");
        Object success= model.asMap().get("success");
        List<String> passedErrors = new ArrayList<String>();
        String passedSuccess = "";

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }
        else if(success != null){
            passedSuccess = success.toString();
        }

        TimeOffRequest specifiedRequest = userService.getTimeOffRequestById(id);
        if(specifiedRequest == null){
            List<String> errors2 = new ArrayList<String>();
            errors2.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors2);
            return "redirect:/viewRequests";
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("success", passedSuccess);
        model.addAttribute("editRequest", specifiedRequest);
        model.addAttribute("entitledDays", userService.findByUsername(username).getEntitledDays());
        return "editTemplate";
    }

    @PostMapping("/editRequest")
    public String submitedEditRequest(@ModelAttribute TimeOffRequest editedRequest, RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id){
        boolean datesNotUploaded = editedRequest.getStartDate() == null || editedRequest.getEndDate() == null;
        boolean approved = editedRequest.isHrApproved() && editedRequest.isHrViewed() && editedRequest.isManagerApproved() && editedRequest.isManagerViewed();
        SiteUser user = userService.findByUsername(userDetails.getUsername());
        List<String> errors = new ArrayList<String>();
        
        if(id == -1){
            return "redirect:/viewRequests";
        }
        
        TimeOffRequest oldRequest = userService.getTimeOffRequestById(id);

        if(approved){
            errors.add("You cannot change an approved request.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/editRequest";
        }

        if(datesNotUploaded){
            errors.add("Both the start and end date must be selected.");
        }

        if(!datesNotUploaded && editedRequest.getStartDate().after(editedRequest.getEndDate())){
            errors.add("The start date must be before the end date.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/editRequest";
        }

        userService.editRequest(editedRequest, oldRequest, user);
        redirectAttributes.addFlashAttribute("success", "Request edited!");
        log.info("Request id#" + id + "edited.");
        return "redirect:/viewRequests";
    }

    @GetMapping("/withdrawRequest")
    public String withdrawRequest(RedirectAttributes redirectAttributes, @RequestParam(value = "id", defaultValue = "-1") int id, @AuthenticationPrincipal UserDetails userDetails){
        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        TimeOffRequest request = userService.getTimeOffRequestById(id);
        if(request == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        SiteUser user = userService.findByUsername(userDetails.getUsername());

        log.info("Request id#" + request.getId() + " withdrawn.");
        userService.deleteRequest(request, user);
        redirectAttributes.addFlashAttribute("success", "Request withdrawn!");
        return "redirect:/viewRequests";
    }

    @GetMapping("/managerActionRequest")
    public String actionRequest(RedirectAttributes redirectAttributes, @RequestParam Map<String, String> params){
        int id = params.get("id") != null ? Integer.parseInt(params.get("id")) : -1;
        String approval = params.get("approval") != null ? params.get("approval") : "null";

        if(id == -1 || approval == "null"){
            List<String> errors = new ArrayList<String>();
            errors.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        SiteUser specifiedUser = null;
        for (SiteUser user : userService.getAllUsers()) {
            for (TimeOffRequest request : user.getTimeOff()) {
                if(id == request.getId()){
                    specifiedUser = user;
                    break;
                }
            }
        }

        if(specifiedUser == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        TimeOffRequest request = userService.getTimeOffRequestById(id);
        if(request.isHrApproved() && request.isManagerApproved()){
            List<String> errors = new ArrayList<String>();
            errors.add("Request has already been completed.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/viewRequests";
        }

        request.setManagerApproved(approval.equals("true"));
        request.setManagerViewed(true);
        userService.setRequestStatus(request, specifiedUser);

        controllerUtilities.checkForCompletedRequest(request, specifiedUser);
        redirectAttributes.addFlashAttribute("success", approval.equals("true") ? "Request approved!" : "Request denied!");
        log.info("Manager to request id#" + id + " viewed request.");
        return "redirect:/viewRequests";
    }

    // NEWS
    @GetMapping("/viewNews")
    public String getNews(@AuthenticationPrincipal UserDetails userDetails, Model model, @RequestParam Map<String, String> params){
        String username = userDetails.getUsername(), role = controllerUtilities.getRole(username), searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE;
        List<News> news = userService.getAllNews();
        Collections.sort(news, new NewsComparator());
        Collections.reverse(news);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<News> temp = new ArrayList<News>(news);
            for (News article : temp) {
                if(!article.getContent().contains(searchTerm) && !article.getAuthor().contains(searchTerm) && !article.getTitle().contains(searchTerm) && 
                   !article.getPostDate().toString().contains(searchTerm)){
                    news.remove(article);
                }
            }
        }
        int totalPages = (int)Math.ceil(news.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<News> toDisplay = new ArrayList<News>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(news.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        controllerUtilities.prepareBaseModel(model, role, username);
        controllerUtilities.preparePagingModel(model, null, null, nextPage, prevPage, searchTerm, userService.getAllNews().size(), news.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "news", toDisplay, false, null, null);
        return "viewTemplate";
    }

    // ACCOUNT
    @GetMapping("/yourAccount")
    public String getAccount(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        String passedSuccess = "";

        Object success= model.asMap().get("success");
        if(success != null){
            passedSuccess = success.toString();
        }
 
        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("user", userService.findByUsername(username));
        model.addAttribute("success", passedSuccess);
        return "account";
    }

    @GetMapping("/editAccount")
    public String editUser(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(id == -1){
            return "redirect:/index";
        }
        
        SiteUser siteUser = userService.findUserById(id);
        if(siteUser == null){
            return "redirect:/index";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("user", siteUser);
        return "editTemplate";
    }

    @PostMapping("/editAccount")
    public String editUserSubmitted(@ModelAttribute SiteUser user, @RequestParam("password") String password, RedirectAttributes redirectAttributes, 
                                    @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id){
        if(id == -1){
            return "redirect:/index";
        }

        SiteUser specified = userService.findUserById(id);
        Pattern pattern;
        Matcher matcher;

        if(specified == null){
            return "redirect:/index";
        }

        List<String> errors = new ArrayList<String>();
        boolean passwordChanged = false;
        if(!password.isBlank()){
            // A valid password has at least 12 characters, containing 1 digit, 1 lowercase character, 1 uppercase character, and 1 special character
            pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
            matcher = pattern.matcher(password);

            if(!matcher.matches()){
                errors.add("ERROR: The password is invalid. A valid password has at least 12 characters, containing 1 digit, 1 lowercase character, 1 uppercase character, and 1 special character.");
            }
            else{
                passwordChanged = true;
            }
        }

        // Email
        if(!user.getAlternateEmail().isBlank()){
            pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(user.getAlternateEmail());
            if(!matcher.matches()){
                errors.add("The email address is invalid.");
            }
    
            if(userService.findByEmail(user.getAlternateEmail()) != null  && !specified.getAlternateEmail().equals(user.getAlternateEmail())){
                errors.add("The email address must be unique.");
            }
        }

        if(!user.getPhoneNum().isBlank()){
            pattern = Pattern.compile("^[0-9]{3}-[0-9]{3}-[0-9]{4}$|^$");
            matcher = pattern.matcher(user.getPhoneNum());
            
            if(!matcher.matches()){
                errors.add("ERROR: The phone number is invalid, format it as ###-###-####.");
            }
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/editAccount?id=" + id;
        }

        if(passwordChanged){
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            specified.setPassword(encoder.encode(password));
        }

        specified.setAlternateEmail(user.getAlternateEmail());
        specified.setPhoneNum(user.getPhoneNum());
        specified.setWorkLocation(user.getWorkLocation());
        userService.saveUser(specified);

        log.info("User " + specified.getUsername() + " has edited their account.");
        redirectAttributes.addFlashAttribute("success", "Account edited!");
        return "redirect:/yourAccount";
    }  

    // PASSWORD RESET
    @GetMapping("/forgotPassword")
    public String forgotPassword(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(!username.isBlank()){
            return "redirect:/yourAccount";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        return "forgotPassword";
    }

    @PostMapping("/forgotPassword")
    public String submitPasswordReset(@RequestParam(name="email") String email, RedirectAttributes redirectAttributes, final HttpServletRequest request){
        List<String> errors = new ArrayList<String>();

        Pattern pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        if(!matcher.matches()){
            errors.add("The email address is invalid.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
        }

        // Send the email here
        publisher.publishEvent(new ResetCompleteEvent(email, applicationUrl(request)));
        log.info("Password request submitted using " + email);
        redirectAttributes.addFlashAttribute("success", "Success! You will get an email with further instructions if it is associated to your account.");
        return "redirect:/forgotPassword";
    }

    @GetMapping("/resetPassword")
	public String resetPassword(@RequestParam("token") String token, RedirectAttributes redirectAttributes){
        List<String> errors = new ArrayList<String>();
        PasswordRefreshToken refreshToken = userService.getTokenWithString(token);
        Calendar calendar = Calendar.getInstance();

		// No user exists with that token
		if(refreshToken == null){
            errors.add("That token is invalid to verify.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
		}

        if((refreshToken.getExpirationTime().getTime() - calendar.getTime().getTime()) > 0){
            return "redirect:/changePassword?token=" + token;
        }

		// Invalid token
        userService.deleteToken(refreshToken);
        errors.add("That token is invalid to verify.");
        redirectAttributes.addFlashAttribute("errors", errors);
        return "redirect:/forgotPassword";
	}

    // Changing the password
    @GetMapping("/changePassword")
    public String changePassword(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes){
        List<String> errors = new ArrayList<String>();

        if(token == null){
            errors.add("That token is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
        }

        PasswordRefreshToken passToken = userService.getTokenWithString(token);
        if(passToken == null){
            errors.add("That token is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
        }

        model.addAttribute("token", token);
        controllerUtilities.prepareBaseModel(model, null, null);
        return "changePassword";
    }

    @PostMapping("/changePassword")
    public String changePasswordSubmitted(@ModelAttribute SiteUser user, @RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes,
                                          @RequestParam("password") String password, @RequestParam("confirmPassword") String confirmPassword){
        List<String> errors = new ArrayList<String>();                
        if(token == null){
            errors.add("That token is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
        }

        PasswordRefreshToken passToken = userService.getTokenWithString(token);
        if(passToken == null){
            errors.add("That token is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
        }

        // A valid password has at least 12 characters, containing 1 digit, 1 lowercase character, 1 uppercase character, and 1 special character
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
        Matcher matcher = pattern.matcher(password);

        if(!matcher.matches()){
            errors.add("The password is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/changePassword?token=" + token;
        }

        if(confirmPassword.isBlank()){
            errors.add("You must confirm your password.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/changePassword?token=" + token;
        }

        // The password inputs should match
		if(!password.isBlank() && !confirmPassword.isBlank() && !password.equals(confirmPassword)){
            errors.add("The password is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/changePassword?token=" + token;
		}

        SiteUser toReset = passToken.getUser();
        if(toReset == null){
            errors.add("That token is invalid.");
			redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/forgotPassword";
        }

        // Upon successful password, save to user and then delete the token
        String result = userService.validateResetTokenAndSetPassword(token, password);
        if(result.equalsIgnoreCase("valid")){
            userService.deleteToken(passToken);
            redirectAttributes.addFlashAttribute("success", "Your password has been reset! Please log in to ensure access.");
            log.info("A user has reset their password.");
            return "redirect:/index";
        }

        errors.add("That token is invalid.");
        redirectAttributes.addFlashAttribute("errors", errors);
        return "redirect:/forgotPassword";
    }

    /**
	 * Create the token url
	 * @param request
	 * @return The token url
	 */
	private String applicationUrl(HttpServletRequest request){
		return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
	}

}
