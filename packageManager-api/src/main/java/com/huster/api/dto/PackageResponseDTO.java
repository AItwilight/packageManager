package com.huster.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageResponseDTO {
    private String id;
    private String waybillNo;
    private String phone;
    private String courier;
    private String courierDesc;
    private String shelf;
    private Integer status;
    private String statusDesc;
    private String checkinTime;
    private String pickupTime;
    private Boolean stale;
}
