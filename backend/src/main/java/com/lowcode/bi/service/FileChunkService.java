package com.lowcode.bi.service;

import com.lowcode.bi.dto.FileChunkUploadRequest;
import com.lowcode.bi.dto.FileChunkUploadResponse;

import java.io.File;
import java.util.UUID;

public interface FileChunkService {
    FileChunkUploadResponse uploadChunk(FileChunkUploadRequest request);

    boolean checkChunkExists(String fileId, int chunkNumber);

    File mergeChunks(String fileId, String fileName, int totalChunks, UUID tenantId);

    void cleanupChunks(String fileId);
}
