package com.share.rental.common.upload;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class ImageUploadService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final float[] QUALITY_STEPS = {0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f};
    private static final double SCALE_DOWN_RATIO = 0.75;
    private static final int MIN_DIMENSION = 100;

    private final UploadProperties properties;

    public ImageUploadService(UploadProperties properties) {
        this.properties = properties;
    }

    public UploadedFile storeImage(MultipartFile file, String bucket) {
        validateBucket(bucket);
        String filename = file.getOriginalFilename();
        String extension = extractExtension(filename);
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        try {
            byte[] data;
            String finalExtension;
            String contentType;
            if ("webp".equals(extension)) {
                data = file.getBytes();
                if (data.length > properties.getMaxBytes()) {
                    throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                }
                finalExtension = "webp";
                contentType = "image/webp";
            } else {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
                }
                BufferedImage rgb = toRgb(image);
                data = compressToJpgUnderLimit(rgb, properties.getMaxBytes());
                finalExtension = "jpg";
                contentType = "image/jpeg";
            }
            String storedName = generateFilename(finalExtension);
            Path targetDir = resolveUploadRoot(Paths.get("").toAbsolutePath().normalize(), properties.getRoot())
                    .resolve(bucket);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(storedName);
            Files.write(target, data);
            String url = "/files/" + bucket + "/" + storedName;
            return new UploadedFile(url, storedName, contentType, data.length);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "图片上传失败: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "图片上传失败");
        }
    }

    private void validateBucket(String bucket) {
        if (!properties.getAllowedBuckets().contains(bucket)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传目录不支持: " + bucket);
        }
    }

    static Path resolveUploadRoot(Path workingDirectory, String configuredRoot) {
        Path configuredPath = Paths.get(configuredRoot);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        Path uploadDirectoryName = configuredPath.getFileName();
        Path current = workingDirectory.toAbsolutePath().normalize();
        while (current != null) {
            Path fileName = current.getFileName();
            if (fileName != null && "backend".equals(fileName.toString()) && current.getParent() != null) {
                return current.getParent().resolve(uploadDirectoryName).normalize();
            }
            current = current.getParent();
        }
        if (configuredPath.startsWith("..") && uploadDirectoryName != null) {
            return workingDirectory.resolve(uploadDirectoryName).normalize();
        }
        return workingDirectory.resolve(configuredPath).normalize();
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private BufferedImage toRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private byte[] compressToJpgUnderLimit(BufferedImage image, long maxBytes) throws IOException {
        BufferedImage current = image;
        while (true) {
            for (float quality : QUALITY_STEPS) {
                byte[] data = writeJpg(current, quality);
                if (data.length <= maxBytes) {
                    return data;
                }
            }
            int newWidth = (int) (current.getWidth() * SCALE_DOWN_RATIO);
            int newHeight = (int) (current.getHeight() * SCALE_DOWN_RATIO);
            if (newWidth < MIN_DIMENSION || newHeight < MIN_DIMENSION) {
                return writeJpg(current, QUALITY_STEPS[QUALITY_STEPS.length - 1]);
            }
            BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(current, 0, 0, newWidth, newHeight, null);
            g.dispose();
            current = scaled;
        }
    }

    private byte[] writeJpg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (ImageOutputStream output = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(output);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private String generateFilename(String extension) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return timestamp + "-" + uuid + "." + extension;
    }
}
