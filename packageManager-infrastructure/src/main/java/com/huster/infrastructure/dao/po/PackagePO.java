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
    /** 取件码(货架-流水号,如A-13-0001) */
    private String pickupCode;
    /** 入库操作人ID */
    private Long userId;
    /** 分页偏移量（仅查询用，非DB字段） */
    private Integer offset;
    /** 分页大小（仅查询用，非DB字段） */
    private Integer limit;
    /** 仅查询滞留包裹（仅查询用，非DB字段） */
    private Boolean stale;
    /** 排序方向 asc/desc（仅查询用，非DB字段） */
    private String sortOrder;
}
