package ai.serviceImpl;

import ai.exception.ServiceProvisioningException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.max-file-size}")
    private long maxFileSize;

    @Value("${app.upload.allowed-extensions}")
    private String allowedExtensions;

    public String saveFile(MultipartFile file) {
        validateFile(file);

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            Files.write(filePath, file.getBytes());

            return filename;
        } catch (IOException e) {
            throw new ServiceProvisioningException("Failed to save file: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ServiceProvisioningException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new ServiceProvisioningException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedExtensions.split(",")));

        if (!allowed.contains(extension.toLowerCase())) {
            throw new ServiceProvisioningException("File type not allowed. Allowed types: " + allowedExtensions);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public Path getFilePath(String filename) {
        return Paths.get(uploadDir).resolve(filename);
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = getFilePath(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ServiceProvisioningException("Failed to delete file: " + e.getMessage());
        }
    }
}
