package com.siempretour.Admin;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.siempretour.Admin.Dto.AdminPdfUploadResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Stores the detailed tour guide PDF uploaded from the admin panel. Mirrors
// AdminImageStorageService's Cloudinary-or-local-disk approach so a PDF ends
// up served the same way as tour images (via /uploads/tours/... locally, or
// a Cloudinary secure_url when CLOUDINARY_URL is configured) - both already
// reachable from the frontend through the existing uploads proxy.
@Slf4j
@Service
public class AdminPdfStorageService {

    private static final long MAX_PDF_SIZE_BYTES = 20L * 1024L * 1024L;

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

    public AdminPdfUploadResponseDto storeTourPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return AdminPdfUploadResponseDto.builder().warning("No PDF file was provided.").build();
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "tour-guide.pdf" : file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = getExtension(originalFilename);

        if (!"application/pdf".equals(contentType) || !"pdf".equals(extension)) {
            return AdminPdfUploadResponseDto.builder().warning(originalFilename + " is not a PDF file.").build();
        }
        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            return AdminPdfUploadResponseDto.builder().warning(originalFilename + " is larger than 20 MB.").build();
        }

        try {
            String pdfUrl = cloudinaryEnabled() ? uploadToCloudinary(file) : storeOnDisk(file);
            if (pdfUrl == null) {
                return AdminPdfUploadResponseDto.builder().warning(originalFilename + " could not be stored safely.").build();
            }
            return AdminPdfUploadResponseDto.builder().pdfUrl(pdfUrl).build();
        } catch (IOException ex) {
            log.warn("PDF upload failed for {}", originalFilename, ex);
            return AdminPdfUploadResponseDto.builder().warning(originalFilename + " could not be uploaded.").build();
        }
    }

    private String uploadToCloudinary(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary().uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", cloudinaryFolder,
                        "public_id", UUID.randomUUID().toString(),
                        "resource_type", "raw",
                        "overwrite", false
                )
        );
        Object secureUrl = result.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : null;
    }

    private String storeOnDisk(MultipartFile file) throws IOException {
        Path targetDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(targetDirectory);

        String storedFilename = UUID.randomUUID() + ".pdf";
        Path destination = targetDirectory.resolve(storedFilename).normalize();
        if (!destination.startsWith(targetDirectory)) {
            return null;
        }
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

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
