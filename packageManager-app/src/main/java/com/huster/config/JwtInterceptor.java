package com.huster.config;

import com.alibaba.fastjson.JSON;
import com.huster.api.response.Response;
import com.huster.types.enums.ResponseCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String SECRET = "PackageManagerJwtSecretKey2026!@#";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            request.setAttribute("username", claims.getSubject());
            return true;
        } catch (ExpiredJwtException e) {
            sendUnauthorized(response, "Token expired");
            return false;
        } catch (Exception e) {
            sendUnauthorized(response, "Invalid token");
            return false;
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(
                Response.fail(ResponseCode.TOKEN_INVALID.getCode(), message)));
    }

    /** 生成 JWT Token */
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
}
