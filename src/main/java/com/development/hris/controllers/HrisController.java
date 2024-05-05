package com.development.hris.controllers;

import com.development.hris.entities.Event;
import com.development.hris.entities.EventRepository;
import com.development.hris.entities.PayrollData;
import com.development.hris.entities.PayrollDataComparator;
import com.development.hris.entities.SiteUser;
import com.development.hris.entities.SiteUserComparator;
import com.development.hris.entities.TimeOffComparator;
import com.development.hris.entities.TimeOffRequest;
import com.development.hris.service.*;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HrisController {
    // Directory for image uploads
	public static String PAY_UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/uploads/pay";
    
    private final UserService userService;
    private final EmailService emailService;

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
        List<SiteUser> allUsers = getSiteUsersOrderByLastName();
        
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

    // Resources
    @GetMapping("/resources")
    public String getResources(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        return "resources";
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

	@PostMapping("logout")
	public String logoutPost(){
		return "redirect:/login";
	}

    // View my pay
    @GetMapping("viewMyPay")
    public String viewMyPay(@AuthenticationPrincipal UserDetails userDetails, Model model, @RequestParam Map<String, String> params){
        String username = userDetails.getUsername();
        String role = getRole(username);

        SiteUser user = userService.findByUsername(username);
        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * 5, max = 4 + (page-1) * 5;
        float viewPerPage = 25;
        List<PayrollData> myPayData = user.getPay();
        
        Collections.sort(myPayData, new PayrollDataComparator());
        Collections.reverse(myPayData);

        // Filter the data if a search term is provided
        if(!searchTerm.isEmpty()){
            List<PayrollData> temp = new ArrayList<PayrollData>(myPayData);
            for (PayrollData payrollData : temp) {
                if(!payrollData.getEndDate().toString().contains(searchTerm) && !payrollData.getStartDate().toString().contains(searchTerm)){
                    myPayData.remove(payrollData);
                }
            }
        }
        int totalPages = (int)Math.ceil(myPayData.size()/viewPerPage), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;

        List<PayrollData> toDisplay = new ArrayList<PayrollData>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(myPayData.get(i));
            }
        }
        catch(Exception e){}

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("statements", toDisplay);
        model.addAttribute("nextPage", nextPage);
		model.addAttribute("prevPage", prevPage);
        model.addAttribute("searched", searchTerm);
        model.addAttribute("baseCount", user.getPay().size());
        model.addAttribute("totalCount", myPayData.size());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("currentPage", page);
        return "viewMyPay";
    }

    // View and make requests
    @GetMapping("/viewRequests")
    public String getRequests(@AuthenticationPrincipal UserDetails userDetails, Model model, @RequestParam Map<String, String> params){
        String username = userDetails.getUsername();
        String role = getRole(username);

        SiteUser user = userService.findByUsername(username);
        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * 5, max = 4 + (page-1) * 5;
        float viewPerPage = 25;
        List<TimeOffRequest> myRequests = user.getTimeOff();
        Collections.sort(myRequests, new TimeOffComparator());
        Collections.reverse(myRequests);

        // Filter the data if a search term is provided
        if(!searchTerm.isEmpty()){
            List<TimeOffRequest> temp = new ArrayList<TimeOffRequest>(myRequests);
            for (TimeOffRequest timeOffRequest : temp) {
                if(!timeOffRequest.getEndDate().toString().contains(searchTerm) && !timeOffRequest.getStartDate().toString().contains(searchTerm)
                   && !timeOffRequest.getReason().contains(searchTerm)){
                    myRequests.remove(timeOffRequest);
                }
            }
        }
        int totalPages = (int)Math.ceil(myRequests.size()/viewPerPage), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;

        List<TimeOffRequest> toDisplay = new ArrayList<TimeOffRequest>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(myRequests.get(i));
            }
        }
        catch(Exception e){}

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

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("user", user);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("requests", toDisplay);
        model.addAttribute("nextPage", nextPage);
		model.addAttribute("prevPage", prevPage);
        model.addAttribute("searched", searchTerm);
        model.addAttribute("baseCount", user.getTimeOff().size());
        model.addAttribute("totalCount", myRequests.size());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("currentPage", page);
        model.addAttribute("myUsers", managedUsers);
        model.addAttribute("newRequest", new TimeOffRequest(null, null, null));
        model.addAttribute("errors", passedErrors);
        model.addAttribute("success", passedSuccess);
        return "viewMyRequests";
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
        userService.addTimeOffRequest(newRequest, user);
        redirectAttributes.addFlashAttribute("success", "Request added!");
        return "redirect:/viewRequests";
    }

    @GetMapping("/editRequest")
    public String editRequest(Model model, RedirectAttributes redirectAttributes, @RequestParam(value = "id", defaultValue = "-1") int id, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername();
        String role = getRole(username);
        
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

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("errors", passedErrors);
        model.addAttribute("success", passedSuccess);
        model.addAttribute("editRequest", specifiedRequest);
        model.addAttribute("entitledDays", userService.findByUsername(username).getEntitledDays());
        return "editMyRequest";
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
        SiteUser user = userService.findByUsername(userDetails.getUsername());

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

        checkForCompletedRequest(request);
        redirectAttributes.addFlashAttribute("success", approval.equals("true") ? "Request approved!" : "Request denied!");
        return "redirect:/viewRequests";
    }

    // ----------------------------------------- HR -----------------------------------------
    @GetMapping("/hrMenu")
    public String hrMenu(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        return "hrMenu";
    }

    @GetMapping("/hrViewPayroll")
    public String hrViewPayroll(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);

        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * 5, max = 4 + (page-1) * 5;
        float viewPerPage = 25;

        List<SiteUser> allUsers = getSiteUsersOrderByLastName();
        List<PayrollData> allPayroll = userService.getAllPayData();

        PayrollDataComparator payrollDataComparator = new PayrollDataComparator();
        Collections.sort(allPayroll, payrollDataComparator);
        Collections.reverse(allPayroll);

        // Filter the data if a search term is provided
        if(!searchTerm.isEmpty()){
            List<PayrollData> temp = new ArrayList<PayrollData>(allPayroll);
            for (PayrollData payrollData : temp) {
                if(!payrollData.getEndDate().toString().contains(searchTerm) && !payrollData.getStartDate().toString().contains(searchTerm) &&
                    !payrollData.getForUser().contains(searchTerm)){
                    allPayroll.remove(payrollData);
                    allUsers.remove(userService.findByUsername(payrollData.getForUser()));
                }
            }
        }
        int totalPages = (int)Math.ceil(allPayroll.size()/viewPerPage), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<PayrollData> toDisplay = new ArrayList<PayrollData>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allPayroll.get(i));
            }
        }
        catch(Exception e){}

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

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("statements", allPayroll);
        model.addAttribute("company", "TempCompany");
        model.addAttribute("users", allUsers);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("success", passedSuccess);
        model.addAttribute("statementNew", new PayrollData());
        model.addAttribute("nextPage", nextPage);
		model.addAttribute("prevPage", prevPage);
        model.addAttribute("searched", searchTerm);
        model.addAttribute("baseCount", userService.getAllPayData().size());
        model.addAttribute("totalCount", allPayroll.size());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("currentPage", page);
        return "hrViewPayroll";
    }

    @PostMapping("/hrNewStatement")
    public String newStatement(@ModelAttribute PayrollData statementNew, @RequestParam("user") String username, RedirectAttributes redirectAttributes, 
                               @RequestParam("statement") MultipartFile file){
        List<String> errors = new ArrayList<String>();
        boolean datesNotUploaded = statementNew.getStartDate() == null ||statementNew.getEndDate() == null;
        String filePath = "";
        
        if(username.isEmpty()){
            errors.add("A user must be selected.");
        }

        if(datesNotUploaded){
            errors.add("Both the start and end date must be selected.");
        }

        if(file.getOriginalFilename().isEmpty()){
            errors.add("A statement must be uploaded.");
        }

        if(!datesNotUploaded && statementNew.getStartDate().after(statementNew.getEndDate())){
            errors.add("The start date must be before the end date.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = Paths.get(PAY_UPLOAD_DIRECTORY, file.getOriginalFilename());
            fileName.append(file.getOriginalFilename());

            try {
                Files.delete(fileNameAndPath);
            }
            catch (java.nio.file.NoSuchFileException e) {
                errors.add("Please contact your IT administrator, an error has occurred with deletion.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrViewPayroll";
            }
            catch (Exception e) {
                errors.add("Please contact your IT administrator, an error has occurred with deletion.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrViewPayroll";
            }
        }
        else{
            StringBuilder fileName = new StringBuilder();
            String tokenizedName = UUID.randomUUID().toString() + ".pdf";
			Path fileNameAndPath = Paths.get(PAY_UPLOAD_DIRECTORY, tokenizedName);
			fileName.append(tokenizedName);
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
            } 
            catch (FileAlreadyExistsException e){
                errors.add("The statement must not already exist.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrViewPayroll";
            }
            catch (Exception e) {
                errors.add("Please contact your IT administrator, an error has occurred with the file name");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrViewPayroll";
            }
			
			filePath = fileName.toString();
            statementNew.setDataLocation(filePath);
            SiteUser user = userService.findByUsername(username);
            statementNew.setForUser(username);
            user.getPay().add(statementNew);

            redirectAttributes.addFlashAttribute("success", "Pay statement added!");
            userService.addPay(statementNew, user);
        }
        
        return "redirect:/hrViewPayroll";
    }

    @GetMapping("/hrEditPayroll")
    public String hrEditPayroll(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        if(id == -1){
            return "redirect:/hrViewPayroll";
        }

        PayrollData data = userService.getPayrollById(id);
        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("errors", passedErrors);
        model.addAttribute("payroll", data);
        model.addAttribute("users", userService.getAllUsers());
        return "hrEditPayroll";
    }

    @PostMapping("/hrEditPayroll")
    public String hrEditPayrollSubmitted(@ModelAttribute PayrollData editedStatement, @RequestParam("user") String username, RedirectAttributes redirectAttributes, 
                                         @RequestParam("statement") MultipartFile file, @RequestParam(value = "id", defaultValue = "-1") int id){
        boolean userFlag = false, fileFlag = false, datesNotUploaded = editedStatement.getStartDate() == null || editedStatement.getEndDate() == null;
        List<String> errors = new ArrayList<String>();

        PayrollData data = userService.getPayrollById(id);

        if(!username.isEmpty()){
            userFlag = true;
        }

        if(datesNotUploaded){
            errors.add("Both the start and end date must be selected.");
        }

        if(!file.getOriginalFilename().isEmpty()){
            fileFlag = true;
        }

        if(!datesNotUploaded && editedStatement.getStartDate().after(editedStatement.getEndDate())){
            errors.add("The start date must be before the end date.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = Paths.get(PAY_UPLOAD_DIRECTORY, file.getOriginalFilename());
            fileName.append(file.getOriginalFilename());

            try {
                Files.delete(fileNameAndPath);
            }
            catch (Exception e) {
                return "redirect:/hrEditPayroll?id=" + id;
            }
            return "redirect:/hrEditPayroll?id=" + id;
        }
        else{
            String filePath = data.getDataLocation();
            if(fileFlag){
                StringBuilder fileName = new StringBuilder();
                String tokenizedName = UUID.randomUUID().toString() + ".pdf";
                Path fileNameAndPath = Paths.get(PAY_UPLOAD_DIRECTORY, tokenizedName);
                fileName.append(tokenizedName);
                try {
                    Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
                } 
                catch (FileAlreadyExistsException e){
                    errors.add("The statement must not already exist.");
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:/hrEditPayroll?id=" + id;
                }
                catch (Exception e) {
                    errors.add("Please contact your IT administrator, an error has occurred with the file name");
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:/hrViewPayroll?id=" + id;
                }
                
                filePath = fileName.toString();
                try {
                    Files.delete(Paths.get(PAY_UPLOAD_DIRECTORY, data.getDataLocation()));
                } catch (Exception e) {}
            }

            String nameToPass = data.getForUser();
            if(userFlag){
                SiteUser user = userService.findByUsername(data.getForUser());
                if(editedStatement == data){
                    user.getPay().remove(data);
                }
                
                editedStatement.setForUser(username);
                user.getPay().add(editedStatement);
                nameToPass = username;
            }

            editedStatement.setDataLocation(filePath);
            editedStatement.setForUser(nameToPass);
            redirectAttributes.addFlashAttribute("success", "Pay statement edited!");
            userService.editPay(editedStatement, userService.findByUsername(data.getForUser()));

            return "redirect:/hrViewPayroll";
        }
    }

    @GetMapping("/hrDeletePayroll")
    public String hrDeletePayroll(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        if(id == -1){
            return "redirect:/hrViewPayroll";
        }

        PayrollData data = userService.getPayrollById(id);

        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("payroll", data);
        return "hrDeletePayroll";
    }
    
    @PostMapping("/hrDeletePayroll")
    public String hrDeletePayrollSubmitted(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        if(id == -1){
            return "redirect:/hrViewPayroll";
        }

        try {
            Files.delete(Paths.get(PAY_UPLOAD_DIRECTORY, userService.getPayrollById(id).getDataLocation()));
        } catch (Exception e) {}
        
        userService.deletePay(userService.getPayrollById(id));
        redirectAttributes.addFlashAttribute("success", "Pay statement deleted!");
        return "redirect:/hrViewPayroll";
    }

    @GetMapping("/hrViewRequests")
    public String hrViewRequest(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        model.addAttribute("myUsers", userService.getAllUsers());

        return "hrViewRequest";
    }

    @GetMapping("/hrActionRequest")
    public String hrActionRequest(RedirectAttributes redirectAttributes, @RequestParam Map<String, String> params){
        int id = params.get("id") != null ? Integer.parseInt(params.get("id")) : -1;
        String approval = params.get("approval") != null ? params.get("approval") : "null";

        if(id == -1 || approval == "null"){
            List<String> errors = new ArrayList<String>();
            errors.add("Request cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewRequests";
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
            return "redirect:/hrViewRequests";
        }

        TimeOffRequest request = userService.getTimeOffRequestById(id);
        if(request.isHrApproved() && request.isManagerApproved()){
            List<String> errors = new ArrayList<String>();
            errors.add("Request has already been completed.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewRequests";
        }

        request.setHrApproved(approval.equals("true"));
        request.setHrViewed(true);
        userService.setRequestStatus(request, specifiedUser);
        checkForCompletedRequest(request);

        redirectAttributes.addFlashAttribute("success", approval.equals("true") ? "Request approved!" : "Request denied!");
        return "redirect:/hrViewRequests";
    }

    // ----------------------------------------- ADMIN -----------------------------------------
    @GetMapping("/adminMenu")
    public String adminMenu(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername();
        String role = getRole(username);
        
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", "TempCompany");
        return "adminMenu";
    }

    
    @GetMapping("/test")
    public String test(){
        userService.addUser("madison.bloom", "openSesame123!", "temp2095@gmail.com", "temp9@gmail.com", "HR_PAYROLL", "123-456-7890", "C322", "Madison", "Bloom", "Payroll Analyst", 3);
        userService.addSupervisedEmployee(userService.findByUsername("madison.bloom"), userService.findByUsername("ben.adams"));
        return "redirect:/index";
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

    private List<SiteUser> getSiteUsersOrderByLastName(){
        List<SiteUser> allUsers = userService.getAllUsers();
        SiteUserComparator userComparator = new SiteUserComparator();
        Collections.sort(allUsers, userComparator);

        return allUsers;
    }

    private void initializeModel(Model model){

    }

    private void checkForCompletedRequest(TimeOffRequest request){
        boolean viewed = request.isHrViewed() && request.isManagerViewed();
        boolean approved = request.isHrApproved() && request.isManagerApproved();
        boolean fullApproval = viewed && approved;
        boolean notApproved = viewed && !approved;
        SiteUser specifiedUser = null;

        // Send an email when request has been actioned by both sides to the user
        if(viewed && approved){
            Event e = new Event();
            LocalDateTime start = LocalDateTime.ofInstant(request.getStartDate().toInstant(), ZoneId.systemDefault()), end = LocalDateTime.ofInstant(request.getEndDate().toInstant(), ZoneId.systemDefault());
            for (SiteUser user : userService.getAllUsers()) {
                for (TimeOffRequest userRequest : user.getTimeOff()) {
                    if(request.getId() == userRequest.getId()){
                        specifiedUser = user;
                        break;
                    }
                }
            }

            e.setStart(start);
            e.setEnd(end);
            e.setText(specifiedUser.getUsername() + ": APPROVED TIME OFF");
            e.setPublicEvent(false);
            specifiedUser.setEntitledDays(specifiedUser.getEntitledDays() - 1);

            userService.addEventFromRequest(e, specifiedUser);
        }

        // When in a production environment, the email code can be uncommented
        if(fullApproval){
            try {
                System.out.println("SEND AN EMAIL!");
                //emailService.sendRequestStatusEmail(specifiedUser, request, true);
            } catch (Exception ex) {}
        }

        if(notApproved){
            try {
                System.out.println("SEND AN EMAIL!");
                //emailService.sendRequestStatusEmail(specifiedUser, request, false);
            } catch (Exception ex) {}
        }
    }
}
