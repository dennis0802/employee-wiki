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

@Service
@RequiredArgsConstructor
public class ControllerUtilities {
    private final UserService userService;
    private final EmailService emailService;

    public static float VIEW_PER_PAGE = 25;

    public void checkForCompletedRequest(TimeOffRequest request){
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

    public String getRole(String username){
        SiteUser user = userService.findByUsername(username);

        if(user != null){
            return user.getRole();
        }
        return "";
    }

    public String getCompany(){
        CustomWebAppElement companyElement = userService.getElementByDescription("company");
        return companyElement == null ? "COMPANY NOT SET" : companyElement.getContent().toString();
    }

    public String getOrgChartLocation(){
        CustomWebAppElement chartElement = userService.getElementByDescription("orgChart");
        return chartElement == null ? "CHART NOT SET" : chartElement.getContentLink().toString();
    }

    public String getLogo(){
        CustomWebAppElement logoElement = userService.getElementByDescription("logo");
        return logoElement == null ? "" : logoElement.getContentLink().toString();
    }

    public int getYear(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        return calendar.get(Calendar.YEAR);
    }

    public List<String> getResources(){
        CustomWebAppElement resourceElement = userService.getElementByDescription("resourceList");
        return resourceElement == null ? new ArrayList<String>() : resourceElement.getContentList();
    }

    public List<SiteUser> getSiteUsersOrderByLastName(){
        List<SiteUser> allUsers = userService.getAllUsers();
        SiteUserComparator userComparator = new SiteUserComparator();
        Collections.sort(allUsers, userComparator);

        return allUsers;
    }

    public void prepareBaseModel(Model model, String role, String username){
        model.addAttribute("role", role);
        model.addAttribute("username", username);
        model.addAttribute("year", getYear());
        model.addAttribute("company", getCompany());
        model.addAttribute("logo", getLogo());
    }

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

    public void prepareModelForEntities(Model model, String entityName, List<?> toDisplay, boolean addMenuPresent, String newEntityName, Object newEntity){
        model.addAttribute(entityName, toDisplay);
        if(addMenuPresent){
            model.addAttribute(newEntityName, newEntity);
        }
    }

    public String getHrEmail(){
        CustomWebAppElement companyElement = userService.getElementByDescription("hrEmail");
        return companyElement == null ? "[EMAIL NOT SET]" : companyElement.getContent().toString();
    }
}
