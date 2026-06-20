package com.huster.api;

import com.huster.api.dto.CheckinRequestDTO;
import com.huster.api.dto.DashboardStatsResponseDTO;
import com.huster.api.dto.EditRequestDTO;
import com.huster.api.dto.PackageResponseDTO;
import com.huster.api.response.Response;

import java.util.Map;

public interface IPackageManageService {
    Response<Map<String, String>> checkin(CheckinRequestDTO request);
    Response<Map<String, Object>> list(String phone, String keyword, Integer status, Integer page, Integer size);
    Response<Void> pickup(String id);
    Response<Void> edit(EditRequestDTO request);
    Response<DashboardStatsResponseDTO> stats();
}
