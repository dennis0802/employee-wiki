package com.development.hris.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

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

    public int getYear(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        return calendar.get(Calendar.YEAR);
    }

    public List<SiteUser> getSiteUsersOrderByLastName(){
        List<SiteUser> allUsers = userService.getAllUsers();
        SiteUserComparator userComparator = new SiteUserComparator();
        Collections.sort(allUsers, userComparator);

        return allUsers;
    }
}
