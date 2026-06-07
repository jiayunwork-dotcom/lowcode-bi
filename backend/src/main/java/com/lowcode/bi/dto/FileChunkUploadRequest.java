package com.lowcode.bi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileChunkUploadRequest {
    @NotBlank(message = "文件唯一标识不能为空")
    private String fileId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "分片序号不能为空")
    private Integer chunkNumber;

    @NotNull(message = "总分片数不能为空")
    private Integer totalChunks;

    @NotNull(message = "分片大小不能为空")
    private Long chunkSize;

    @NotNull(message = "文件总大小不能为空")
    private Long totalSize;

    @NotNull(message = "文件内容不能为空")
    private MultipartFile file;
}
