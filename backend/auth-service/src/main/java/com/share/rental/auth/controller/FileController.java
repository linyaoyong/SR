package com.share.rental.auth.controller;

import com.share.rental.auth.dto.FileUploadResponse;
import com.share.rental.auth.service.UserService;
import com.share.rental.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final UserService userService;

    @Autowired
    public FileController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/avatars")
    public ApiResponse<FileUploadResponse> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userService.updateAvatar(userId, file));
    }
}
