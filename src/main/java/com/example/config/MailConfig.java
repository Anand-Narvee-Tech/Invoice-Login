package com.example.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("host.narveetech.com");
        mailSender.setPort(465); // SSL Port
        mailSender.setUsername("no-reply@singularanalysts.com");
        mailSender.setPassword("qL!DO@{^Uci{tHyx");
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.debug", "true"); // for debugging logs

        mailSender.setDefaultEncoding("UTF-8");
        props.put("mail.smtp.from", "no-reply@singularanalysts.com");
     // Force JavaMail to always use your company email


     
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@singularanalysts.com"); // 
       message.setTo();
       message.setSubject("verify your otp");
       message.setText("Your OTP is: " );
        return mailSender;
    }
}
