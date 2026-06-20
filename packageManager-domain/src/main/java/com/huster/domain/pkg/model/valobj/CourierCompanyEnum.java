package com.huster.domain.pkg.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum CourierCompanyEnum {
    SF("SF", "顺丰"),
    YTO("YTO", "圆通"),
    ZTO("ZTO", "中通"),
    STO("STO", "申通"),
    YD("YD", "韵达"),
    JD("JD", "京东"),
    DB("DB", "德邦"),
    OTHER("OTHER", "其他"),
    ;

    private String code;
    private String info;

    public static CourierCompanyEnum valueOfCode(String code) {
        for (CourierCompanyEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        throw new IllegalArgumentException("Unknown CourierCompany code: " + code);
    }
}
