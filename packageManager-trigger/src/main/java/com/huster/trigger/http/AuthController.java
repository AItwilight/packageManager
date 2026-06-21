package com.huster.trigger.http;

import com.alibaba.fastjson.JSON;
import com.huster.api.dto.LoginRequestDTO;
import com.huster.api.response.Response;
import com.huster.config.JwtInterceptor;
import com.huster.types.enums.ResponseCode;
import com.huster.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/auth/")
public class AuthController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("login")
    public Response<Map<String, String>> login(@RequestBody LoginRequestDTO request) {
        try {
            log.info("登录请求: {}", JSON.toJSONString(request));

            // 1. 参数校验
            if (StringUtils.isBlank(request.getUsername())
                    || StringUtils.isBlank(request.getPassword())) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                        ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }

            // 2. 查询用户
            String sql = "SELECT id, username, password FROM sys_user WHERE username = ?";
            Map<String, Object> user;
            try {
                user = jdbcTemplate.queryForMap(sql, request.getUsername());
            } catch (Exception e) {
                return Response.fail(ResponseCode.AUTH_FAIL.getCode(),
                        ResponseCode.AUTH_FAIL.getInfo());
            }

            // 3. 验证密码
            String storedPassword = (String) user.get("password");
            if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
                return Response.fail(ResponseCode.AUTH_FAIL.getCode(),
                        ResponseCode.AUTH_FAIL.getInfo());
            }

            // 4. 生成 Token
            Long userId = ((Number) user.get("id")).longValue();
            String token = JwtInterceptor.generateToken(request.getUsername(), userId);
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            return Response.success(data);

        } catch (AppException e) {
            log.error("业务异常", e);
            return Response.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR.getCode(),
                    ResponseCode.UN_ERROR.getInfo());
        }
    }
}
