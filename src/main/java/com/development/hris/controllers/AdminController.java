package com.development.hris.controllers;

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

import com.development.hris.entities.CustomWebAppElement;
import com.development.hris.entities.CustomWebAppElementComparator;
import com.development.hris.entities.Event;
import com.development.hris.entities.PayrollData;
import com.development.hris.entities.SiteUser;
import com.development.hris.service.ControllerUtilities;
import com.development.hris.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Date;
import java.util.regex.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@Slf4j
public class AdminController {
    private final ControllerUtilities controllerUtilities;
    private final UserService userService;

    // Directory for uploads to website
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/uploads";
    public static String LOGO_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/img";

    @GetMapping("/adminAppElements")
    public String getWebElements(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam Map<String, String> params){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username), searchTerm = params.get("search") != null ? params.get("search") : "";
        int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 1;
        int min = 0 + (page-1) * (int)ControllerUtilities.VIEW_PER_PAGE, max = ((int)ControllerUtilities.VIEW_PER_PAGE-1) + (page-1) * 25;
        
        List<CustomWebAppElement> allElements = userService.getAllElements();
        Collections.sort(allElements, new CustomWebAppElementComparator());

        // Filter the data if a search term is provided
        if(!searchTerm.isBlank()){
            List<CustomWebAppElement> temp = new ArrayList<CustomWebAppElement>(allElements);
            for (CustomWebAppElement element : temp) {
                if(!element.getDescription().toString().contains(searchTerm) && !element.getContent().contains(searchTerm) && !element.getContentLink().toString().contains(searchTerm) && 
                   !controllerUtilities.checkForStringInList(element.getContentList(), searchTerm)){
                    allElements.remove(element);
                }
            }
        }
        int totalPages = (int)Math.ceil(allElements.size()/ControllerUtilities.VIEW_PER_PAGE), nextPage = page + 1 > totalPages ? 1 : page + 1, prevPage = page - 1 < 1 ? totalPages : page - 1;
        
        List<CustomWebAppElement> toDisplay = new ArrayList<CustomWebAppElement>();
        try{
            for(int i = min; i <= max; i++){
                toDisplay.add(allElements.get(i));
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
        controllerUtilities.preparePagingModel(model, passedErrors, passedSuccess, nextPage, prevPage, searchTerm, userService.getAllElements().size(), allElements.size(), totalPages, page);
        controllerUtilities.prepareModelForEntities(model, "elements", toDisplay, true, "newElement", new CustomWebAppElement());
        return "adminViewElements";
    }

    @PostMapping("/adminNewElement")
    public String addNewElement(@ModelAttribute CustomWebAppElement newElement, RedirectAttributes redirectAttributes, @RequestParam(name="file", required = false) MultipartFile file, 
                                @RequestParam(name="content", required = false) String content, @RequestParam(name="list", required=false) String list){

        List<String> errors = new ArrayList<String>();
        boolean moreThanOneField = !content.isBlank() && !file.isEmpty();

        if(newElement.getDescription().isBlank()){
            errors.add("A description must be present.");
        }

        if(userService.getElementByDescription(newElement.getDescription()) != null){
            errors.add("The description must be unique (ie. not already in the system).");
        }

        if(moreThanOneField){
            errors.add("Only one field of content is allowed per element.");
        }

        if(!moreThanOneField && file.isEmpty() && content.isBlank()){
            errors.add("One of a file, a list, or text-based content must be present.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/adminAppElements";
        }

        String filePath = "";
        if(!file.isEmpty()){
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = newElement.getDescription().equals("logo") ? Paths.get(LOGO_DIRECTORY, file.getOriginalFilename()) : Paths.get(UPLOAD_DIRECTORY, file.getOriginalFilename());

            fileName.append(file.getOriginalFilename());
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
            } 
            catch (FileAlreadyExistsException e){
                errors.add("The file must not already exist.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/adminAppElements";
            }
            catch (Exception e) {
                errors.add("Please check the application file contents, an error has occurred with the file name");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/adminAppElements";
            }
            
            filePath = fileName.toString();
            newElement.setContentLink(filePath);
            newElement.setContent("");
            newElement.setContentList(null);
        }
        else if(list != null){
            if(newElement.getContent().isEmpty()){
                errors.add("Content must be present for a list");
                return "redirect:/adminAppElements";
            }

            List<String> contentLinks = Arrays.asList(newElement.getContent().split(","));
            newElement.setContent("");
            newElement.setContentLink("");
            newElement.setContentList(contentLinks);
        }
        else if(!content.isBlank()){
            newElement.setContent(content);
            newElement.setContentLink("");
            newElement.setContentList(null);
        }
        
        CustomWebAppElement added = userService.addOrEditElement(newElement);
        redirectAttributes.addFlashAttribute("success", "Element added!");
        log.info("New app element called " + added.getDescription() + " created with id#" + added.getId());
        return "redirect:/adminAppElements";
    }

    @GetMapping("/adminEditElement")
    public String editElement(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("Element cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/adminAppElements";
        }
        
        CustomWebAppElement customWebAppElement = userService.getElementById(id);
        if(customWebAppElement == null){
            List<String> errors = new ArrayList<String>();
            errors.add("Element cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/adminAppElements";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("element", customWebAppElement);
        return "editTemplate";
    }

    @PostMapping("/adminEditElement")
    public String editElementSubmitted(@ModelAttribute CustomWebAppElement element, RedirectAttributes redirectAttributes, @RequestParam(name="file", required = false) MultipartFile file, 
                                       @RequestParam(name="content", required = false) String content, @RequestParam(name="list", required=false) String list, 
                                       @RequestParam(value = "id", defaultValue = "-1") int id){
        
        List<String> errors = new ArrayList<String>();
        boolean moreThanOneField = !content.isBlank() && !file.isEmpty();
        boolean noContentChange = content.isBlank() && file.isEmpty() && list == null;
        CustomWebAppElement specified = userService.getElementById(id);
                                
        if(element.getDescription().isBlank()){
            errors.add("A description must be present.");
        }

        if(userService.getElementByDescription(element.getDescription()) != null && !specified.getDescription().equals(element.getDescription())){
            errors.add("The description must be unique (ie. not already in the system).");
        }

        if(moreThanOneField){
            errors.add("Only one field of content is allowed per element.");
        }

        if(!noContentChange && !moreThanOneField && file.isEmpty() && content.isBlank()){
            errors.add("One of a file, a list, or text-based content must be present.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/adminEditElement?id=" + id;
        }

        String filePath = specified.getContentLink();
        if(!noContentChange && !file.isEmpty()){
            StringBuilder fileName = new StringBuilder();
            Path fileNameAndPath = element.getDescription().equals("logo") ? Paths.get(LOGO_DIRECTORY, file.getOriginalFilename()) : Paths.get(UPLOAD_DIRECTORY, file.getOriginalFilename());
            fileName.append(file.getOriginalFilename());
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE_NEW);
            } 
            catch (FileAlreadyExistsException e){
                errors.add("The file must not already exist.");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/adminEditElement?id=" + id;
            }
            catch (Exception e) {
                errors.add("Please check the application file contents, an error has occurred with the file name");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:/adminEditElement?id=" + id;
            }
            
            if(!filePath.isBlank()){
                Path oldFilePath = Paths.get(UPLOAD_DIRECTORY, filePath);
                try {
                    Files.delete(oldFilePath);
                }
                catch (Exception e) {
                    log.info(e.toString());
                    return "redirect:/adminEditElement?id=" + id;
                }
                log.info(filePath + " will be replaced with " + fileName.toString());
            }
            else{
                log.info("Setting file " + fileName.toString() + " as a content link for element id#" + id);
            }

            filePath = fileName.toString();
            element.setContentLink(filePath);
            element.setContent("");
            element.setContentList(null);
        }
        else if(!noContentChange && list != null){
            List<String> contentLinks = Arrays.asList(element.getContent().split(","));
            element.setContent("");
            element.setContentLink("");
            element.setContentList(contentLinks);

            if(!filePath.isBlank()){
                Path oldFilePath = Paths.get(UPLOAD_DIRECTORY, filePath);
                System.out.println(oldFilePath);
                try {
                    Files.delete(oldFilePath);
                    log.info("Removing " + filePath);
                }
                catch (Exception e) {
                    log.info(e.toString());
                    return "redirect:/adminEditElement?id=" + id;
                }
            }
        }
        else if(!noContentChange && !content.isBlank()){
            element.setContent(content);
            element.setContentLink("");
            element.setContentList(null);

            if(!filePath.isBlank()){
                Path oldFilePath = Paths.get(UPLOAD_DIRECTORY, filePath);
                System.out.println(oldFilePath);
                try {
                    Files.delete(oldFilePath);
                    log.info("Removing " + filePath);
                }
                catch (Exception e) {
                    log.info(e.toString());
                    return "redirect:/adminEditElement?id=" + id;
                }
            }
        }

        if(noContentChange){
            element.setContent(specified.getContent());
            element.setContentList(specified.getContentList());
            element.setContentLink(specified.getContentLink());
        }
        
        CustomWebAppElement edited = userService.addOrEditElement(element);
        redirectAttributes.addFlashAttribute("success", "Element edited!");
        log.info("App element called " + edited.getDescription() + " with id#" + edited.getId() + " edited");
        return "redirect:/adminAppElements";
    }

    @GetMapping("/adminDeleteElement")
    public String deleteElement(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        List<String> errors = new ArrayList<String>();
        
        if(id == -1){
            errors.add("Element cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/adminAppElements";
        }

        CustomWebAppElement element = userService.getElementById(id);

        if(element == null){
            errors.add("Element cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/adminAppElements";
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("element", element);
        return "deleteTemplate";
    }

    @PostMapping("/adminDeleteElement")
    public String deleteElementSubmitted(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        if(id == -1){
            return "redirect:/adminAppElements";
        }

        CustomWebAppElement specified = userService.getElementById(id);

        if(specified == null){
            return "redirect:/adminAppElements";
        }

        if(!specified.getContentLink().isBlank()){
            try {
                Files.delete(Paths.get(UPLOAD_DIRECTORY, specified.getContentLink()));
            } catch (Exception e) {/* File not found */}
        }

        log.info(userDetails.getUsername() + " deleted element id#" + id + " with description " + specified.getDescription());
        userService.deleteElement(id);
        redirectAttributes.addFlashAttribute("success", "Element deleted!");
        return "redirect:/adminAppElements";
    }

    // USERS
    @PostMapping("/adminNewUser")
    public String addUser(@ModelAttribute SiteUser newUser, @RequestParam("managedBy") String managedBy, @RequestParam("role") String role,
                          RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails){
        List<String> errors = new ArrayList<String>();
        Pattern pattern;
        Matcher matcher;
        
        // Valid username: firstname.lastname with optional digits at end for repeat names
        pattern = Pattern.compile("[A-Za-z]+\\.[A-Za-z]+(\\d*)");
        matcher = pattern.matcher(newUser.getUsername());

        if(!matcher.matches()){
            errors.add("The username must be of the form: firstname.lastname");
        }

        if(userService.findByUsername(newUser.getUsername()) != null){
            errors.add("The username must be unique.");
        }

        // First name
        pattern = Pattern.compile("[A-Za-z]+(-*)[A-Za-z]+");
        matcher = pattern.matcher(newUser.getFirstName());
        if(!matcher.matches()){
            errors.add("The submitted first name is not legal");
        }

        // Last name
        pattern = Pattern.compile("[A-Za-z]+(\\. )*[A-Za-z]+");
        matcher = pattern.matcher(newUser.getLastName());
        if(!matcher.matches()){
            errors.add("The submitted last name is not legal");
        }

        // Job Title
        if(newUser.getJobTitle().isBlank()){
            errors.add("A job title must be present.");
        }

        // Email
		pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(newUser.getEmail());
		if(!matcher.matches()){
			errors.add("The email address is invalid.");
		}

        if(userService.findByEmail(newUser.getEmail()) != null){
            errors.add("The email address must be unique.");
        }

        // Role
        if(role.isBlank()){
            errors.add("A role must be selected.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        SiteUser user = userService.addUser(newUser.getUsername(), "openSesame123!", newUser.getEmail(), "", role, "", "", newUser.getFirstName(), 
                                            newUser.getLastName(), newUser.getJobTitle(), 3, managedBy, new Date());
        log.info("User " + user.getUsername() + " has been added with id#" + user.getId());
        redirectAttributes.addFlashAttribute("success", "User added!");
        return "redirect:/gal";
    }

    @GetMapping("/adminToggle")
    public String toggleUser(RedirectAttributes redirectAttributes, @RequestParam(value = "id", defaultValue = "-1") int id, @AuthenticationPrincipal UserDetails userDetails){
        SiteUser user = userService.findUserById(id);
        
        if(user.getUsername().equals("rootUser")){
            List<String> errors = new ArrayList<String>();
            errors.add("You cannot disable the root user.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }
        else if(userDetails.getUsername().equals(user.getUsername())){
            List<String> errors = new ArrayList<String>();
            errors.add("You cannot disable your own account while logged in.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        userService.toggleUser(user.getUsername(), user.isEnabled());
        log.info("User " + user.getUsername() + " enabled status is now " + user.isEnabled());
        redirectAttributes.addFlashAttribute("success", user.isEnabled() ? "User is now enabled!" : "User is now disabled!");
        return "redirect:/gal";
    }

    @GetMapping("/adminResetUser")
    public String resetUser(RedirectAttributes redirectAttributes, @RequestParam(value = "id", defaultValue = "-1") int id, @AuthenticationPrincipal UserDetails userDetails){
        SiteUser user = userService.findUserById(id);

        if(user.getUsername().equals("rootUser")){
            List<String> errors = new ArrayList<String>();
            errors.add("You cannot reset the root user.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }
        else if(userDetails.getUsername().equals(user.getUsername())){
            List<String> errors = new ArrayList<String>();
            errors.add("You cannot reset your own account while logged in.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        log.info("User " + user.getUsername() + " has been reset!");
        userService.resetPassword(user.getUsername());
        redirectAttributes.addFlashAttribute("success", "User has been reset! Please notify them to login and change their password immediately!");
        return "redirect:/gal";
    }

    @GetMapping("/adminEditUser")
    public String editUserAsAdmin(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);

        if(id == -1){
            List<String> errors = new ArrayList<String>();
            errors.add("User cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }
        
        SiteUser siteUser = userService.findUserById(id);
        if(siteUser == null){
            List<String> errors = new ArrayList<String>();
            errors.add("User cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        // Check for failure to display
		Object errors = model.asMap().get("errors");
		List<String> passedErrors = new ArrayList<String>();

        if(errors != null){
            passedErrors = ((ArrayList<String>)errors);
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("errors", passedErrors);
        model.addAttribute("userByAdmin", siteUser);
        return "editTemplate";
    }

    @PostMapping("/adminEditUser")
    public String editUserAsAdminSubmitted(@ModelAttribute SiteUser user, @RequestParam("managedBy") String managedBy, @RequestParam("role") String role,
                                    RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id,
                                    final HttpServletRequest httpServletRequest){
        if(id == -1){
            return "redirect:/gal";
        }

        SiteUser specified = userService.findUserById(id);
        List<String> errors = new ArrayList<String>();
        Pattern pattern;
        Matcher matcher;
        
        // Valid username: firstname.lastname with optional digits at end for repeat names
        pattern = Pattern.compile("[A-Za-z]+\\.[A-Za-z]+(\\d*)");
        matcher = pattern.matcher(user.getUsername());

        if(!matcher.matches()){
            errors.add("The username must be of the form: firstname.lastname");
        }

        if(userService.findByUsername(user.getUsername()) != null && !specified.getUsername().equals(user.getUsername())){
            errors.add("The username must be unique.");
        }

        // First name
        pattern = Pattern.compile("[A-Za-z]+(-*)[A-Za-z]+");
        matcher = pattern.matcher(user.getFirstName());
        if(!matcher.matches()){
            errors.add("The submitted first name is not legal");
        }

        // Last name
        pattern = Pattern.compile("[A-Za-z]+(\\. )*[A-Za-z]+");
        matcher = pattern.matcher(user.getLastName());
        if(!matcher.matches()){
            errors.add("The submitted last name is not legal");
        }

        // Job Title
        if(user.getJobTitle().isBlank()){
            errors.add("A job title must be present.");
        }

        // Email
		pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(user.getEmail());
		if(!matcher.matches()){
			errors.add("The email address is invalid.");
		}

        if(userService.findByEmail(user.getEmail()) != null  && !specified.getEmail().equals(user.getEmail())){
            errors.add("The email address must be unique.");
        }

        if(errors.size() > 0){
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        // Did the role change?
        if(!role.isBlank()){
            specified.setRole(user.getRole());
        }

        // Did the manager change?
        if(managedBy == null){
            specified.setManagedBy(specified.getManagedBy().isBlank() ? "" : specified.getManagedBy());
        }
        else if(!managedBy.isBlank()){
            specified.setManagedBy(managedBy.equals(specified.getManagedBy()) ? specified.getManagedBy() : managedBy);
        }

        String oldUsername = specified.getUsername();
        specified.setUsername(user.getUsername());
        specified.setFirstName(user.getFirstName());
        specified.setLastName(user.getLastName());
        specified.setEmail(user.getEmail());
        specified.setJobTitle(user.getJobTitle());

        userService.saveUser(specified);
        log.info("User " + specified.getUsername() + " with id#" + user.getId() + " has been edited");
        redirectAttributes.addFlashAttribute("success", "User edited!");
        if(!oldUsername.equals(specified.getUsername())){
            List<SiteUser> managedUsersByThisUser = userService.getAllUsers().stream().filter(u -> u.getManagedBy().equals(oldUsername)).collect(Collectors.toList());
            for (SiteUser siteUser : managedUsersByThisUser) {
                siteUser.setManagedBy(managedBy);
                userService.saveUser(siteUser);
            }

            List<PayrollData> temp = specified.getPay();
            for(PayrollData pay : temp){
                specified.getPay().remove(pay);
                pay.setForUser(user.getUsername());
                specified.getPay().add(pay);
                userService.editPay(pay, specified);
            }

            List<Event> tempEvents = specified.getEvents();
            for(Event event : tempEvents){
                if(event.getText().contains(oldUsername)){
                    event.setText(event.getText().replace(oldUsername, user.getUsername()));
                }
            }
            log.info("Managed users, pay, and events have been transferred to the new username");

            try {
                httpServletRequest.logout();
            } catch (Exception e) {
                // Logout failure
            }
            
            return "redirect:/login";
        }

        return "redirect:/gal";
    }

    @GetMapping("/adminDeleteUser")
    public String deleteUser(Model model, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        String username = userDetails == null ? "" : userDetails.getUsername(), role = controllerUtilities.getRole(username);
        List<String> errors = new ArrayList<String>();

        if(id == -1){
            errors.add("User cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        SiteUser user = userService.findUserById(id);
        if(user == null){
            errors.add("User cannot be found.");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/gal";
        }

        controllerUtilities.prepareBaseModel(model, role, username);
        model.addAttribute("userByAdmin", user);
        return "deleteTemplate";
    }

    @PostMapping("/adminDeleteUser")
    public String deleteUserSubmitted(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "id", defaultValue = "-1") int id, RedirectAttributes redirectAttributes){
        if(id == -1){
            return "redirect:/gal";
        }

        SiteUser user = userService.findUserById(id);

        if(user == null){
            return "redirect:/gal";
        }

        userService.deleteUser(user.getUsername());
        log.info(userDetails.getUsername() + " deleted user with id#" + id);
        redirectAttributes.addFlashAttribute("success", "User deleted!");
        return "redirect:/gal";
    }
}
