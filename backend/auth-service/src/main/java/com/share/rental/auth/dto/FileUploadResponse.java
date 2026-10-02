package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponse {
    private String url;
    private String filename;
    private String contentType;
    private long size;
}
