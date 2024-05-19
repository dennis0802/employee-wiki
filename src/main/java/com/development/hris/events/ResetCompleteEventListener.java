package com.development.hris.events;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import org.springframework.context.ApplicationListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.development.hris.entities.SiteUser;
import com.development.hris.service.ControllerUtilities;
import com.development.hris.service.UserService;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResetCompleteEventListener implements ApplicationListener<ResetCompleteEvent>{
    private final UserService userService;
    private final JavaMailSender mailSender;
    private final ControllerUtilities controllerUtilities;
    private String email;
    private SiteUser user;

    @Override
    public void onApplicationEvent(ResetCompleteEvent event){
        // Get newly created user
        email = event.getEmail();

        // Create verification token
        String resetToken = UUID.randomUUID().toString();
        try {
            user = userService.findByEmail(email);
        } catch (Exception e) {
            return;
        }
        
        // Save the verification token
        userService.saveUserResetToken(user, resetToken);

        // Build verification url
        String url = event.getApplicationUrl() + "/resetPassword?token=" + resetToken;

        // Send the email
        try {
            sendResetEmail(url);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        log.info("Click the link to reset your password : {}", url);
    }

    /**
     * Send the verification email
     * @param url The url for the user to verify their email
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public void sendResetEmail(String url) throws MessagingException, UnsupportedEncodingException{
        String subject = "Password Reset";
        String content = "Hi " + user.getUsername() + ",\n\n" +
                         "Please click the link below to reset your password by verifying your email.\n\n" +
                         url + "\n\n"+
                         "This URL will be active for 24 hours. After this validity period, you must submit your email again.\n\n" + 
                         "Thank you.\n\nEmployee Wiki"
        ;

        // Create message
        MimeMessage message = mailSender.createMimeMessage();
        String email = this.email;
        message.setFrom(controllerUtilities.getHrEmail());
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
