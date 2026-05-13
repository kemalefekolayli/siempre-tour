package com.siempretour.Contact.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequestDto {

    @NotBlank(message = "Ad alanı zorunludur")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "E-posta alanı zorunludur")
    @Email(message = "Geçerli bir e-posta adresi girin")
    private String email;

    @NotBlank(message = "Konu alanı zorunludur")
    @Size(max = 200)
    private String subject;

    @NotBlank(message = "Mesaj alanı zorunludur")
    @Size(max = 5000)
    private String message;
}
