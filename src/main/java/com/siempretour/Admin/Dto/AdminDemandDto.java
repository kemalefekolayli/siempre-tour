package com.siempretour.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDemandDto {
    private Long id;
    private String name;
    private String category;
    private String destination;
    private long reservationRequests;
    private long informationRequests;
    private long totalRequests;
}
