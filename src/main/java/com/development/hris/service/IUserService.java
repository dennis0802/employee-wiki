package com.development.hris.service;

import java.util.Date;
import java.util.List;

import com.development.hris.entities.Event;
import com.development.hris.entities.News;
import com.development.hris.entities.OpenJob;
import com.development.hris.entities.PayrollData;
import com.development.hris.entities.SiteUser;
import com.development.hris.entities.TimeOffRequest;
import com.development.hris.entities.WhistleInfo;

public interface IUserService {
    /**
     * Add a new user
     * @param username User's username
     * @param password User's password
     * @param email User's email
     * @param altEmail User's alternate email
     * @param role User's role in the system
     * @param phoneNum User's phone number (###-###-####)
     * @param workLocation User's work location
     * @param firstName User's first name
     * @param lastName User's last name
     * @param jobTitle User's job title
     * @param entitledDays User's entitled days off
     * @param managedBy Who this user is managed by
     * @param joinDate When the user joined
     * @return The added user
     */
    public SiteUser addUser(String username, String password, String email, String altEmail, String role, String phoneNum, String workLocation, String firstName, String lastName, 
                        String jobTitle, int entitledDays, String managedBy, Date joinDate);

    /**
     * Find a user by username
     * @param username The username to search for
     * @return The user, or null if cannot find a user
     */
    public SiteUser findByUsername(String username);

    /**
     * Toggle whether a user is archived (enabled)
     * @param username The user's username
     * @param isArchiving Is the user being archived?
     */
    public void toggleUser(String username, boolean isArchiving);

    /**
     * Delete a user
     * @param username The username of the user
     */
    public void deleteUser(String username);

    /**
     * Get all users in the repository
     * @return A list of users in the repository
     */
    public List<SiteUser> getAllUsers();

    /**
     * Edit a user (this will be changed)
     * @param username User's username
     * @param firstName User's first name
     * @param lastName User's last name
     * @param jobTitle User's job title
     * @param date User's join date
     * @return True if successful, false if cannot be found
     */
    boolean editUser(String username, String firstName, String lastName, String jobTitle, Date date);

    /**
     * Add a supervisor to an employee
     * @param employee The employee
     * @param supervisor The supervisor
     */
    void addSupervisedEmployee(SiteUser employee, SiteUser supervisor);
    
    // PAY
    /**
     * Get all pay data in the repository
     * @return A list of data in the pay repository
     */
    List<PayrollData> getAllPayData();

    /**
     * Add a new pay statement
     * @param pay The new pay statement
     * @param user The user who owns the pay data
     * @return The id of the new statement
     */
    long addPay(PayrollData pay, SiteUser user);

    /**
     * Edit a pay statement
     * @param editedStatement The edited statement
     * @param user The user who owns the statement
     */
    void editPay(PayrollData editedStatement, SiteUser user);

    /**
     * Delete a pay statement
     * @param statement The statement to delete
     */
    void deletePay(PayrollData statement);
    /**
     * Get payroll by its id
     * @param id The id of the payroll
     * @return The payroll, or null if id cannot be found
     */
    PayrollData getPayrollById(long id);

    // TIME REQUESTS
    /**
     * Add a time off request
     * @param request The request to add
     * @param user The user who created the request
     * @return The id of the request
     */
    long addTimeOffRequest(TimeOffRequest request, SiteUser user);

    /**
     * Get a time off request by its id
     * @param id The id of the request
     * @return The request, or null if id cannot be found
     */
    TimeOffRequest getTimeOffRequestById(long id);

    /**
     * Set the status of the request after it was reviewed by manager/HR
     * @param request The reviewed request
     * @param user The user this request belongs to
     */
    void setRequestStatus(TimeOffRequest request, SiteUser user);

    /**
     * Edit a request
     * @param request The edited request
     * @param oldRequest The old request
     * @param user The user this request belongs to
     */
    public void editRequest(TimeOffRequest request, TimeOffRequest oldRequest, SiteUser user);

    /**
     * Delete a time off request
     * @param request The specified request
     * @param user The user's request to delete
     */
    void deleteRequest(TimeOffRequest request, SiteUser user);

    /**
     * Add an event from a user's time off request
     * @param e The event to create
     * @param user The user who created the request
     */
    void addEventFromRequest(Event e, SiteUser user);

    // NEWS
    /**
     * Get all news in the news repository
     * @return A list of news data in the news repository
     */
    List<News> getAllNews();

    /**
     * Add or edit a news article
     * @param article The article to add/edit
     * @return The news article that was added/edited.
     */
    News addOrEditNews(News article);

    /**
     * Get an article by id
     * @param id The id of the article
     * @return The article, or null if not found
     */
    News getNewsById(long id);

    /**
     * Delete a news article
     * @param id The id of the article to delete
     */
    void deleteNews(long id);

    // WHISTLE INFO
    /**
     * Add a whistleblower submission
     * @param submission The submission
     * @return The added submission
     */
    WhistleInfo addSubmission(WhistleInfo submission);

    /**
     * Get all whistleblower submissions
     * @return A list of the submissions
     */
    List<WhistleInfo> getAllSubmissions();

    // OPEN JOB POSTINGS
    /**
     * Get all open job postings
     * @return A list of open job postings
     */
    List<OpenJob> getAllJobs();

    /**
     * Add or edit a job posting
     * @param posting The posting to add/edit
     */
    OpenJob addOrEditPosting(OpenJob posting);

    /**
     * Get an posting by id
     * @param id The id of the posting
     * @return The posting, or null if not found
     */
    OpenJob getJobById(long id);

    /**
     * Delete a posting
     * @param id The id of the posting to delete
     */
    void deleteJob(long id);
}
