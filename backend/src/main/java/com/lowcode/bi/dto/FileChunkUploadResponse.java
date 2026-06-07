package com.lowcode.bi.dto;

import lombok.Data;

@Data
public class FileChunkUploadResponse {
    private String fileId;
    private Integer chunkNumber;
    private Boolean success;
    private Boolean completed;
    private String message;
    private String uploadedFilePath;
}
