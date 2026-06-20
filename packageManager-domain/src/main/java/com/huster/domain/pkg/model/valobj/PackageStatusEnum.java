package com.huster.domain.pkg.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum PackageStatusEnum {
    PENDING(0, "待取件"),
    PICKED_UP(1, "已取件"),
    ;

    private Integer code;
    private String info;

    public static PackageStatusEnum valueOf(Integer code) {
        for (PackageStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        throw new IllegalArgumentException("Unknown PackageStatus code: " + code);
    }
}
