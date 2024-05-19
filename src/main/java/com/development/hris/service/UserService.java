package com.development.hris.service;

import com.development.hris.entities.*;
import com.development.hris.token.PasswordRefreshToken;
import com.development.hris.token.PasswordTokenRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Calendar;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final SiteUserRepository userRepository;
    private final EventRepository eventRepository;
    private final PayrollDataRepository payrollDataRepository;
    private final TimeOffRequestRepository timeOffRequestRepository;
    private final NewsRepository newsRepository;
    private final WhistleInfoRepository whistleInfoRepository;
    private final CustomWebAppElementRepository customWebAppElementRepository;
    private final OpenJobRepository openJobRepository;
    private final PasswordTokenRepository passwordRepository;

    public SiteUser addUser(String username, String password, String email, String altEmail, String role, String phoneNum, String workLocation, String firstName, String lastName, 
                        String jobTitle, int entitledDays, String managedBy, Date joinDate){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        SiteUser user = new SiteUser(username, encoder.encode(password), email, altEmail, role, phoneNum, workLocation, firstName, lastName, jobTitle, entitledDays, managedBy, joinDate);
        user.setEnabled(false);
        return userRepository.save(user);
    }

    public SiteUser findByUsername(String username){
        if(userRepository.findByUsername(username).isPresent()){
            return userRepository.findByUsername(username).get();
        }
        return null;
    }

    public SiteUser findByEmail(String email){
        if(userRepository.findByEmail(email).isPresent()){
            return userRepository.findByEmail(email).get();
        }
        return null;
    }

    public SiteUser findUserById(long id){
        return userRepository.findById(id);
    }

    public void toggleUser(String username, boolean isEnabled){
        SiteUser user = findByUsername(username);
        if(user != null && isEnabled){
            user.setEnabled(false);
        }
        else if(user != null && !isEnabled){
            user.setEnabled(true);
        }

        userRepository.save(user);
    }

    public void deleteUser(String username){
        SiteUser user = findByUsername(username);
        
        if(user != null){
            List<Event> events = new ArrayList<Event>(user.getEvents());
            if(events.size() > 0){
                for (Event event : events) {
                    user.getEvents().remove(event);
                    eventRepository.delete(event);
                }
            }

            List<PayrollData> pay = new ArrayList<PayrollData>(user.getPay());
            if(pay.size() > 0){
                for(PayrollData yourPay : pay){
                    user.getPay().remove(yourPay);
                    payrollDataRepository.delete(yourPay);
                }
            }

            List<TimeOffRequest> timeOffRequests = new ArrayList<TimeOffRequest>(user.getTimeOff());
            if(timeOffRequests.size() > 0){
                for (TimeOffRequest yourRequest : timeOffRequests) {
                    user.getTimeOff().remove(yourRequest);
                    timeOffRequestRepository.delete(yourRequest);
                }
            }

            userRepository.delete(user);
        }
    }

    public List<SiteUser> getAllUsers(){
        return userRepository.findAll();
    }

    public boolean editUser(String username, String firstName, String lastName, String jobTitle, Date date){
        SiteUser user;
        try {
            user = userRepository.findByUsername(username).get();
        } catch (Exception e) {
            return false;
        }
        
        user.setJoinDate(date);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setJobTitle(jobTitle);
        user.setRole("ADMIN");
        userRepository.save(user);
        return true;
    }

    public void addSupervisedEmployee(SiteUser employee, SiteUser supervisor){
        employee.setManagedBy(supervisor.getUsername());
        userRepository.save(employee);
    }

    public void saveUser(SiteUser user){
        userRepository.save(user);
    }

    public void resetPassword(String username){
        SiteUser user = userRepository.findByUsername(username).isPresent() ? userRepository.findByUsername(username).get() : null;
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if(user == null){
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String pass = "EmployeeWiki" + calendar.get(Calendar.YEAR) + "!";
        user.setPassword(encoder.encode(pass));
        userRepository.save(user);
    }
    
    // PAY
    public List<PayrollData> getAllPayData(){
        return payrollDataRepository.findAll();
    }

    public long addPay(PayrollData pay, SiteUser user){
        PayrollData saved = payrollDataRepository.save(pay);
        userRepository.save(user);
        return saved.getId();
    }

    public void clearPay(){
        payrollDataRepository.deleteAll();
    }

    public void editPay(PayrollData editedStatement, SiteUser user){
        userRepository.save(user);
        payrollDataRepository.save(editedStatement);
    }

    public void deletePay(PayrollData statement){
        SiteUser user = userRepository.findByUsername(statement.getForUser()).get();
        user.getPay().remove(statement);
        userRepository.save(user);
        payrollDataRepository.delete(statement);
    }

    public PayrollData getPayrollById(long id){
        return payrollDataRepository.findById(id);
    }

    // TIME REQUESTS
    public long addTimeOffRequest(TimeOffRequest request, SiteUser user){
        TimeOffRequest saved = timeOffRequestRepository.save(request);
        userRepository.save(user);
        return saved.getId();
    }

    public TimeOffRequest getTimeOffRequestById(long id){
        return timeOffRequestRepository.findById(id);
    }

    public void setRequestStatus(TimeOffRequest request, SiteUser user){
        long id = request.getId();

        List<TimeOffRequest> requests = user.getTimeOff().stream().filter(r -> r.getId() == id).collect(Collectors.toList());
        TimeOffRequest specified = requests.get(0);
        user.getTimeOff().remove(specified);
        user.getTimeOff().add(request);

        userRepository.save(user);
        timeOffRequestRepository.save(request);
    }

    public void editRequest(TimeOffRequest request, TimeOffRequest oldRequest, SiteUser user){
        request.setManagerApproved(false);
        request.setHrApproved(false);
        request.setHrViewed(false);
        request.setManagerViewed(false);

        user.getTimeOff().remove(oldRequest);

        user.getTimeOff().add(request);
        userRepository.save(user);
        timeOffRequestRepository.save(request);
    }

    public void deleteRequest(TimeOffRequest request, SiteUser user){
        user.getTimeOff().remove(request);
        userRepository.save(user);
        timeOffRequestRepository.delete(request);
    }

    // EVENTS
    public void addEventFromRequest(Event e, SiteUser user){
        user.getEvents().add(e);
        
        eventRepository.save(e);
        userRepository.save(user);
    }

    // NEWS
    public List<News> getAllNews(){
        return newsRepository.findAll();
    }

    public News addOrEditNews(News article){
        return newsRepository.save(article);
    }

    public News getNewsById(long id){
        return newsRepository.findById(id);
    }

    public void deleteNews(long id){
        newsRepository.deleteById(id);
    }

    public void clearNews(){
        newsRepository.deleteAll();
    }

    // WHISTLE INFO
    public WhistleInfo addSubmission(WhistleInfo submission){
        submission.setPostDate(new Date());
        return whistleInfoRepository.save(submission);
    }

    public List<WhistleInfo> getAllSubmissions(){
        return whistleInfoRepository.findAll();
    }

    public void clearSubmissions(){
        whistleInfoRepository.deleteAll();
    }

    // OPEN JOB POSTINGS
    public List<OpenJob> getAllJobs(){
        return openJobRepository.findAll();
    }

    public OpenJob addOrEditPosting(OpenJob posting){
        return openJobRepository.save(posting);
    }

    public OpenJob getJobById(long id){
        return openJobRepository.findById(id);
    }

    public void deleteJob(long id){
        openJobRepository.deleteById(id);
    }

    // WEB ELEMENTS
    public CustomWebAppElement getElementByDescription(String description){
        return customWebAppElementRepository.findByDescription(description);
    }

    public List<CustomWebAppElement> getAllElements(){
        return customWebAppElementRepository.findAll();
    }

    public CustomWebAppElement getElementById(long id){
        return customWebAppElementRepository.findById(id);
    }

    public CustomWebAppElement addOrEditElement(CustomWebAppElement element){
        return customWebAppElementRepository.save(element);
    }

    public void deleteElement(long id){
        customWebAppElementRepository.deleteById(id);
    }

    // RESET
    public void saveUserResetToken(SiteUser user, String resetToken){
        var verification = new PasswordRefreshToken(resetToken, user);
        passwordRepository.save(verification);
    }

    public String validateResetTokenAndSetPassword(String verifyToken, String rawPass){
        PasswordRefreshToken token = passwordRepository.findByToken(verifyToken);

        // Invalid token
        if(token.equals(null)){
            return "invalid";
        }

        SiteUser user = token.getUser();
        Calendar calendar = Calendar.getInstance();

        // Expired token
        if((token.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0){
            passwordRepository.delete(token);
            return "expired";
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(rawPass));
        userRepository.save(user);

        return "valid";
    }

    public PasswordRefreshToken getTokenWithString(String token){
        return passwordRepository.findByToken(token);
    }

    public void deleteToken(PasswordRefreshToken token){
        passwordRepository.delete(token);
    }
}
