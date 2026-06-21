package com.huster.infrastructure.dao;

import com.huster.infrastructure.dao.po.PackagePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IPackageDao {

    void insert(PackagePO po);

    PackagePO queryByBizId(String bizId);

    PackagePO queryByWaybillNoAndStatus(PackagePO req);

    List<PackagePO> queryPage(PackagePO req);

    int countPage(PackagePO req);

    int countTodayCheckin();

    int countPending();

    int countStale();

    int countTodayPickup();

    int updatePickup(PackagePO po);

    int updateInfo(PackagePO po);
}
