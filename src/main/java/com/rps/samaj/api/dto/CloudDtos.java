package com.rps.samaj.api.dto;

public final class CloudDtos {

    private CloudDtos() {
    }

    public record CloudUploadResponse(String url, String provider) {
    }
}
