package com.huster.domain.pkg.model.aggregate;

import com.huster.domain.pkg.model.entity.PackageEntity;
import com.huster.domain.pkg.model.valobj.CourierCompanyEnum;
import com.huster.domain.pkg.model.valobj.PackageStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageAggregate {
    private PackageEntity packageEntity;

    /** 静态工厂：创建新入库包裹 */
    public static PackageAggregate createForCheckin(
            String waybillNo, String phone, CourierCompanyEnum courier, String shelf) {
        PackageEntity entity = PackageEntity.builder()
                .id(UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .waybillNo(waybillNo)
                .recipientPhone(phone)
                .courierCompany(courier)
                .shelfLocation(shelf)
                .status(PackageStatusEnum.PENDING)
                .checkinTime(LocalDateTime.now())
                .build();
        PackageAggregate aggregate = new PackageAggregate();
        aggregate.setPackageEntity(entity);
        return aggregate;
    }
}
