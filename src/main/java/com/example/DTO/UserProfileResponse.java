package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserProfileResponse {

	private Long id;
	private String fullName;
	private String primaryEmail;
	private String mobileNumber;
	private String alternativeEmail;
	private String alternativeMobileNumber;
	private String companyName;
	private String taxId;
	private String businessId;
	private String preferredCurrency;
	private String invoicePrefix;
	private String profilePicPath;
	private String role;

}
