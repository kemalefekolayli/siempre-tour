package com.siempretour.Admin;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.siempretour.Admin.Dto.AdminVideoUploadResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Stores the optional tour video uploaded from the admin panel (shown first in the
// tour detail gallery). Same Cloudinary-or-local-disk approach as AdminPdfStorageService,
// so the file is served via /uploads/tours/... (through the frontend uploads proxy) or
// a Cloudinary secure_url.
@Slf4j
@Service
public class AdminVideoStorageService {

    private static final long MAX_VIDEO_SIZE_BYTES = 100L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("video/mp4", "video/webm");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "webm");

    @Value("${admin.upload-dir:uploads/tours}")
    private String uploadDir;

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    @Value("${ASSET_PUBLIC_BASE:}")
    private String assetPublicBase;

    @Value("${cloudinary.tours-folder:siempre/tours}")
    private String cloudinaryFolder;

    private volatile Cloudinary cloudinary;

    private boolean cloudinaryEnabled() {
        return cloudinaryUrl != null && !cloudinaryUrl.isBlank();
    }

    private Cloudinary cloudinary() {
        if (cloudinary == null) {
            synchronized (this) {
                if (cloudinary == null) {
                    cloudinary = new Cloudinary(cloudinaryUrl);
                    cloudinary.config.secure = true;
                }
            }
        }
        return cloudinary;
    }

    public AdminVideoUploadResponseDto storeTourVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return AdminVideoUploadResponseDto.builder().warning("No video file was provided.").build();
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "tour-video.mp4" : file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = getExtension(originalFilename);

        if (!ALLOWED_CONTENT_TYPES.contains(contentType) || !ALLOWED_EXTENSIONS.contains(extension)) {
            return AdminVideoUploadResponseDto.builder().warning(originalFilename + " is not an MP4 or WebM video.").build();
        }
        if (file.getSize() > MAX_VIDEO_SIZE_BYTES) {
            return AdminVideoUploadResponseDto.builder().warning(originalFilename + " is larger than 100 MB.").build();
        }

        try {
            if (!hasVideoSignature(file, extension)) {
                return AdminVideoUploadResponseDto.builder().warning(originalFilename + " is not a valid " + extension.toUpperCase(Locale.ROOT) + " file.").build();
            }
            String videoUrl = cloudinaryEnabled() ? uploadToCloudinary(file) : storeOnDisk(file, extension);
            if (videoUrl == null) {
                return AdminVideoUploadResponseDto.builder().warning(originalFilename + " could not be stored safely.").build();
            }
            return AdminVideoUploadResponseDto.builder().videoUrl(videoUrl).build();
        } catch (IOException ex) {
            log.warn("Video upload failed for {}", originalFilename, ex);
            return AdminVideoUploadResponseDto.builder().warning(originalFilename + " could not be uploaded.").build();
        }
    }

    // Checks the file's leading bytes so a renamed non-video file is rejected:
    // MP4 has "ftyp" at offset 4, WebM starts with the EBML header 1A 45 DF A3.
    private boolean hasVideoSignature(MultipartFile file, String extension) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        if ("mp4".equals(extension)) {
            return read >= 8 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p';
        }
        return read >= 4 && (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45
                && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3;
    }

    private String uploadToCloudinary(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary().uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", cloudinaryFolder,
                        "public_id", UUID.randomUUID().toString(),
                        "resource_type", "video",
                        "overwrite", false
                )
        );
        Object secureUrl = result.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : null;
    }

    private String storeOnDisk(MultipartFile file, String extension) throws IOException {
        Path targetDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(targetDirectory);

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = targetDirectory.resolve(storedFilename).normalize();
        if (!destination.startsWith(targetDirectory)) {
            return null;
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        String path = "/uploads/tours/" + storedFilename;
        if (assetPublicBase != null && !assetPublicBase.isBlank()) {
            return assetPublicBase.replaceAll("/+$", "") + path;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path)
                .toUriString();
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
