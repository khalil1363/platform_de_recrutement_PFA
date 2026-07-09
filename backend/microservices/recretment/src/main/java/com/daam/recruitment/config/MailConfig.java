package com.daam.recruitment.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            org.springframework.core.env.Environment env) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(env.getProperty("spring.mail.host", "smtp.gmail.com"));
        sender.setPort(env.getProperty("spring.mail.port", Integer.class, 587));
        sender.setUsername(env.getProperty("spring.mail.username"));
        sender.setPassword(env.getProperty("spring.mail.password"));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", env.getProperty("app.mail.debug", "false"));
        return sender;
    }
}
