package com.huster.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huster.api.dto.*;
import com.huster.api.response.Response;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApiTest {

    @LocalServerPort
    private int port;

    @Resource
    private TestRestTemplate restTemplate;

    private String token;
    private String testPackageId;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ==================== T1: 正常登录 ====================

    @Test
    public void test01_login_success() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("admin");
        req.setPassword("admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        assertNotNull(json.getJSONObject("data").getString("token"));
        token = json.getJSONObject("data").getString("token");
    }

    // ==================== T2: 登录失败 ====================

    @Test
    public void test02_login_fail() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("admin");
        req.setPassword("wrong");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("E0101", json.getString("code"));
    }

    // ==================== T3: 正常入库 ====================

    @Test
    public void test03_checkin_success() {
        CheckinRequestDTO req = new CheckinRequestDTO();
        req.setWaybillNo("SF20260620001");
        req.setPhone("13800138000");
        req.setCourier("SF");
        req.setShelf("A-01");

        HttpHeaders headers = authHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        assertNotNull(json.getJSONObject("data").getString("id"));
        testPackageId = json.getJSONObject("data").getString("id");
    }

    // ==================== T4: 重复运单号入库 ====================

    @Test
    public void test04_checkin_duplicate() {
        CheckinRequestDTO req = new CheckinRequestDTO();
        req.setWaybillNo("SF20260620001");
        req.setPhone("13800138000");
        req.setCourier("SF");
        req.setShelf("A-02");

        HttpHeaders headers = authHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("E0202", json.getString("code"));
    }

    // ==================== T5: 入库参数缺失 ====================

    @Test
    public void test05_checkin_missing_param() {
        CheckinRequestDTO req = new CheckinRequestDTO();
        req.setWaybillNo("");
        req.setPhone("13800138000");
        req.setCourier("SF");
        req.setShelf("A-01");

        HttpHeaders headers = authHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0002", json.getString("code"));
    }

    // ==================== T6: 按手机号查询 ====================

    @Test
    public void test06_query_by_phone() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/package/list?phone=13800138000&page=1&size=20"),
                HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        assertTrue(json.getJSONObject("data").getInteger("total") >= 1);
    }

    // ==================== T7: 确认取件 ====================

    @Test
    public void test07_pickup() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/package/pickup/" + testPackageId),
                HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
    }

    // ==================== T8: 重复取件 ====================

    @Test
    public void test08_pickup_duplicate() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/package/pickup/" + testPackageId),
                HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("E0203", json.getString("code"));
    }

    // ==================== T9: 统计概览 ====================

    @Test
    public void test09_dashboard_stats() {
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/dashboard/stats"),
                HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject json = JSON.parseObject(resp.getBody());
        assertEquals("0000", json.getString("code"));
        JSONObject data = json.getJSONObject("data");
        assertNotNull(data.getInteger("todayCheckin"));
        assertNotNull(data.getInteger("pendingTotal"));
        assertNotNull(data.getInteger("staleTotal"));
        assertNotNull(data.getInteger("todayPickup"));
    }

    // ==================== T10: 未登录访问 ====================

    @Test
    public void test10_unauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/package/checkin"), entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ==================== 辅助方法 ====================

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }
}
