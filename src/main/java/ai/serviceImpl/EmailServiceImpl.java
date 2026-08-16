package ai.serviceImpl;

import ai.dto.EmailRequest;
import ai.exception.ServiceProvisioningException;
import ai.repository.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;


import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @PostConstruct
    public void checkMailConfiguration() {
        log.info("Mail username: {}", mailUsername);
        log.info("Mail password configured: {}", mailPassword != null && !mailPassword.isBlank());
        log.info("Mail password length: {}", mailPassword == null ? 0 : mailPassword.length());
    }

    @Override
    public void send(EmailRequest request) {

        try {
            // Create Thymeleaf context
            Context context = new Context();
            if (request.getVariables() != null) {
                context.setVariables(request.getVariables());
            }
            // Resolve template
            String htmlContent = templateEngine.process("email/" + request.getTemplateName(), context);
            // Create email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(request.getTo());
            if (request.getCc() != null) {
                helper.setCc(request.getCc());
            }
            if (request.getBcc() != null) {
                helper.setBcc(request.getBcc());
            }
            helper.setSubject(request.getSubject());
            // true = HTML email
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email sent successfully. template={}, to={}", request.getTemplateName(), request.getTo());

        } catch (MessagingException e) {
            log.error("Failed to send email. template={}, to={}", request.getTemplateName(), request.getTo(), e);
            throw new ServiceProvisioningException("Failed to send email", e);
        }
    }
}
