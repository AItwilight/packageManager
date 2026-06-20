package com.huster.domain.pkg.service;

import com.huster.domain.pkg.adapter.repository.IPackageRepository;
import com.huster.domain.pkg.model.aggregate.PackageAggregate;
import com.huster.domain.pkg.model.entity.PackageEntity;
import com.huster.domain.pkg.model.valobj.PackageStatusEnum;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
public class PackageService implements IPackageService {

    @Resource
    private IPackageRepository repository;

    @Override
    public PackageEntity checkin(PackageAggregate aggregate) {
        // 校验：同运单号不允许重复待取件
        PackageEntity existing = repository.queryByWaybillNoAndStatus(
                aggregate.getPackageEntity().getWaybillNo(),
                PackageStatusEnum.PENDING.getCode());
        if (existing != null) {
            throw new AppException(ResponseCode.WAYBILL_DUPLICATE.getCode(),
                    ResponseCode.WAYBILL_DUPLICATE.getInfo());
        }
        return repository.save(aggregate);
    }

    @Override
    public List<PackageEntity> queryList(String phone, String keyword, Integer statusCode, int page, int size) {
        int offset = (page - 1) * size;
        return repository.queryPage(phone, keyword, statusCode, offset, size);
    }

    @Override
    public int countTotal(String phone, String keyword, Integer statusCode) {
        return repository.countPage(phone, keyword, statusCode);
    }

    @Override
    public void pickup(String bizId) {
        PackageEntity entity = repository.queryByBizId(bizId);
        if (entity == null) {
            throw new AppException(ResponseCode.PACKAGE_NOT_FOUND.getCode(),
                    ResponseCode.PACKAGE_NOT_FOUND.getInfo());
        }
        if (entity.getStatus() == PackageStatusEnum.PICKED_UP) {
            throw new AppException(ResponseCode.ALREADY_PICKED.getCode(),
                    ResponseCode.ALREADY_PICKED.getInfo());
        }
        int rows = repository.updatePickup(bizId);
        if (rows == 0) {
            throw new AppException(ResponseCode.UPDATE_ZERO.getCode(),
                    ResponseCode.UPDATE_ZERO.getInfo());
        }
    }

    @Override
    public void edit(PackageEntity entity) {
        PackageEntity existing = repository.queryByBizId(entity.getId());
        if (existing == null) {
            throw new AppException(ResponseCode.PACKAGE_NOT_FOUND.getCode(),
                    ResponseCode.PACKAGE_NOT_FOUND.getInfo());
        }
        if (existing.getStatus() == PackageStatusEnum.PICKED_UP) {
            throw new AppException(ResponseCode.CANT_EDIT_PICKED.getCode(),
                    ResponseCode.CANT_EDIT_PICKED.getInfo());
        }
        repository.updateInfo(entity);
    }

    @Override
    public int[] getDashboardStats() {
        return new int[]{
                repository.countTodayCheckin(),
                repository.countPending(),
                repository.countStale(),
                repository.countTodayPickup()
        };
    }
}
