package com.siempretour.User.Dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupabaseUserInfo {
    @NotEmpty
    private String userId;          // sub claim
    @NotEmpty
    private String email;           // email claim
    @NotEmpty
    private String role;            // role claim
    @NotEmpty
    private Map<String, Object> userMetadata;  // user_metadata claim
    @NotEmpty
    private Map<String, Object> appMetadata;   // app_metadata claim

}