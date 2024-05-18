package com.development.hris.controllers;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.development.hris.entities.News;
import com.development.hris.entities.NewsComparator;
import com.development.hris.entities.OpenJob;
import com.development.hris.entities.OpenJobComparator;
import com.development.hris.entities.PayrollData;
import com.development.hris.entities.PayrollDataComparator;
import com.development.hris.entities.SiteUser;
import com.development.hris.entities.SiteUserComparator;
import com.development.hris.entities.TimeOffRequest;
import com.development.hris.entities.WhistleInfo;
import com.development.hris.entities.WhistleInfoComparator;
import com.development.hris.service.ControllerUtilities;
import com.development.hris.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HrController {
    // Directory for pay uploads
	public static String PAY_UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/uploads/pay";

    // Directory for news image uploads
    public static String NEWS_IMG_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/uploads/news";

    // Directory for job postings
    public static String POSTINGS_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/uploads/postings";

    private final UserService userService;
    private final ControllerUtilities controllerUtilities;

    @GetMapping("/hrMenu")
    public String hrMenu(Model model, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        
        controllerUtilities.prepareBaseModel(model, role, username);
        return "hrMenu";
    }

    @GetMapping("/hrViewPayroll")
    public String hrViewPayroll(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username), searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
        
        List<SiteUser> allUsers = controllerUtilities.getSiteUsersOrderByLastName();
        List<PayrollData> allPayroll = userService.getAllPayData();

        PayrollDataComparator payrollDataComparator = new PayrollDataComparator();
        Collections.sort(allPayroll, payrollDataComparator);
        Collections.reverse(allPayroll);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<PayrollData> temp = new ArrayList<PayrollData>(allPayroll);
            for (PayrollData payrollData : temp) {
                if(!payrollData.getEndDate().toString().contains(searchTerm) && !payrollData.getStartDate().toString().contains(searchTerm) &&
                    !payrollData.getForUser().contains(searchTerm)){
                    allPayroll.remove(payrollData);
                    allUsers.remove(userService.findByUsername(payrollData.getForUser()));
                }
            }
        }
        int totalPages = (int)Math.ceil(allPayroll.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<PayrollData> toDisplay = new ArrayList<PayrollData>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allPayroll.get(i));
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
        controllerUtilities.preparePagingModel(model, passedErrors, passedSuccess, nextPage, prevPage, searchTerm, userService.getAllPayData().size(), allPayroll.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "statements", toDisplay, true, "statementNew", new PayrollData());
        return "hrViewPayroll";
    }

    @PostMapping("/hrNewStatement")
    public String newStatement(@ModelAttribute PayrollData statementNew, @RequestParam("user") String username, RedirectAttributes redirectAttributes, 
                               @RequestParam("statement") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();
        boolean datesNotUploaded = statementNew.getStartDate() == null ||statementNew.getEndDate() == null;
        String filePath = "";
        
        if(username.isBlank()){
            errors.add("A user must be selected.");
        }

        if(datesNotUploaded){
            errors.add("Both the start and end date must be selected.");
        }

        if(file.getOriginalFilename().isBlank()){
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
        long id = userService.addPay(statementNew, user);
        log.info(userDetails.getUsername() + " added pay statement id#" + id);
        return "redirect:/hrViewPayroll";
    }

    @GetMapping("/hrEditPayroll")
    public String hrEditPayroll(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Pay data cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewPayroll";
        }

        PayrollData data = userService.getPayrollById(id);
        if(data == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Pay data cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewPayroll";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("payroll", data);
        model.addAttribute("users", userService.getAllUsers());
        return "hrEditPayroll";
    }

    @PostMapping("/hrEditPayroll")
    public String hrEditPayrollSubmitted(@ModelAttribute PayrollData editedStatement, @RequestParam("user") String username, RedirectAttributes redirectAttributes, 
                                         @RequestParam("statement") MultipartFile file, @RequestParam(value = "id", defaultValue = "-1") int id,
                                         @AuthenticationPrincipal UserDetails userDetails){
        boolean userFlag = false, fileFlag = false, datesNotUploaded = editedStatement.getStartDate() == null || editedStatement.getEndDate() == null;
        List<String> errors = new ArrayList<String>();

        PayrollData data = userService.getPayrollById(id);

        if(!username.isBlank()){
            userFlag = true;
        }

        if(datesNotUploaded){
            errors.add("Both the start and end date must be selected.");
        }

        if(!file.getOriginalFilename().isBlank()){
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
                log.info(data.getDataLocation() + " will be replaced with " + filePath);
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

            log.info(userDetails.getUsername() + " edited pay id#" + id);
            return "redirect:/hrViewPayroll";
        }
    }

    @GetMapping("/hrDeletePayroll")
    public String hrDeletePayroll(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        
        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Pay data cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewPayroll";
        }

        PayrollData data = userService.getPayrollById(id);

        if(data == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Pay data cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewPayroll";
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("payroll", data);
        return "hrDeletePayroll";
    }
    
    @PostMapping("/hrDeletePayroll")
    public String hrDeletePayrollSubmitted(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, 
                                            RedirectAttributes redirectAttributes){
        if(id == -1){
            return "redirect:/hrViewPayroll";
        }

        try {
            Files.delete(Paths.get(PAY_UPLOAD_DIRECTORY, userService.getPayrollById(id).getDataLocation()));
        } catch (Exception e) {}
        
        log.info(userDetails.getUsername() + " deleted pay id#" + id);
        userService.deletePay(userService.getPayrollById(id));
        redirectAttributes.addFlashAttribute("success", "Pay statement deleted!");
        return "redirect:/hrViewPayroll";
    }

    @GetMapping("/hrViewRequests")
    public String hrViewRequest(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;

        List<SiteUser> allUsers = userService.getAllUsers();
        Collections.sort(allUsers, new SiteUserComparator());
        Collections.reverse(allUsers);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<SiteUser> temp = new ArrayList<SiteUser>(allUsers);
            for (SiteUser user : temp) {
                if(!user.getFirstName().contains(searchTerm) && !user.getLastName().contains(searchTerm)){
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
        
        controllerUtilities.prepareBaseModel(model, role, username);
        controllerUtilities.preparePagingModel(model, null, null, nextPage, prevPage, searchTerm, userService.getAllUsers().size(), allUsers.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "myUsers", toDisplay, false, null, null);
        return "hrViewRequest";
    }

    @GetMapping("/hrActionRequest")
    public String hrActionRequest(RedirectAttributes redirectAttributes, @RequestParam Map<String, String> params, @AuthenticationPrincipal UserDetails userDetails){
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
        controllerUtilities.checkForCompletedRequest(request);

        log.info("HR viewed time-off request id#" + request.getId());
        redirectAttributes.addFlashAttribute("success", approval.equals("true") ? "Request approved!" : "Request denied!");
        return "redirect:/hrViewRequests";
    }

    @GetMapping("/hrViewNews")
    public String hrViewNews(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
       
        List<News> allNews = userService.getAllNews();

        Collections.sort(allNews, new NewsComparator());
        Collections.reverse(allNews);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<News> temp = new ArrayList<News>(allNews);
            for (News article : temp) {
                if(!article.getContent().contains(searchTerm) && !article.getAuthor().contains(searchTerm) && !article.getTitle().contains(searchTerm) && 
                   !article.getPostDate().toString().contains(searchTerm)){
                    allNews.remove(article);
                }
            }
        }
        int totalPages = (int)Math.ceil(allNews.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<News> toDisplay = new ArrayList<News>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allNews.get(i));
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
        controllerUtilities.preparePagingModel(model, passedErrors, passedSuccess, nextPage, prevPage, searchTerm, userService.getAllNews().size(), allNews.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "news", toDisplay, true, "newArticle", new News());
        return "hrViewNews";
    }

    @PostMapping("/hrNewArticle")
    public String postNewArticle(@ModelAttribute News newArticle, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile file,
                                 @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();

        if(newArticle.getTitle().isBlank()){
            errors.add("The article must have a title.");
        }

        if(newArticle.getContent().isBlank()){
            errors.add("The article must have content");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewNews";
        }

        String filePath = "";
        if(!file.isEmpty()){
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = Paths.get(NEWS_IMG_DIRECTORY, file.getOriginalFilename());
            fileName.append(file.getOriginalFilename());
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
            } 
            catch (FileAlreadyExistsException e){
                errors.add("The image must not already exist.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrViewNews";
            }
            catch (Exception e) {
                errors.add("Please contact your IT administrator, an error has occurred with the file name");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrViewNews";
            }
            
            filePath = fileName.toString();
            newArticle.setImageLocation(filePath);
        }

        // All verified
        SiteUser user = userService.findByUsername(userDetails.getUsername());
        String fullName = user.getFirstName() + " " + user.getLastName();

        newArticle.setContent(newArticle.getContent().replaceAll("(\r\n|\n)", "<br/>"));
        newArticle.setAuthor(fullName);
        newArticle.setPostDate(new Date());
        News added = userService.addOrEditNews(newArticle);

        log.info(user.getUsername() + " created a news posting with id#" + added.getId());
        return "redirect:/hrViewNews";
    }

    @GetMapping("/hrEditNews")
    public String editNews(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Article cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewNews";
        }

        News data = userService.getNewsById(id);
        if(data == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Article cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewNews";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("article", data);
        return "hrEditNews";
    }

    @PostMapping("/hrEditNews")
    public String editNewsSubmitted(@ModelAttribute News article, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile file, 
                                    @RequestParam(value = "id", defaultValue = "-1") int id, @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();
        boolean imageChangedFlag = false;

        if(article.getTitle().isBlank()){
            errors.add("The article must have a title.");
        }

        if(article.getContent().isBlank()){
            errors.add("The article must have content");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrEditNews?id=" + id;
        }

        String filePath = userService.getNewsById(id).getImageLocation();
        imageChangedFlag = !file.isEmpty() && !file.getOriginalFilename().equals(filePath);
        
        if(imageChangedFlag){
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = Paths.get(NEWS_IMG_DIRECTORY, file.getOriginalFilename());
            fileName.append(file.getOriginalFilename());
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
            } 
            catch (FileAlreadyExistsException e){
                errors.add("The image must not already exist.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrEditNews";
            }
            catch (Exception e) {
                System.out.println(e);
                errors.add("Please contact your IT administrator, an error has occurred with the file name");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrEditNews";
            }

            try {
                Files.delete(Paths.get(NEWS_IMG_DIRECTORY, userService.getNewsById(id).getImageLocation()));
            } catch (Exception e) {}
            filePath = fileName.toString();
        }

        SiteUser user = userService.findByUsername(userDetails.getUsername());
        String fullName = user.getFirstName() + " " + user.getLastName();

        article.setContent(article.getContent().replaceAll("(\r\n|\n)", "<br/>"));
        article.setAuthor(fullName);
        article.setPostDate(new Date());
        article.setImageLocation(filePath);
        userService.addOrEditNews(article);

        log.info(user.getUsername() + " edited posting id#" + id + ", titled " + userService.getNewsById(id).getTitle());
        redirectAttributes.addFlashAttribute("success", "Article edited!");
        return "redirect:/hrViewNews";
    }

    @GetMapping("/hrDeleteNews")
    public String hrDeleteNews(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        List<String> errors = new ArrayList<String>();
        
        if(id == -1){
            errors.add("Article cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewNews";
        }

        News article = userService.getNewsById(id);

        if(article == null){
            errors.add("Article cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewNews";
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("article", article);
        return "hrDeleteNews";
    }

    @PostMapping("/hrDeleteNews")
    public String hrDeleteNewsSubmitted(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        if(id == -1){
            return "redirect:/hrViewNews";
        }

        try {
            Files.delete(Paths.get(NEWS_IMG_DIRECTORY, userService.getNewsById(id).getImageLocation()));
        } catch (Exception e) {}
        
        log.info(userDetails.getUsername() + " deleted news posting id#" + id + ", titled " + userService.getNewsById(id).getTitle());
        userService.deleteNews(id);
        redirectAttributes.addFlashAttribute("success", "Article deleted!");
        return "redirect:/hrViewNews";
    }

    @GetMapping("/hrViewWhistleblower")
    public String viewWhistleblowerBox(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
        
        List<WhistleInfo> allSubmissions = userService.getAllSubmissions();
        Collections.sort(allSubmissions, new WhistleInfoComparator());
        Collections.reverse(allSubmissions);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<WhistleInfo> temp = new ArrayList<WhistleInfo>(allSubmissions);
            for (WhistleInfo submission : temp) {
                if(!submission.getContent().contains(searchTerm) && !submission.getSubject().contains(searchTerm) && !submission.getPostDate().toString().contains(searchTerm)){
                    allSubmissions.remove(submission);
                }
            }
        }
        int totalPages = (int)Math.ceil(allSubmissions.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<WhistleInfo> toDisplay = new ArrayList<WhistleInfo>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allSubmissions.get(i));
            }
        }
        catch(Exception e){/* Less than max per page */}

        controllerUtilities.prepareBaseModel(model, role, username);
        controllerUtilities.preparePagingModel(model, null, null, nextPage, prevPage, searchTerm, userService.getAllSubmissions().size(), allSubmissions.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "submissions", toDisplay, false, null, null);
        return "hrViewWhistleInfo";
    }

    @GetMapping("/hrViewJobs")
    public String hrViewJobs(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        String searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
        
        List<OpenJob> allJobs = userService.getAllJobs();
        Collections.sort(allJobs, new OpenJobComparator());
        Collections.reverse(allJobs);

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<OpenJob> temp = new ArrayList<OpenJob>(allJobs);
            for (OpenJob submission : temp) {
                if(!submission.getPosition().contains(searchTerm) && !submission.getDeadline().toString().contains(searchTerm) && !submission.getPostDate().toString().contains(searchTerm)){
                    allJobs.remove(submission);
                }
            }
        }
        int totalPages = (int)Math.ceil(allJobs.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<OpenJob> toDisplay = new ArrayList<OpenJob>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allJobs.get(i));
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
        controllerUtilities.preparePagingModel(model, passedErrors, passedSuccess, nextPage, prevPage, searchTerm, userService.getAllJobs().size(), allJobs.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "postings", toDisplay, true, "newPosting", new OpenJob(null, null, null, null, false));
        return "viewJobPostingList";
    }

    @PostMapping("/hrNewPosting")
    public String newJobPosting(@ModelAttribute OpenJob newPosting, RedirectAttributes redirectAttributes, @RequestParam("file") MultipartFile file, 
                                @RequestParam(name="active", required = false) String active, @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();

        if(newPosting.getPosition().isBlank()){
            errors.add("The posting must have a position name.");
        }

        if(newPosting.getDeadline() == null){
            errors.add("A deadline must be selected.");
        }

        if(newPosting.getDeadline() != null){
            if(newPosting.getDeadline().before(new Date())){
                errors.add("The deadline cannot have already past.");
            }
        }

        if(file.isEmpty()){
            errors.add("A posting file must be uploaded.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        String filePath = "";
        StringBuilder fileName = new StringBuilder();
        Path fileNameAndPath = Paths.get(POSTINGS_DIRECTORY, file.getOriginalFilename());
        fileName.append(file.getOriginalFilename());
        try {
            Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
        } 
        catch (FileAlreadyExistsException e){
            errors.add("The file must not already exist.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }
        catch (Exception e) {
            errors.add("Please contact your IT administrator, an error has occurred with the file name");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }
        
        filePath = fileName.toString();

        // All verified
        newPosting.setPostDate(new Date());
        newPosting.setLink(filePath);
        newPosting.setActive(active != null);
        OpenJob added = userService.addOrEditPosting(newPosting);

        redirectAttributes.addFlashAttribute("success", "Posting added!");
        log.info(userDetails.getUsername() + " added job posting for id#" + added.getId() + ", the " + added.getPosition() + "position");
        return "redirect:/hrViewJobs";
    }

    @GetMapping("/hrEditPosting")
    public String getEditPosting(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Posting cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        OpenJob openJob = userService.getJobById(id);
        if(openJob == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Posting cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("posting", openJob);
        return "hrEditPosting";
    }

    @PostMapping("/hrEditPosting")
    public String editJobPosting(@ModelAttribute OpenJob posting, RedirectAttributes redirectAttributes, @RequestParam("file") MultipartFile file, 
                                 @RequestParam(name="active", required = false) String active, @RequestParam(value = "id", defaultValue = "-1") int id,
                                 @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();

        if(id == -1){
            return "redirect:/hrViewJobs";
        }

        OpenJob specifiedJob = userService.getJobById(id);
        if(specifiedJob == null){
            return "redirect:/hrViewJobs";
        }

        if(posting.getPosition().isBlank()){
            errors.add("The posting must have a position name.");
        }

        if(posting.getDeadline() == null){
            errors.add("A deadline must be selected.");
        }

        if(posting.getDeadline() != null){
            if(posting.getDeadline().before(new Date())){
                errors.add("The deadline cannot have already past.");
            }
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrEditPosting?id=" + id;
        }

        String filePath = specifiedJob.getLink();
        if(!file.isEmpty()){
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = Paths.get(POSTINGS_DIRECTORY, file.getOriginalFilename());
            fileName.append(file.getOriginalFilename());
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
            } 
            catch (FileAlreadyExistsException e){
                errors.add("The file must not already exist.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrEditPosting?id=" + id;
            }
            catch (Exception e) {
                errors.add("Please contact your IT administrator, an error has occurred with the file name");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/hrEditPosting?id=" + id;
            }

            Path oldFilePath = Paths.get(POSTINGS_DIRECTORY, filePath);
            try {
                Files.delete(oldFilePath);
            }
            catch (Exception e) {
                log.info(e.toString());
                return "redirect:/hrViewJobs";
            }
            log.info(filePath + " will be replaced with " + fileName.toString());
            filePath = fileName.toString();
        }

        // All verified
        posting.setPostDate(new Date());
        posting.setLink(filePath);
        OpenJob editedPosting = userService.addOrEditPosting(posting);

        log.info(userDetails.getUsername() + " edited posting id#" + id + ", for the " + editedPosting.getPosition() + " position");
        redirectAttributes.addFlashAttribute("success", "Posting edited!");
        return "redirect:/hrViewJobs";
    }

    @GetMapping("/hrTogglePosting")
    public String togglePosting(@RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();

        if(id == -1){
            errors.add("Posting cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        OpenJob openJob = userService.getJobById(id);

        if(openJob == null){
            errors.add("Posting cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        openJob.setActive(!openJob.isActive());
        OpenJob toggled = userService.addOrEditPosting(openJob);
        log.info(userDetails.getUsername() + " toggled active status for posting #" + id + " for the " + toggled.getPosition() + "position to " + toggled.isActive());
        return "redirect:/hrViewJobs";
    }

    @GetMapping("/hrDeletePosting")
    public String hrDeleteJob(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        List<String> errors = new ArrayList<String>();
        
        if(id == -1){
            errors.add("Posting cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        OpenJob openJob = userService.getJobById(id);

        if(openJob == null){
            errors.add("Posting cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/hrViewJobs";
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("posting", openJob);
        return "hrDeletePosting";
    }

    @PostMapping("/hrDeletePosting")
    public String hrDeleteJobSubmitted(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        if(id == -1){
            return "redirect:/hrViewJobs";
        }

        try {
            Files.delete(Paths.get(POSTINGS_DIRECTORY, userService.getJobById(id).getLink()));
        } catch (Exception e) {/* File not found */}
        
        log.info(userDetails.getUsername() + " deleted posting id#" + id + ", for the " + userService.getJobById(id).getPosition() + " position");
        userService.deleteJob(id);
        redirectAttributes.addFlashAttribute("success", "Posting deleted!");
        return "redirect:/hrViewJobs";
    }
}
