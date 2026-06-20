package com.huster.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackagePO {
    private Long id;
    private String bizId;
    private String waybillNo;
    private String phone;
    private String courier;
    private String shelf;
    private Integer status;
    private Date checkinTime;
    private Date pickupTime;
    private Date createTime;
    private Date updateTime;
}
