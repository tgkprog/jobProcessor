package com.sel2in.jobProc;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end API tests for Job Processor Engine.
 * 
 * Uses a real H2 in-memory database (create-drop per run).
 * Starts a real embedded server on a random port.
 * Tests are ordered to simulate a real user workflow.
 * 
 * Jobs have a minimum 30s delay before execution, so during
 * these tests all scheduled jobs remain in SCHEDULED status.
 *
 * Run with: cd app && mvn test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JobApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ============================
    // Processor API Tests
    // ============================

    @Test
    @Order(1)
    @DisplayName("GET /api/job/listAll - empty on fresh DB")
    void listProcessors_empty() {
        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/listAll"), List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty(), "Processor list should be empty on fresh DB");
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/job/add - register a processor")
    void addProcessor() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"className\":\"com.sel2in.jobProc.processors.DateFileTxn\",\"jarPath\":\"./processors/dateFileTxn.jar\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.postForEntity(url("/api/job/add"), request, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("saved successfully"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/job/listAll - should have 1 processor")
    void listProcessors_hasOne() {
        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/listAll"), List.class);
        assertEquals(1, resp.getBody().size());

        Map proc = (Map) resp.getBody().get(0);
        assertEquals("com.sel2in.jobProc.processors.DateFileTxn", proc.get("className"));
        assertEquals("./processors/dateFileTxn.jar", proc.get("jarPath"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/job/add - register second processor")
    void addSecondProcessor() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"className\":\"com.sel2in.jobProc.processors.CsvExport\",\"jarPath\":\"./processors/csvExport.jar\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.postForEntity(url("/api/job/add"), request, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/job/listAll - should have 2 processors")
    void listProcessors_hasTwo() {
        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/listAll"), List.class);
        assertEquals(2, resp.getBody().size());
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/job/remove - remove second processor")
    void removeProcessor() {
        rest.delete(url("/api/job/remove/com.sel2in.jobProc.processors.CsvExport"));

        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/listAll"), List.class);
        assertEquals(1, resp.getBody().size());
        Map proc = (Map) resp.getBody().get(0);
        assertEquals("com.sel2in.jobProc.processors.DateFileTxn", proc.get("className"));
    }

    // ============================
    // Job Scheduling API Tests
    // ============================

    @Test
    @Order(10)
    @DisplayName("GET /api/job/status - empty initially")
    void jobStatus_empty() {
        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/status"), List.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/job/schedule - schedule a job (stays SCHEDULED due to 30s min delay)")
    void scheduleJob() {
        String scheduleUrl = url("/api/job/schedule?jobName=AuditQ1&processorClassName=com.sel2in.jobProc.processors.DateFileTxn&comment=Test+job");
        ResponseEntity<Map> resp = rest.postForEntity(scheduleUrl, null, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map body = resp.getBody();
        assertNotNull(body.get("id"));
        assertEquals("AuditQ1", body.get("jobName"));
        assertEquals("SCHEDULED", body.get("status"), "Job should be SCHEDULED (30s min delay)");
        assertEquals("com.sel2in.jobProc.processors.DateFileTxn", body.get("processorClassName"));
        assertNotNull(body.get("jobSubmittedDateTime"));
        assertNotNull(body.get("scheduledRunTime"), "scheduledRunTime should be set");
    }

    @Test
    @Order(12)
    @DisplayName("POST /api/job/schedule - schedule second job")
    void scheduleSecondJob() {
        String scheduleUrl = url("/api/job/schedule?jobName=AuditQ2&processorClassName=com.sel2in.jobProc.processors.DateFileTxn&comment=Another");
        ResponseEntity<Map> resp = rest.postForEntity(scheduleUrl, null, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("AuditQ2", resp.getBody().get("jobName"));
        assertEquals("SCHEDULED", resp.getBody().get("status"));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/job/status - should have 2 jobs")
    void jobStatus_hasTwo() {
        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/status"), List.class);
        assertEquals(2, resp.getBody().size());
    }

    @Test
    @Order(14)
    @DisplayName("Verify job fields persisted correctly — all SCHEDULED (not yet triggered)")
    void jobFields() {
        ResponseEntity<List> resp = rest.getForEntity(url("/api/job/status"), List.class);
        Map job1 = (Map) resp.getBody().get(0);

        assertEquals("AuditQ1", job1.get("jobName"));
        assertEquals("Test job", job1.get("comment"));
        assertEquals("SCHEDULED", job1.get("status"), "Should still be SCHEDULED (30s min delay)");
        assertNotNull(job1.get("id"));
        assertNotNull(job1.get("createdTs"));
        assertNotNull(job1.get("scheduledRunTime"));
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/job/run - manual trigger runs job immediately")
    void manualRun() {
        ResponseEntity<String> resp = rest.getForEntity(
                url("/api/job/run?jobId=1"), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("rescheduled"));
    }

    // ============================
    // Static Resource Tests
    // ============================

    @Test
    @Order(20)
    @DisplayName("GET /jobs.html - admin UI served")
    void jobsHtml() {
        ResponseEntity<String> resp = rest.getForEntity(url("/jobs.html"), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("Job Tracking"));
    }

    @Test
    @Order(21)
    @DisplayName("GET /jobsProcs.html - processor admin UI served")
    void jobsProcsHtml() {
        ResponseEntity<String> resp = rest.getForEntity(url("/jobsProcs.html"), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("Processor Control Panel"));
    }

    @Test
    @Order(22)
    @DisplayName("GET /h2 - H2 console available")
    void h2Console() {
        ResponseEntity<String> resp = rest.getForEntity(url("/h2"), String.class);
        assertNotNull(resp);
    }
}
