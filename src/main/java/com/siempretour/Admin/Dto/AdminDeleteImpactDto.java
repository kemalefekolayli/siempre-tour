package com.siempretour.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDeleteImpactDto {
    private Long tourId;
    private long bookingReferences;
    private boolean canPermanentlyDelete;
    private String message;
}
