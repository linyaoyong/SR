package com.share.rental.common.upload;

public record UploadedFile(String url, String filename, String contentType, long size) {
}
