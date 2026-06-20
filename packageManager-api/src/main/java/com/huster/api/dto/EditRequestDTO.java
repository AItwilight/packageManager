package com.huster.api.dto;

import lombok.Data;

@Data
public class EditRequestDTO {
    private String id;
    private String waybillNo;
    private String phone;
    private String courier;
    private String shelf;
}
