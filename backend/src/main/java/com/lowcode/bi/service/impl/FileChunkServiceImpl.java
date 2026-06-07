package com.lowcode.bi.service.impl;

import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.dto.FileChunkUploadRequest;
import com.lowcode.bi.dto.FileChunkUploadResponse;
import com.lowcode.bi.service.FileChunkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
public class FileChunkServiceImpl implements FileChunkService {

    @Value("${app.upload.chunk-dir:${user.home}/lowcode-bi/chunks}")
    private String chunkDir;

    @Value("${app.upload.merge-dir:${user.home}/lowcode-bi/merged}")
    private String mergeDir;

    @Override
    public FileChunkUploadResponse uploadChunk(FileChunkUploadRequest request) {
        FileChunkUploadResponse response = new FileChunkUploadResponse();
        response.setFileId(request.getFileId());
        response.setChunkNumber(request.getChunkNumber());

        try {
            Path chunkStorageDir = Paths.get(chunkDir, request.getFileId());
            Files.createDirectories(chunkStorageDir);

            Path chunkPath = chunkStorageDir.resolve(request.getChunkNumber().toString());

            try (var inputStream = request.getFile().getInputStream()) {
                Files.copy(inputStream, chunkPath, StandardCopyOption.REPLACE_EXISTING);
            }

            response.setSuccess(true);
            response.setMessage("分片上传成功");

            boolean allChunksUploaded = checkAllChunksUploaded(
                    request.getFileId(),
                    request.getTotalChunks(),
                    request.getChunkSize()
            );
            response.setCompleted(allChunksUploaded);

            log.info("分片上传成功: fileId={}, chunkNumber={}, totalChunks={}",
                    request.getFileId(), request.getChunkNumber(), request.getTotalChunks());

        } catch (Exception e) {
            log.error("分片上传失败: fileId={}, chunkNumber={}",
                    request.getFileId(), request.getChunkNumber(), e);
            response.setSuccess(false);
            response.setMessage("分片上传失败: " + e.getMessage());
            response.setCompleted(false);
        }

        return response;
    }

    @Override
    public boolean checkChunkExists(String fileId, int chunkNumber) {
        Path chunkPath = Paths.get(chunkDir, fileId, String.valueOf(chunkNumber));
        return Files.exists(chunkPath);
    }

    @Override
    public File mergeChunks(String fileId, String fileName, int totalChunks, UUID tenantId) {
        try {
            Path chunkStorageDir = Paths.get(chunkDir, fileId);
            if (!Files.exists(chunkStorageDir)) {
                throw new BusinessException("分片目录不存在");
            }

            File[] chunkFiles = chunkStorageDir.toFile().listFiles();
            if (chunkFiles == null || chunkFiles.length != totalChunks) {
                throw new BusinessException("分片不完整，无法合并");
            }

            Arrays.sort(chunkFiles, Comparator.comparingInt(f -> Integer.parseInt(f.getName())));

            String sanitizedFileName = sanitizeFileName(fileName);
            Path mergeStorageDir = Paths.get(mergeDir, tenantId.toString());
            Files.createDirectories(mergeStorageDir);

            Path mergedFilePath = mergeStorageDir.resolve(sanitizedFileName);

            try (FileOutputStream fos = new FileOutputStream(mergedFilePath.toFile());
                 FileChannel mergedChannel = fos.getChannel()) {

                for (File chunkFile : chunkFiles) {
                    try (FileInputStream fis = new FileInputStream(chunkFile);
                         FileChannel chunkChannel = fis.getChannel()) {
                        chunkChannel.transferTo(0, chunkChannel.size(), mergedChannel);
                    }
                }
            }

            log.info("文件合并成功: fileId={}, fileName={}, path={}",
                    fileId, fileName, mergedFilePath);

            return mergedFilePath.toFile();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件合并失败: fileId={}, fileName={}", fileId, fileName, e);
            throw new BusinessException("文件合并失败: " + e.getMessage());
        }
    }

    @Override
    public void cleanupChunks(String fileId) {
        try {
            Path chunkStorageDir = Paths.get(chunkDir, fileId);
            if (Files.exists(chunkStorageDir)) {
                Files.walk(chunkStorageDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("分片清理成功: fileId={}", fileId);
            }
        } catch (IOException e) {
            log.warn("分片清理失败: fileId={}", fileId, e);
        }
    }

    private boolean checkAllChunksUploaded(String fileId, int totalChunks, long expectedChunkSize) {
        try {
            Path chunkStorageDir = Paths.get(chunkDir, fileId);
            if (!Files.exists(chunkStorageDir)) {
                return false;
            }

            File[] chunkFiles = chunkStorageDir.toFile().listFiles();
            if (chunkFiles == null || chunkFiles.length != totalChunks) {
                return false;
            }

            for (int i = 1; i <= totalChunks; i++) {
                Path chunkPath = chunkStorageDir.resolve(String.valueOf(i));
                if (!Files.exists(chunkPath)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("检查分片完整性失败: fileId={}", fileId, e);
            return false;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "upload.csv";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
