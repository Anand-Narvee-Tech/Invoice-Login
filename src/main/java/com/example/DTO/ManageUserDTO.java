package com.example.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManageUserDTO {
    private Long id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String roleName;
    private String addedBy;       // addedBy user ID as String
    private Long updatedBy;       // updater user ID
    private String addedByName;   // addedBy full name
    private String updatedByName; // updater full name
}
