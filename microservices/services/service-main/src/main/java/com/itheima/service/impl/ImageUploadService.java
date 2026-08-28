package com.itheima.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

@Slf4j
@Component
public class ImageUploadService {

    @Value("${image.upload.url}")
    private String uploadUrl;

    @Value("${image.upload.token}")
    private String token;

    @Value("${image.upload.permission}")
    private Integer permission;

    @Value("${image.upload.strategy-id:#{null}}")
    private Integer strategyId;

    @Value("${image.upload.album-id:#{0}}")
    private Integer albumId;

    @Value("${image.upload.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${image.upload.read-timeout:30000}")
    private int readTimeout;

    @Value("${image.upload.temp-token:#{null}}")
    private String tempUploadToken;

    @Value("${image.upload.temp-dir:#{systemProperties['java.io.tmpdir']}}")
    private String tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImageUploadResponse upload(MultipartFile file) throws IOException {
        log.info("开始上传文件：文件名={}，大小={}字节，ContentType={}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType());

        // 1. 基础校验
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 2. 读取文件魔数（读取前12字节）
        byte[] fileHeader = new byte[12];
        int readBytes;
        try (InputStream inputStream = file.getInputStream()) {
            readBytes = inputStream.read(fileHeader);
            inputStream.reset();
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取文件内容：" + e.getMessage());
        }

        if (readBytes < 3) {
            throw new IllegalArgumentException("文件内容过短，无法检测类型");
        }

        // 3. 魔数检测文件实际类型
        String actualFileType = detectFileType(fileHeader, readBytes);
        String originalExt = FileUtil.extName(originalFilename).toLowerCase();

        // 双重验证：魔数未知时用文件名扩展名兜底
        if ("unknown".equals(actualFileType) && !originalExt.isEmpty()) {
            if ("jpg".equals(originalExt) || "jpeg".equals(originalExt)) {
                actualFileType = "jpeg";
            } else if (isValidImageExtension(originalExt)) {
                actualFileType = originalExt;
            }
        }

        log.info("文件原始扩展名：{}，实际文件类型：{}", originalExt, actualFileType);

        // 4. 验证支持的文件类型
        if (!isValidImageFileType(actualFileType)) {
            throw new IllegalArgumentException("不支持的文件格式：" + actualFileType +
                    "，支持：jpeg, png, gif, webp, bmp");
        }

        // 5. 确定最终的扩展名和Content-Type（根据实际文件类型）
        String finalExt = switch (actualFileType) {
            case "jpeg" -> "jpg";
            case "png", "gif", "webp", "bmp" -> actualFileType;
            default -> originalExt;
        };
        // 若原始文件名扩展名与实际类型不一致，替换为实际有效扩展名（保证文件能正常打开）
        String finalFileName = FileUtil.mainName(originalFilename) + "." + finalExt;
        String finalContentType = switch (finalExt) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        };

        log.info("最终上传文件名：{}，Content-Type={}", finalFileName, finalContentType);

        // 6. 创建临时文件（指定目录 + 仅保留原始文件名，修正扩展名）
        File tempFile = null;
        try {
            // 确保指定的临时目录存在，不存在则创建
            File tempDirectory = new File(tempDir);
            if (!tempDirectory.exists()) {
                boolean mkdirsSuccess = tempDirectory.mkdirs();
                log.info("临时目录{}不存在，创建结果：{}", tempDir, mkdirsSuccess);
                if (!mkdirsSuccess) {
                    throw new IOException("无法创建临时目录：" + tempDirectory.getAbsolutePath());
                }
            }

            // 核心修改：仅使用【原始文件名（修正扩展名）】作为临时文件名，无额外拼接
            tempFile = Paths.get(tempDir, finalFileName).toFile();
            // 写入文件内容
            file.transferTo(tempFile);

            log.info("临时文件创建成功：{}，大小={}字节",
                    tempFile.getAbsolutePath(), tempFile.length());

            // 7. 构建上传请求
            String cleanToken = token.trim();
            if (cleanToken.startsWith("Bearer ")) {
                cleanToken = cleanToken.substring("Bearer ".length());
            }

            log.info("使用Token：{}...", cleanToken.length() > 10 ?
                    cleanToken.substring(0, 10) + "..." : cleanToken);

            HttpRequest httpRequest = HttpRequest.post(uploadUrl)
                    .header("Authorization", "Bearer " + cleanToken)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    // 上传时使用【原始文件名】，保持与用户上传的名称一致
                    .form("file", tempFile, originalFilename, finalContentType)
                    .timeout(Math.max(connectTimeout, readTimeout));

            // 添加有效请求参数
            if (permission != null) {
                httpRequest.form("permission", permission.toString());
            }
            if (strategyId != null && strategyId > 0) {
                httpRequest.form("strategy_id", strategyId.toString());
                log.warn("注意：使用了strategy_id={}，请确保该ID有效", strategyId);
            }
            httpRequest.form("album_id", albumId.toString());

            // 打印请求信息
            log.info("请求URL：{}", uploadUrl);
            log.info("发送参数：file={} ({}字节), permission={}, strategy_id={}, album_id={}",
                    finalFileName, tempFile.length(), permission, strategyId, albumId);

            // 8. 执行上传请求
            try (HttpResponse response = httpRequest.execute()) {
                int statusCode = response.getStatus();
                String responseBody = response.body();

                log.info("响应状态码：{}，响应体：{}", statusCode, responseBody);

                if (statusCode != 200) {
                    throw new RuntimeException("图床接口异常，状态码：" + statusCode + "，响应：" + responseBody);
                }

                // 9. 解析响应结果
                JsonNode result = objectMapper.readTree(responseBody);
                boolean success = result.has("status") ? result.get("status").asBoolean(false) :
                        (result.has("code") && result.get("code").asInt() == 200);

                if (!success) {
                    String errorMessage = result.has("message") ? result.get("message").asText() :
                            result.has("msg") ? result.get("msg").asText() : "未知错误";
                    // 特定错误精准提示
                    if (errorMessage.contains("策略不存在")) {
                        throw new RuntimeException("上传失败：策略ID不存在，当前配置：" + strategyId);
                    } else if (errorMessage.contains("文件类型")) {
                        throw new RuntimeException("上传失败：文件类型不支持，实际类型：" + actualFileType + "，错误：" + errorMessage);
                    } else if (errorMessage.contains("token") || errorMessage.contains("权限")) {
                        throw new RuntimeException("上传失败：Token无效/权限不足，错误：" + errorMessage);
                    } else {
                        throw new RuntimeException("上传失败：" + errorMessage);
                    }
                }

                // 10. 封装返回结果
                ImageUploadResponse uploadResponse = new ImageUploadResponse();
                JsonNode data = result.has("data") ? result.get("data") : result;

                if (data.has("url")) {
                    uploadResponse.setUrl(data.get("url").asText());
                } else if (data.has("links") && data.get("links").has("url")) {
                    uploadResponse.setUrl(data.get("links").get("url").asText());
                } else {
                    throw new RuntimeException("响应格式异常：未找到图片URL，响应体：" + responseBody);
                }

                if (data.has("delete_url")) {
                    uploadResponse.setDeleteUrl(data.get("delete_url").asText());
                } else if (data.has("links") && data.get("links").has("delete_url")) {
                    uploadResponse.setDeleteUrl(data.get("links").get("delete_url").asText());
                }

                if (data.has("key")) {
                    uploadResponse.setKey(data.get("key").asText());
                }
                uploadResponse.setOriginName(originalFilename); // 返回原始文件名

                log.info("上传成功！图片URL：{}，原始文件名：{}", uploadResponse.getUrl(), originalFilename);
                return uploadResponse;
            }

        } catch (Exception e) {
            log.error("上传过程异常", e);
            if (e instanceof RuntimeException) {
                throw e;
            } else {
                throw new RuntimeException("图片上传失败：" + e.getMessage(), e);
            }
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                try {
                    boolean deleted = tempFile.delete();
                    log.info("临时文件{}清理{}", tempFile.getName(), deleted ? "成功" : "失败");
                } catch (Exception e) {
                    log.warn("清理临时文件失败：{}", e.getMessage());
                }
            }
        }
    }

    /**
     * 精准的文件魔数检测
     */
    private String detectFileType(byte[] header, int readBytes) {
        // JPEG: FF D8 FF (前3字节)
        if (readBytes >= 3 &&
                header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF) {
            return "jpeg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A (前8字节)
        if (readBytes >= 8 &&
                header[0] == (byte)0x89 && header[1] == (byte)0x50 && header[2] == (byte)0x4E &&
                header[3] == (byte)0x47 && header[4] == (byte)0x0D && header[5] == (byte)0x0A &&
                header[6] == (byte)0x1A && header[7] == (byte)0x0A) {
            return "png";
        }
        // GIF: GIF87a or GIF89a (前6字节)
        if (readBytes >= 6 &&
                header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8' &&
                (header[4] == '7' || header[4] == '9') && header[5] == 'a') {
            return "gif";
        }
        // WEBP: RIFF .... WEBP (前12字节)
        if (readBytes >= 12 &&
                header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        // BMP: BM (前2字节)
        if (readBytes >= 2 && header[0] == 'B' && header[1] == 'M') {
            return "bmp";
        }
        return "unknown";
    }

    /**
     * 验证是否为支持的图片文件类型
     */
    private boolean isValidImageFileType(String fileType) {
        return fileType.equals("jpeg") || fileType.equals("png") ||
                fileType.equals("gif") || fileType.equals("webp") ||
                fileType.equals("bmp");
    }

    /**
     * 验证图片文件扩展名
     */
    private boolean isValidImageExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return extension.toLowerCase().matches("^(jpg|jpeg|png|gif|webp|bmp)$");
    }

    @Data
    public static class ImageUploadResponse {
        private String url;
        private String deleteUrl;
        private String key;
        private String originName;
    }
}