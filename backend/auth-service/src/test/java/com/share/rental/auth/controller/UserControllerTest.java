package com.share.rental.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.auth.dto.BlacklistRequest;
import com.share.rental.auth.dto.BlacklistResponse;
import com.share.rental.auth.dto.UserMeResponse;
import com.share.rental.auth.dto.UserPublicResponse;
import com.share.rental.auth.service.BlacklistService;
import com.share.rental.auth.service.UserService;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class UserControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.context.WebApplicationContext gatewayFixtureContext;

    @org.junit.jupiter.api.BeforeEach
    void useExplicitGatewayCredentialFixture() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(gatewayFixtureContext)
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("X-Internal-Token", "test-only-backend-ingress-token"))
                .build();
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private BlacklistService blacklistService;

    @Test
    void me_returnsProfile() throws Exception {
        when(userService.getMe(eq(10L))).thenReturn(new UserMeResponse(
                10L, "alice", null, null, 100, 0, 0, 0, 0, 0, 1, null));

        mvc.perform(get("/api/users/me").header("X-User-Id", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void publicProfile_returnsProfile() throws Exception {
        when(userService.getPublic(eq(1L))).thenReturn(new UserPublicResponse(
                1L, "bob", null, null, 90, 0, 1));

        mvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("bob"));
    }

    @Test
    void blacklist_success() throws Exception {
        BlacklistRequest req = new BlacklistRequest();
        req.setReason("spam");
        doNothing().when(blacklistService).blacklist(eq(10L), eq(2L), eq("spam"));

        mvc.perform(post("/api/users/blacklist/2")
                        .header("X-User-Id", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listBlacklist_returnsList() throws Exception {
        when(blacklistService.listBlacklist(eq(10L))).thenReturn(Collections.singletonList(
                new BlacklistResponse(1L, 2L, "bob", "spam", LocalDateTime.now())));

        mvc.perform(get("/api/users/blacklist").header("X-User-Id", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].targetUsername").value("bob"));
    }
}
