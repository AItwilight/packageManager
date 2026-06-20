package com.huster.api.dto;

import lombok.Data;

@Data
public class CheckinRequestDTO {
    private String waybillNo;
    private String phone;
    private String courier;
    private String shelf;
}
