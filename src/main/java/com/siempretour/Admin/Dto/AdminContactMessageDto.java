package com.siempretour.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContactMessageDto {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private boolean emailSent;
    private LocalDateTime createdAt;
}
