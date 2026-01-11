package com.example.courtierprobackend.transactions.datalayer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAddressDTO {
    private String street;
    private String city;
    private String province;
    private String postalCode;
}
