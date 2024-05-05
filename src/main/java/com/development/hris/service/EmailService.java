package com.development.hris.service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.development.hris.entities.SiteUser;
import com.development.hris.entities.TimeOffRequest;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    /**
     * Send the verification email
     * @param url The url for the user to verify their email
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public void sendRequestStatusEmail(SiteUser user, TimeOffRequest request, boolean status) throws MessagingException, UnsupportedEncodingException{
        String subject = "Time Off Request ID#" + request.getId().toString();
        String content = status ?
                         ("Hi " + user.getUsername() + ",\n\n" +
                         "Your time-off request has been approved.\n\n" +
                         "Thank you.")
                         :
                         ("Hi " + user.getUsername() + ",\n\n" +
                         "Your time-off request has been denied. If you believe this is a mistake, please discuss with your manager and/or HR.\n\n" +
                         "Thank you.")
        ;

        // Create message
        MimeMessage message = mailSender.createMimeMessage();
        String email = user.getEmail();
        message.setFrom("dennisdao2001@gmail.com");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject(subject);
        message.setText(content, "utf-8");
        message.setSentDate(new Date());

        // Create properties and session to send (GMAIL must have IMAP and POP active)
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth","true");
        props.put("mail.smtp.starttls.enable", true);
        Session session = Session.getDefaultInstance(props);
        Transport transport = session.getTransport("smtp");
        transport.connect("smtp.gmail.com", 587, System.getenv("APP_EMAIL"), System.getenv("APP_PASSWORD"));
        transport.sendMessage(message, message.getAllRecipients());

    }
}
