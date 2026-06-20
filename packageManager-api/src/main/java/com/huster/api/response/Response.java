package com.huster.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 7000723935764546321L;

    private String code;
    private String info;
    private T data;

    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.setCode("0000");
        response.setInfo("成功");
        response.setData(data);
        return response;
    }

    public static <T> Response<T> fail(String code, String info) {
        Response<T> response = new Response<>();
        response.setCode(code);
        response.setInfo(info);
        return response;
    }

}
