package com.huster.domain.pkg.model.entity;

import com.huster.domain.pkg.model.valobj.CourierCompanyEnum;
import com.huster.domain.pkg.model.valobj.PackageStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageEntity {
    /** 业务主键 */
    private String id;
    /** 运单号 */
    private String waybillNo;
    /** 收件人手机号 */
    private String recipientPhone;
    /** 快递公司 */
    private CourierCompanyEnum courierCompany;
    /** 货架位置 */
    private String shelfLocation;
    /** 包裹状态 */
    private PackageStatusEnum status;
    /** 入库时间 */
    private LocalDateTime checkinTime;
    /** 取件时间 */
    private LocalDateTime pickupTime;
    /** 取件码(货架-流水号,如A-13-0001) */
    private String pickupCode;
    /** 入库操作人ID */
    private Long userId;

    /** 是否滞留（入库超48小时且未取件） */
    public boolean isStale() {
        return this.status == PackageStatusEnum.PENDING
                && java.time.temporal.ChronoUnit.HOURS.between(this.checkinTime, LocalDateTime.now()) >= 48;
    }
}
