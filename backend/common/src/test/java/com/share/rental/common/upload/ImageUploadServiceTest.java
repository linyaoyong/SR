package com.share.rental.common.upload;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeImage_rejectsNonImageExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "bad".getBytes());
        ImageUploadService service = new ImageUploadService(testProperties());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.storeImage(file, "items"));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
    }

    @Test
    void storeImage_compressesLargePngToJpgUnderOneMb() throws Exception {
        byte[] png = createLargePngBytes(2400, 2400);
        MockMultipartFile file = new MockMultipartFile("file", "camera.png", "image/png", png);
        ImageUploadService service = new ImageUploadService(testProperties());
        UploadedFile uploaded = service.storeImage(file, "items");
        assertThat(uploaded.url()).startsWith("/files/items/");
        assertThat(uploaded.size()).isLessThanOrEqualTo(1024L * 1024L);
        assertThat(uploaded.filename()).endsWith(".jpg");
        Path expectedFile = tempDir.resolve("items").resolve(uploaded.filename());
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(Files.size(expectedFile)).isLessThanOrEqualTo(1024L * 1024L);
    }

    @Test
    void storeImage_acceptsWebpUnderOneMb() throws Exception {
        byte[] webp = new byte[1024];
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", webp);
        ImageUploadService service = new ImageUploadService(testProperties());
        UploadedFile uploaded = service.storeImage(file, "items");
        assertThat(uploaded.url()).startsWith("/files/items/");
        assertThat(uploaded.url()).endsWith(".webp");
        assertThat(uploaded.filename()).endsWith(".webp");
        Path expectedFile = tempDir.resolve("items").resolve(uploaded.filename());
        assertThat(Files.exists(expectedFile)).isTrue();
    }

    @Test
    void storeImage_rejectsWebpOverOneMb() {
        byte[] webp = new byte[(int) (1024L * 1024L) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", webp);
        ImageUploadService service = new ImageUploadService(testProperties());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.storeImage(file, "items"));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void storeImage_rejectsInvalidBucket() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        ImageUploadService service = new ImageUploadService(testProperties());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.storeImage(file, "evil"));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void resolveUploadRoot_whenStartedFromBackendService_usesProjectSiblingUploads() {
        Path workDir = Paths.get("/workspace/SR/backend/item-service");

        Path resolved = ImageUploadService.resolveUploadRoot(workDir, "../uploads");

        assertThat(resolved).isEqualTo(Paths.get("/workspace/SR/uploads"));
    }

    @Test
    void resolveUploadRoot_whenStartedFromProjectRoot_usesProjectUploads() {
        Path workDir = Paths.get("/Users/linyaoyong/CodeProjects/SR");

        Path resolved = ImageUploadService.resolveUploadRoot(workDir, "../uploads");

        assertThat(resolved).isEqualTo(Paths.get("/Users/linyaoyong/CodeProjects/SR/uploads"));
    }

    private UploadProperties testProperties() {
        UploadProperties properties = UploadProperties.defaults();
        properties.setRoot(tempDir.toString());
        return properties;
    }

    private byte[] createLargePngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, (x * 73 + y * 37) % 0xFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
