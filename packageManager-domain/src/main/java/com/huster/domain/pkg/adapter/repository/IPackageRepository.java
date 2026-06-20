package com.huster.domain.pkg.adapter.repository;

import com.huster.domain.pkg.model.aggregate.PackageAggregate;
import com.huster.domain.pkg.model.entity.PackageEntity;

import java.util.List;

public interface IPackageRepository {

    /** 按业务ID查询 */
    PackageEntity queryByBizId(String bizId);

    /** 按运单号 + 状态查询（用于查重） */
    PackageEntity queryByWaybillNoAndStatus(String waybillNo, Integer statusCode);

    /** 分页查询 */
    List<PackageEntity> queryPage(String phone, String keyword, Integer statusCode, int offset, int limit);

    /** 统计总数 */
    int countPage(String phone, String keyword, Integer statusCode);

    /** 统计概览：今日入库数 */
    int countTodayCheckin();

    /** 统计概览：待取件总数 */
    int countPending();

    /** 统计概览：滞留数（>48h） */
    int countStale();

    /** 统计概览：今日取件数 */
    int countTodayPickup();

    /** 新增包裹 */
    PackageEntity save(PackageAggregate aggregate);

    /** 确认取件 */
    int updatePickup(String bizId);

    /** 编辑入库信息 */
    int updateInfo(PackageEntity entity);
}
