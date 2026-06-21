package com.huster.domain.pkg.service;

import com.huster.domain.pkg.model.aggregate.PackageAggregate;
import com.huster.domain.pkg.model.entity.PackageEntity;

import java.util.List;

public interface IPackageService {
    PackageEntity checkin(PackageAggregate aggregate);
    List<PackageEntity> queryList(String phone, String keyword, Integer statusCode,
                                   Boolean stale, String sortOrder, int page, int size);
    int countTotal(String phone, String keyword, Integer statusCode, Boolean stale);
    void pickup(String bizId);
    void edit(PackageEntity entity);
    int[] getDashboardStats();
}
