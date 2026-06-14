package com.siempretour.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSummaryDto {
    private long totalReservationRequests;
    private long totalInformationRequests;
    private long thisMonthRequests;
    private AdminDemandDto mostRequestedTour;
    private AdminDemandDto mostPopularCategory;
    private boolean genderDataAvailable;
    private boolean ageDataAvailable;
}
