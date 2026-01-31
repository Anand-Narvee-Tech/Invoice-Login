package com.example.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManageUserDTO {

	private Long id;
	private String fullName;
	private String firstName;
	private String middleName;
	private String lastName;
	private String email;
	private String primaryEmail;
	private String mobileNumber;
	private String companyName;
	private String roleName;
	private String addedBy; // addedBy user ID (as String)
	private Long updatedBy; // updater user ID
	private String addedByName; // addedBy display name
	private String updatedByName; // updater display name
	
	
	 // 🔽 Newly added fields
	//Bhargav
    private String state;
    private String country;
    private String pincode;
    private String telephone;
    private String ein;
    private String gstin;
    private String website;
	private String address;


	
}
