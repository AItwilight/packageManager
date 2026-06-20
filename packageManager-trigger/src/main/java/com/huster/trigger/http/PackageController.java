package com.huster.trigger.http;

import com.alibaba.fastjson.JSON;
import com.huster.api.IPackageManageService;
import com.huster.api.dto.CheckinRequestDTO;
import com.huster.api.dto.DashboardStatsResponseDTO;
import com.huster.api.dto.EditRequestDTO;
import com.huster.api.dto.PackageResponseDTO;
import com.huster.api.response.Response;
import com.huster.domain.pkg.model.aggregate.PackageAggregate;
import com.huster.domain.pkg.model.entity.PackageEntity;
import com.huster.domain.pkg.model.valobj.CourierCompanyEnum;
import com.huster.domain.pkg.service.IPackageService;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/")
public class PackageController implements IPackageManageService {

    @Resource
    private IPackageService packageService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== 包裹入库 ==========

    @PostMapping("package/checkin")
    @Override
    public Response<Map<String, String>> checkin(@RequestBody CheckinRequestDTO request) {
        try {
            log.info("包裹入库: {}", JSON.toJSONString(request));

            // 参数校验
            if (StringUtils.isBlank(request.getWaybillNo())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "运单号不能为空");
            }
            if (StringUtils.isBlank(request.getPhone()) || request.getPhone().length() != 11) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "手机号格式错误");
            }
            if (StringUtils.isBlank(request.getCourier())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "快递公司不能为空");
            }
            if (StringUtils.isBlank(request.getShelf())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "货架位置不能为空");
            }

            // 校验快递公司编码
            CourierCompanyEnum courier;
            try {
                courier = CourierCompanyEnum.valueOfCode(request.getCourier());
            } catch (IllegalArgumentException e) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "无效的快递公司编码");
            }

            // 创建聚合
            PackageAggregate aggregate = PackageAggregate.createForCheckin(
                    request.getWaybillNo(), request.getPhone(), courier, request.getShelf());

            // 入库
            PackageEntity entity = packageService.checkin(aggregate);

            Map<String, String> data = new HashMap<>();
            data.put("id", entity.getId());
            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 包裹列表/搜索 ==========

    @GetMapping("package/list")
    @Override
    public Response<Map<String, Object>> list(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            log.info("查询包裹: phone={}, keyword={}, status={}, page={}, size={}",
                    phone, keyword, status, page, size);

            List<PackageEntity> entities = packageService.queryList(phone, keyword, status, page, size);
            int total = packageService.countTotal(phone, keyword, status);

            List<PackageResponseDTO> list = entities.stream().map(e -> {
                CourierCompanyEnum cc = e.getCourierCompany();
                return PackageResponseDTO.builder()
                        .id(e.getId())
                        .waybillNo(e.getWaybillNo())
                        .phone(e.getRecipientPhone())
                        .courier(cc != null ? cc.getCode() : "")
                        .courierDesc(cc != null ? cc.getInfo() : "")
                        .shelf(e.getShelfLocation())
                        .status(e.getStatus().getCode())
                        .statusDesc(e.getStatus().getInfo())
                        .checkinTime(e.getCheckinTime() != null ? e.getCheckinTime().format(DT_FMT) : null)
                        .pickupTime(e.getPickupTime() != null ? e.getPickupTime().format(DT_FMT) : null)
                        .stale(e.isStale())
                        .build();
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("list", list);
            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 确认取件 ==========

    @PutMapping("package/pickup/{id}")
    @Override
    public Response<Void> pickup(@PathVariable("id") String id) {
        try {
            log.info("确认取件: id={}", id);

            if (StringUtils.isBlank(id)) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "包裹ID不能为空");
            }

            packageService.pickup(id);
            return Response.success(null);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 编辑入库信息 ==========

    @PutMapping("package/edit")
    @Override
    public Response<Void> edit(@RequestBody EditRequestDTO request) {
        try {
            log.info("编辑包裹: {}", JSON.toJSONString(request));

            if (StringUtils.isBlank(request.getId())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "包裹ID不能为空");
            }

            CourierCompanyEnum courier;
            try {
                courier = CourierCompanyEnum.valueOfCode(request.getCourier());
            } catch (IllegalArgumentException e) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "无效的快递公司编码");
            }

            PackageEntity entity = PackageEntity.builder()
                    .id(request.getId())
                    .waybillNo(request.getWaybillNo())
                    .recipientPhone(request.getPhone())
                    .courierCompany(courier)
                    .shelfLocation(request.getShelf())
                    .build();

            packageService.edit(entity);
            return Response.success(null);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }

    // ========== 统计概览 ==========

    @GetMapping("dashboard/stats")
    @Override
    public Response<DashboardStatsResponseDTO> stats() {
        try {
            log.info("查询统计概览");

            int[] stats = packageService.getDashboardStats();
            DashboardStatsResponseDTO data = DashboardStatsResponseDTO.builder()
                    .todayCheckin(stats[0])
                    .pendingTotal(stats[1])
                    .staleTotal(stats[2])
                    .todayPickup(stats[3])
                    .build();

            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
        }
    }
}
