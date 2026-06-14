package com.siempretour.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminImageUploadResponseDto {
    private List<String> imageUrls;
    private List<String> warnings;
}
