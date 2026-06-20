package com.huster.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    DUP_KEY("0003", "唯一索引冲突"),
    UPDATE_ZERO("0004", "更新影响行数为0"),

    // 认证错误
    AUTH_FAIL("E0101", "用户名或密码错误"),
    TOKEN_INVALID("E0102", "未登录或Token已过期"),

    // 包裹业务错误
    PACKAGE_NOT_FOUND("E0201", "包裹不存在"),
    WAYBILL_DUPLICATE("E0202", "该运单号已存在待取件包裹"),
    ALREADY_PICKED("E0203", "该包裹已被取走"),
    CANT_EDIT_PICKED("E0204", "已取件包裹不可编辑"),
    ;

    private String code;
    private String info;

}
