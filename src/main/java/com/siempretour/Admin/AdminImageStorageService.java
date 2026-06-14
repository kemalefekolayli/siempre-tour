package com.siempretour.Admin;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.siempretour.Admin.Dto.AdminImageUploadResponseDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class AdminImageStorageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 8L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${admin.upload-dir:uploads/tours}")
    private String uploadDir;

    // cloudinary://<api_key>:<api_secret>@<cloud_name> ; empty -> fall back to local disk
    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    // Optional delivery transform applied to every served image (auto format/quality).
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

    public AdminImageUploadResponseDto storeTourImages(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            warnings.add("No image files were provided.");
            return AdminImageUploadResponseDto.builder().imageUrls(imageUrls).warnings(warnings).build();
        }

        boolean useCloudinary = cloudinaryEnabled();

        // Local-disk fallback only needs a directory when Cloudinary is not configured.
        Path targetDirectory = null;
        if (!useCloudinary) {
            targetDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not create upload directory", ex);
            }
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                warnings.add("Skipped an empty file.");
                continue;
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "tour-image" : file.getOriginalFilename());
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            String extension = getExtension(originalFilename);

            if (!ALLOWED_CONTENT_TYPES.contains(contentType) || !ALLOWED_EXTENSIONS.contains(extension)) {
                warnings.add(originalFilename + " is not a supported image type.");
                continue;
            }

            if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
                warnings.add(originalFilename + " is larger than 8 MB.");
                continue;
            }

            try {
                String imageUrl = useCloudinary
                        ? uploadToCloudinary(file)
                        : storeOnDisk(file, extension, targetDirectory);
                if (imageUrl != null) {
                    imageUrls.add(imageUrl);
                } else {
                    warnings.add(originalFilename + " could not be stored safely.");
                }
            } catch (IOException ex) {
                log.warn("Image upload failed for {}", originalFilename, ex);
                warnings.add(originalFilename + " could not be uploaded.");
            }
        }

        return AdminImageUploadResponseDto.builder()
                .imageUrls(imageUrls)
                .warnings(warnings)
                .build();
    }

    private String uploadToCloudinary(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary().uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", cloudinaryFolder,
                        "public_id", UUID.randomUUID().toString(),
                        "resource_type", "image",
                        "overwrite", false,
                        // auto format + quality at delivery for smaller, faster images
                        "fetch_format", "auto",
                        "quality", "auto"
                )
        );
        Object secureUrl = result.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : null;
    }

    private String storeOnDisk(MultipartFile file, String extension, Path targetDirectory) throws IOException {
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = targetDirectory.resolve(storedFilename).normalize();
        if (!destination.startsWith(targetDirectory)) {
            return null;
        }
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/tours/")
                .path(storedFilename)
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
