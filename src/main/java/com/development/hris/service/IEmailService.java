package com.development.hris.service;

import java.io.UnsupportedEncodingException;

import com.development.hris.entities.SiteUser;
import com.development.hris.entities.TimeOffRequest;

import jakarta.mail.MessagingException;

public interface IEmailService {
    /**
     * Send an email noting the status of their request
     * @param user The user to send this email to
     * @param request The request
     * @param status Was the request approved?
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    void sendRequestStatusEmail(SiteUser user, TimeOffRequest request, boolean status) throws MessagingException, UnsupportedEncodingException;
}
