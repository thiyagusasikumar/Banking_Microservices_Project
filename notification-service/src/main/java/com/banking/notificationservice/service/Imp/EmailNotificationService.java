package com.banking.notificationservice.service.Imp;



import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EmailNotificationService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

    public void sendRegistrationEmail(String recipientEmail,String customerName) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setFrom(sender);
        mailMessage.setTo(recipientEmail);
        mailMessage.setSubject("Registration Confirmation");
        mailMessage.setText(
                "Hello " + customerName + ",\n\n" +
                        "Your banking account registration " +
                        "was successful.\n\n" +
                        "Thank you."
        );
        mailSender.send(mailMessage);
    }
}
