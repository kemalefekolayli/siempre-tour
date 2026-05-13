package com.siempretour.Contact;

import com.siempretour.Contact.Dto.ContactRequestDto;
import com.siempretour.Contact.Dto.ContactResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactResponseDto> submitContact(@Valid @RequestBody ContactRequestDto dto) {
        log.info("Contact form submission from: {}", dto.getEmail());
        ContactResponseDto response = contactService.submitContact(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
