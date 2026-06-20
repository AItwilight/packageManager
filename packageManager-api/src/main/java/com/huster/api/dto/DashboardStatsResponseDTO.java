package com.huster.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponseDTO {
    private Integer todayCheckin;
    private Integer pendingTotal;
    private Integer staleTotal;
    private Integer todayPickup;
}
