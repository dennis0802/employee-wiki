package com.development.hris.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.development.hris.entities.CustomWebAppElement;
import com.development.hris.entities.Event;
import com.development.hris.entities.SiteUser;
import com.development.hris.entities.SiteUserComparator;
import com.development.hris.entities.TimeOffRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ControllerUtilities {
    private final UserService userService;
    private final EmailService emailService;

    public static float VIEW_PER_PAGE = 25;

    /**
     * Check for a completed request
     * @param request The time off request to check
     * @param user The user who owns this request
     */
    public void checkForCompletedRequest(TimeOffRequest request, SiteUser user){
        boolean viewed = request.isHrViewed() && request.isManagerViewed();
        boolean approved = request.isHrApproved() && request.isManagerApproved();
        boolean fullApproval = viewed && approved;
        boolean notApproved = viewed && !approved;

        // User is not managed by anyone, only HR must have approved
        if(user.getManagedBy().isBlank()){
            fullApproval = request.isHrApproved() && request.isHrViewed();
        }

        // When in a production environment, the email code can be uncommented
        if(fullApproval){
            Event e = new Event();
            LocalDateTime start = LocalDateTime.ofInstant(request.getStartDate().toInstant(), ZoneId.systemDefault()), end = LocalDateTime.ofInstant(request.getEndDate().toInstant(), ZoneId.systemDefault());

            e.setStart(start);
            e.setEnd(end);
            e.setText(user.getUsername() + ": APPROVED TIME OFF");
            e.setPublicEvent(false);
            user.setEntitledDays(user.getEntitledDays() - 1);

            userService.addEventFromRequest(e, user);

            try {
                log.info("Send an email with approved request.");
                //emailService.sendRequestStatusEmail(specifiedUser, request, true);
            } catch (Exception ex) {}
        }

        if(notApproved){
            try {
                log.info("Send an email with unapproved request.");
                //emailService.sendRequestStatusEmail(specifiedUser, request, false);
            } catch (Exception ex) {}
        }
    }

    /**
     * Get the user's role
     * @param username The username of the user
     * @return The user's role, empty string otherwise
     */
    public String getRole(String username){
        SiteUser user = userService.findByUsername(username);

        if(user != null){
            return user.getRole();
        }
        return "";
    }

    /**
     * Get the company string
     * @return The company name if the element is set, COMPANY NOT SET otherwise
     */
    public String getCompany(){
        CustomWebAppElement companyElement = userService.getElementByDescription("company");
        return companyElement == null ? "COMPANY NOT SET" : companyElement.getContent().toString();
    }

    /**
     * Get the org chart location
     * @return The location of the orgChart if the element is set, CHART NOT SET otherwise
     */
    public String getOrgChartLocation(){
        CustomWebAppElement chartElement = userService.getElementByDescription("orgChart");
        return chartElement == null ? "CHART NOT SET" : chartElement.getContentLink().toString();
    }

    /**
     * Get the logo
     * @return The location of the logo if the element is set, null otherwise
     */
    public String getLogo(){
        CustomWebAppElement logoElement = userService.getElementByDescription("logo");
        return logoElement == null ? "" : logoElement.getContentLink().toString();
    }

    /**
     * Get the current calendar year
     * @return The current calendar year
     */
    public int getYear(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        return calendar.get(Calendar.YEAR);
    }

    /**
     * Get the resources list
     * @return The resources list if the element is set, an empty list otherwise
     */
    public List<String> getResources(){
        CustomWebAppElement resourceElement = userService.getElementByDescription("resourceList");
        return resourceElement == null ? new ArrayList<String>() : resourceElement.getContentList();
    }

    /**
     * Get users by their last name
     * @return A list of site users by their last names
     */
    public List<SiteUser> getSiteUsersOrderByLastName(){
        List<SiteUser> allUsers = userService.getAllUsers();
        SiteUserComparator userComparator = new SiteUserComparator();
        Collections.sort(allUsers, userComparator);

        return allUsers;
    }

    /**
     * Prepare the base model
     * @param model The model to modify
     * @param role The role of the current user
     * @param username The username of the current user
     */
    public void prepareBaseModel(Model model, String role, String username){
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", getCompany());
        model.addAttribute("logo", getLogo());
    }

    /**
     * Check for a string in a resource lsit
     * @param list The resource list to inspect
     * @param query The search term
     * @return False if the list or the term does not exist, true if it does exist
     */
    public boolean checkForStringInList(List<String> list, String query){
        boolean exists = false;

        if(list == null){
            return false;
        }

        for (String string : list) {
            if(string.contains(query)){
                exists = true;
                break;
            }
        }

        return exists;
    }

    /**
     * Prepare the paging model
     * @param model The model to modify
     * @param passedErrors A list of errors to pass
     * @param passedSuccess The passed success message
     * @param nextPage The next page
     * @param prevPage The previous page
     * @param searchTerm The search term
     * @param baseCount The count of all elements, including on subsequent pages
     * @param totalCount The count of elements on the page during a search
     * @param totalPages The total number of pages
     * @param currentPage The current page
     * @param toDisplay The list of entities to display
     * @param entityName The name of the entities
     */
    public void preparePagingModel(Model model, List<String> passedErrors, String passedSuccess, int nextPage, int prevPage, String searchTerm, int baseCount, int totalCount, int totalPages,
                                   int currentPage){
        model.addAttribute("errors", passedErrors);
        model.addAttribute("success", passedSuccess);
        model.addAttribute("nextPage", nextPage);
        model.addAttribute("prevPage", prevPage);
        model.addAttribute("searched", searchTerm);
        model.addAttribute("baseCount", baseCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
    }

    /**
     * Prepare the model for entities
     * @param model The model to modify
     * @param entityName The name of each entity in the toDisplay list
     * @param toDisplay The list of entities
     * @param addMenuPresent Is there a menu to add more entities on the model?
     * @param newEntityName The new entity's name
     * @param newEntity The type of new entity (ex. SiteUser)
     */
    public void prepareModelForEntities(Model model, String entityName, List<?> toDisplay, boolean addMenuPresent, String newEntityName, Object newEntity){
        model.addAttribute(entityName, toDisplay);
        if(addMenuPresent){
            model.addAttribute(newEntityName, newEntity);
        }
    }

    /**
     * Get HR's email
     * @return HR's email if the element is set, "[EMAIL NOT SET]" otherwise
     */
    public String getHrEmail(){
        CustomWebAppElement companyElement = userService.getElementByDescription("hrEmail");
        return companyElement == null ? "[EMAIL NOT SET]" : companyElement.getContent().toString();
    }
}
