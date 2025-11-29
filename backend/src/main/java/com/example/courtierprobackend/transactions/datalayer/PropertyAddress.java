package com.example.courtierprobackend.transactions.datalayer;

import lombok.*;

import jakarta.persistence.Embeddable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyAddress {
    private String street;
    private String city;
    private String province;
    private String postalCode;
}


