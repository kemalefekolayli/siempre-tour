package com.siempretour.Contact;

import com.siempretour.Contact.Dto.ContactRequestDto;
import com.siempretour.Contact.Dto.ContactResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;
    private final JavaMailSender mailSender;

    @Value("${contact.target-email}")
    private String targetEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public ContactResponseDto submitContact(ContactRequestDto dto) {
        // Save to DB
        ContactMessage msg = new ContactMessage();
        msg.setName(dto.getName());
        msg.setEmail(dto.getEmail());
        msg.setSubject(dto.getSubject());
        msg.setMessage(dto.getMessage());

        // Try sending email
        try {
            if (mailUsername != null && !mailUsername.isBlank()) {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(targetEmail);
                mail.setReplyTo(dto.getEmail());
                mail.setSubject("[Siempre Tour İletişim] " + dto.getSubject());
                mail.setText(
                        "Gönderen: " + dto.getName() + "\n" +
                        "E-posta: " + dto.getEmail() + "\n\n" +
                        dto.getMessage()
                );
                mailSender.send(mail);
                msg.setEmailSent(true);
                log.info("Contact email sent to {} from {}", targetEmail, dto.getEmail());
            } else {
                log.warn("Mail username not configured, skipping email send. Message saved to DB.");
            }
        } catch (Exception e) {
            log.error("Failed to send contact email: {}", e.getMessage());
        }

        contactMessageRepository.save(msg);
        return new ContactResponseDto("Mesajınız başarıyla alındı.");
    }
}
