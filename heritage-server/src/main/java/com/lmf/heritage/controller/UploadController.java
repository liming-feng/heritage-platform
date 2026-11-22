package com.lmf.heritage.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图片上传（商品封面 / 课程封面 / 文章封面 / 轮播图 / 头像共用）
 *
 * <p><b>为什么有三个路径而只有一套实现：</b>上传本身与角色无关，但「谁能上传」有关。
 * 拦截器按前缀做角色隔离（{@code /admin/**} 仅管理员、{@code /craftsman/**} 仅匠人与管理员），
 * 所以这里保留三个入口，落到同一个 {@link #store(MultipartFile)}，
 * 前端按所在端调用对应路径即可，无需自己判断角色。</p>
 *
 * <p><b>安全约定：</b></p>
 * <ol>
 *   <li>文件名由服务端生成（UUID + 白名单扩展名），<b>不使用</b>客户端文件名，
 *       从根上杜绝 {@code ../../web.xml} 这类路径穿越；</li>
 *   <li>扩展名与 Content-Type 双重校验，且白名单来自配置 {@code heritage.upload.allowed-ext}
 *       （默认 jpg/jpeg/png/webp/gif）。<b>刻意不含 svg</b>：SVG 可内嵌脚本，
 *       当作用户上传内容直接回显会构成存储型 XSS；</li>
 *   <li>大小上限取配置 {@code heritage.upload.max-size}（5MB），与 Spring 的 multipart 限制一致。</li>
 * </ol>
 *
 * <p>落盘结构按日期分目录（{@code uploads/2026/10/27/xxx.png}），单目录文件数不会无限膨胀；
 * 对外地址由 {@code heritage.upload.access-prefix} 拼出，形如
 * {@code /api/uploads/2026/10/27/xxx.png}，与 {@code WebMvcConfig} 的静态资源映射对应。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "公共-文件上传", description = "图片上传，返回可直接存入 cover/image 字段的相对地址")
@RestController
public class UploadController {

    private static final DateTimeFormatter DAY_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 图片物理存储根目录，如 E:/.../uploads/ */
    @Value("${heritage.upload.path}")
    private String uploadPath;

    /** 对外访问前缀（含 context-path），如 /api/uploads/ */
    @Value("${heritage.upload.access-prefix:/api/uploads/}")
    private String accessPrefix;

    /** 允许的扩展名，逗号分隔 */
    @Value("${heritage.upload.allowed-ext:jpg,jpeg,png,webp,gif}")
    private String allowedExt;

    /** 单文件大小上限（字节） */
    @Value("${heritage.upload.max-size:5242880}")
    private long maxSize;

    @Operation(summary = "上传图片（用户端）", description = "登录用户均可调用，用于头像、评价晒图等")
    @PostMapping("/portal/upload/image")
    @LoginRequired
    public Result<Map<String, Object>> uploadForPortal(
            @Parameter(description = "图片文件，表单字段名固定为 file") @RequestPart("file") MultipartFile file) {
        return Result.success(store(file));
    }

    @Operation(summary = "上传图片（匠人端）", description = "商品封面、课程封面等")
    @PostMapping("/craftsman/upload/image")
    @LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
    public Result<Map<String, Object>> uploadForCraftsman(
            @Parameter(description = "图片文件，表单字段名固定为 file") @RequestPart("file") MultipartFile file) {
        return Result.success(store(file));
    }

    @Operation(summary = "上传图片（管理端）", description = "文章封面、轮播图、分类图等")
    @PostMapping("/admin/upload/image")
    @LoginRequired(roles = {Constants.ROLE_ADMIN})
    public Result<Map<String, Object>> uploadForAdmin(
            @Parameter(description = "图片文件，表单字段名固定为 file") @RequestPart("file") MultipartFile file) {
        return Result.success(store(file));
    }

    /**
     * 校验 + 落盘 + 生成访问地址
     *
     * @return url（存库用）/ name（原文件名）/ size（字节）
     */
    private Map<String, Object> store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "图片不能超过 " + (maxSize / 1024 / 1024) + "MB，当前 " + (file.getSize() / 1024) + "KB");
        }

        String original = file.getOriginalFilename();
        String ext = extensionOf(original);
        Set<String> allowed = allowedExtensions();
        if (ext.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件缺少扩展名，无法识别图片格式");
        }
        if (!allowed.contains(ext)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的图片格式：" + ext + "，仅支持 " + String.join(" / ", allowed));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该文件不是图片");
        }

        String day = LocalDate.now().format(DAY_DIR);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path root = Paths.get(uploadPath.replace('\\', '/'));
        Path dir = root.resolve(day);
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("图片上传失败 dir={}", dir, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败，请稍后重试");
        }

        String url = normalizePrefix(accessPrefix) + day + "/" + fileName;
        log.info("图片上传成功 user={} size={} url={}", com.lmf.heritage.security.UserContext.userIdOrNull(),
                file.getSize(), url);

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("url", url);
        vo.put("name", original);
        vo.put("size", file.getSize());
        return vo;
    }

    /** 取小写扩展名，无扩展名返回空串 */
    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private Set<String> allowedExtensions() {
        return Arrays.stream(allowedExt.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 保证前缀以 / 开头、以 / 结尾，避免拼出 /api/uploads2026/... 这种地址 */
    private String normalizePrefix(String prefix) {
        String p = (prefix == null || prefix.isBlank()) ? "/uploads/" : prefix.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        return p;
    }
}
