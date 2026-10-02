package com.share.rental.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.auth.dto.FileUploadResponse;
import com.share.rental.auth.dto.UpdatePasswordRequest;
import com.share.rental.auth.dto.UpdateUserProfileRequest;
import com.share.rental.auth.dto.UserMeResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, FileController.class})
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class UserControllerStage4Test {

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
    void updateProfile_returnsUpdatedProfile() throws Exception {
        when(userService.updateProfile(eq(10L), any(UpdateUserProfileRequest.class)))
                .thenReturn(new UserMeResponse(
                        10L, "alice2", null, "hi", 100, 0, 0, 0, 0, 0, 1, null));

        UpdateUserProfileRequest req = new UpdateUserProfileRequest("alice2", "hi", 1);

        mvc.perform(put("/api/users/me")
                        .header("X-User-Id", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("alice2"))
                .andExpect(jsonPath("$.data.description").value("hi"));
    }

    @Test
    void updatePassword_returnsSuccess() throws Exception {
        doNothing().when(userService).updatePassword(eq(10L), any(UpdatePasswordRequest.class));

        UpdatePasswordRequest req = new UpdatePasswordRequest("oldpass", "newpass");

        mvc.perform(put("/api/users/me/password")
                        .header("X-User-Id", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void uploadAvatar_returnsFileUrl() throws Exception {
        when(userService.updateAvatar(eq(10L), any()))
                .thenReturn(new FileUploadResponse("/files/avatars/abc.jpg", "abc.jpg", "image/jpeg", 1024));

        MockMultipartFile file = new MockMultipartFile(
                "file", "abc.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/files/avatars")
                        .file(file)
                        .header("X-User-Id", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").value("/files/avatars/abc.jpg"))
                .andExpect(jsonPath("$.data.filename").value("abc.jpg"));
    }
}
