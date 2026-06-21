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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApiTest {

    @LocalServerPort
    private int port;

    @Resource
    private TestRestTemplate restTemplate;

    @Resource
    private JdbcTemplate jdbcTemplate;

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

    // ==================== T11: 批量入库 + 滞留包裹验证 ====================

    @Test
    public void test11_bulk_checkin_with_stale() {
        // 确保已登录（兼容单独运行此用例）
        if (token == null) {
            test01_login_success();
        }

        final int TOTAL = 100;            // 总包裹数
        final int STALE_COUNT = 5;        // 其中滞留包裹数
        final String[] COURIERS = {"SF", "YTO", "ZTO", "STO", "YD", "JD", "DB", "OTHER"};

        List<String> bizIds = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 1. 批量随机入库
        for (int i = 0; i < TOTAL; i++) {
            CheckinRequestDTO req = new CheckinRequestDTO();
            String currentCourier = COURIERS[rnd.nextInt(COURIERS.length)];
            // 随机运单号（防重复）
            req.setWaybillNo(currentCourier + System.currentTimeMillis()
                    + String.format("%04d", rnd.nextInt(10000)));
            // 随机手机号
            req.setPhone("1" + String.format("%010d", rnd.nextLong(10000000000L)));
            // 随机快递公司
            req.setCourier(currentCourier);
            // 随机货架：A-01 ~ H-99
            char zone = (char) ('A' + rnd.nextInt(8));
            req.setShelf(String.format("%c-%02d", zone, rnd.nextInt(1, 100)));

            HttpHeaders headers = authHeaders();
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url("/api/v1/package/checkin"), entity, String.class);
            assertEquals(HttpStatus.OK, resp.getStatusCode());

            JSONObject json = JSON.parseObject(resp.getBody());
            assertEquals("0000", json.getString("code"));

            String bizId = json.getJSONObject("data").getString("id");
            String pickupCode = json.getJSONObject("data").getString("pickupCode");
            assertNotNull(bizId);
            assertNotNull("取件码不应为空", pickupCode);
            assertTrue("取件码格式错误: " + pickupCode,
                    pickupCode.matches("^[A-Z]-\\d{2}-\\d{4}$"));
            bizIds.add(bizId);
        }

        System.out.println("批量入库完成: 共 " + TOTAL + " 条，取件码格式已验证");

        // 2. 将前 STALE_COUNT 条改为滞留（checkin_time 设为 72 小时前）
        for (int i = 0; i < STALE_COUNT; i++) {
            jdbcTemplate.update(
                    "UPDATE package SET checkin_time = DATE_SUB(NOW(), INTERVAL 72 HOUR) "
                            + "WHERE biz_id = ?", bizIds.get(i));
        }

        // 3. 验证仪表盘统计 —— 滞留数 >= STALE_COUNT
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/v1/dashboard/stats"), HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        JSONObject statsJson = JSON.parseObject(resp.getBody());
        assertEquals("0000", statsJson.getString("code"));
        JSONObject stats = statsJson.getJSONObject("data");
        int staleTotal = stats.getInteger("staleTotal");
        assertTrue("滞留包裹数应 >= " + STALE_COUNT + "，实际: " + staleTotal,
                staleTotal >= STALE_COUNT);
        System.out.println("仪表盘统计: todayCheckin=" + stats.getInteger("todayCheckin")
                + ", pendingTotal=" + stats.getInteger("pendingTotal")
                + ", staleTotal=" + staleTotal
                + ", todayPickup=" + stats.getInteger("todayPickup"));

        // 4. 查询列表 — 按状态=待取件 + 手机号后四位
        // 取第一个滞留包裹的手机号后四位
        Map<String, Object> firstPkg = jdbcTemplate.queryForMap(
                "SELECT phone, pickup_code FROM package WHERE biz_id = ?", bizIds.get(0));
        String phoneSuffix = ((String) firstPkg.get("phone")).substring(7);
        String pickupCode = (String) firstPkg.get("pickup_code");

        // 4a. 手机号后四位查询
        ResponseEntity<String> r1 = restTemplate.exchange(
                url("/api/v1/package/list?phone=" + phoneSuffix + "&status=0&page=1&size=10"),
                HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
        assertEquals(HttpStatus.OK, r1.getStatusCode());
        JSONObject list1 = JSON.parseObject(r1.getBody());
        assertEquals("0000", list1.getString("code"));
        assertTrue("按手机号后四位应查至少1条",
                list1.getJSONObject("data").getInteger("total") >= 1);

        // 4b. 取件码关键字搜索
        ResponseEntity<String> r2 = restTemplate.exchange(
                url("/api/v1/package/list?keyword=" + pickupCode.substring(0, 4) + "&page=1&size=10"),
                HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
        assertEquals(HttpStatus.OK, r2.getStatusCode());
        JSONObject list2 = JSON.parseObject(r2.getBody());
        assertEquals("0000", list2.getString("code"));
        assertTrue("按取件码前缀应查至少1条",
                list2.getJSONObject("data").getInteger("total") >= 1);

        // 4c. 验证列表返回字段包含 pickupCode
        JSONObject firstItem = list1.getJSONObject("data").getJSONArray("list").getJSONObject(0);
        assertNotNull("列表项应包含 pickupCode", firstItem.getString("pickupCode"));
        assertNotNull("列表项应包含 stale 标记", firstItem.getBoolean("stale"));

        System.out.println("滞留包裹验证通过: 共制造 " + STALE_COUNT + " 条滞留记录");
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
