package com.huster.infrastructure.adapter.repository;

import com.huster.domain.pkg.adapter.repository.IPackageRepository;
import com.huster.domain.pkg.model.aggregate.PackageAggregate;
import com.huster.domain.pkg.model.entity.PackageEntity;
import com.huster.domain.pkg.model.valobj.CourierCompanyEnum;
import com.huster.domain.pkg.model.valobj.PackageStatusEnum;
import com.huster.infrastructure.dao.IPackageDao;
import com.huster.infrastructure.dao.po.PackagePO;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PackageRepository implements IPackageRepository {

    @Resource
    private IPackageDao dao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public PackageEntity queryByBizId(String bizId) {
        PackagePO result = dao.queryByBizId(bizId);
        return toEntity(result);
    }

    @Override
    public PackageEntity queryByWaybillNoAndStatus(String waybillNo, Integer statusCode) {
        PackagePO req = PackagePO.builder()
                .waybillNo(waybillNo)
                .status(statusCode)
                .build();
        PackagePO result = dao.queryByWaybillNoAndStatus(req);
        return toEntity(result);
    }

    @Override
    public List<PackageEntity> queryPage(String phone, String keyword, Integer statusCode,
                                          int offset, int limit) {
        PackagePO req = PackagePO.builder()
                .phone(phone)
                .waybillNo(keyword)
                .status(statusCode)
                .offset(offset)
                .limit(limit)
                .build();
        List<PackagePO> list = dao.queryPage(req);
        return list.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public int countPage(String phone, String keyword, Integer statusCode) {
        PackagePO req = PackagePO.builder()
                .phone(phone)
                .waybillNo(keyword)
                .status(statusCode)
                .build();
        return dao.countPage(req);
    }

    @Override
    public int countTodayCheckin() {
        return dao.countTodayCheckin();
    }

    @Override
    public int countPending() {
        return dao.countPending();
    }

    @Override
    public int countStale() {
        return dao.countStale();
    }

    @Override
    public int countTodayPickup() {
        return dao.countTodayPickup();
    }

    @Override
    @Transactional(timeout = 500)
    public PackageEntity save(PackageAggregate aggregate) {
        PackageEntity entity = aggregate.getPackageEntity();

        // 原子生成当日流水号并组成取件码
        int serial = generateDailySerial();
        String pickupCode = entity.getShelfLocation() + "-" + String.format("%04d", serial);
        entity.setPickupCode(pickupCode);

        PackagePO po = toPO(entity);
        try {
            dao.insert(po);
        } catch (DuplicateKeyException e) {
            throw new AppException(ResponseCode.DUP_KEY.getCode(),
                    ResponseCode.DUP_KEY.getInfo());
        }
        entity.setId(po.getBizId());
        return entity;
    }

    @Override
    @Transactional(timeout = 500)
    public int updatePickup(String bizId) {
        return dao.updatePickup(bizId);
    }

    @Override
    @Transactional(timeout = 500)
    public int updateInfo(PackageEntity entity) {
        // 如果货架位置变更，取件码前缀同步更新（保留流水号后缀）
        PackagePO existing = dao.queryByBizId(entity.getId());
        if (existing != null && existing.getPickupCode() != null
                && !entity.getShelfLocation().equals(existing.getShelf())) {
            String serial = existing.getPickupCode().substring(
                    existing.getPickupCode().lastIndexOf('-') + 1);
            entity.setPickupCode(entity.getShelfLocation() + "-" + serial);
        } else if (existing != null) {
            entity.setPickupCode(existing.getPickupCode());
        }

        PackagePO po = toPO(entity);
        return dao.updateInfo(po);
    }

    // ========== 每日序列号（原子操作） ==========

    private int generateDailySerial() {
        jdbcTemplate.update(
                "INSERT INTO daily_sequence (date_key, seq_no) VALUES (CURDATE(), 1) " +
                        "ON DUPLICATE KEY UPDATE seq_no = seq_no + 1");
        Integer seq = jdbcTemplate.queryForObject(
                "SELECT seq_no FROM daily_sequence WHERE date_key = CURDATE()", Integer.class);
        if (seq == null || seq > 9999) {
            throw new AppException(ResponseCode.SERIAL_OVERFLOW.getCode(),
                    ResponseCode.SERIAL_OVERFLOW.getInfo());
        }
        return seq;
    }

    // ========== PO ↔ Entity 转换 ==========

    private PackagePO toPO(PackageEntity entity) {
        return PackagePO.builder()
                .bizId(entity.getId())
                .waybillNo(entity.getWaybillNo())
                .phone(entity.getRecipientPhone())
                .courier(entity.getCourierCompany().getCode())
                .shelf(entity.getShelfLocation())
                .status(entity.getStatus().getCode())
                .checkinTime(toDate(entity.getCheckinTime()))
                .pickupCode(entity.getPickupCode())
                .userId(entity.getUserId())
                .build();
    }

    private PackageEntity toEntity(PackagePO po) {
        if (po == null) return null;
        return PackageEntity.builder()
                .id(po.getBizId())
                .waybillNo(po.getWaybillNo())
                .recipientPhone(po.getPhone())
                .courierCompany(CourierCompanyEnum.valueOfCode(po.getCourier()))
                .shelfLocation(po.getShelf())
                .status(PackageStatusEnum.valueOf(po.getStatus()))
                .checkinTime(toLocalDateTime(po.getCheckinTime()))
                .pickupTime(toLocalDateTime(po.getPickupTime()))
                .pickupCode(po.getPickupCode())
                .userId(po.getUserId())
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
