package com.igarciamen.documents.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// Almacenamiento en el sistema de archivos local (sin MinIO/S3 por ahora, tal
// como se decidio para este bloque). Cada tarea tiene su propia subcarpeta,
// y cada fichero se guarda con un nombre generado (UUID), nunca con el nombre
// original -- asi se evita cualquier problema de path traversal o de caracteres
// raros en el nombre que suba el usuario.
@Service
public class DocumentStorageService {

    @Value("${documents.storage-path}")
    private String storagePath;

    public String store(Long taskId, MultipartFile file) {
        try {
            Path taskDir = Path.of(storagePath, taskId.toString());
            Files.createDirectories(taskDir);

            String extension = extractExtension(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + extension;

            Path target = taskDir.resolve(storedFilename);
            file.transferTo(target);

            return storedFilename;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el fichero: " + e.getMessage(), e);
        }
    }

    public Resource load(Long taskId, String storedFilename) {
        Path file = Path.of(storagePath, taskId.toString(), storedFilename);
        if (!Files.exists(file)) {
            throw new IllegalStateException("El fichero ya no existe en disco: " + storedFilename);
        }
        return new FileSystemResource(file);
    }

    public void delete(Long taskId, String storedFilename) {
        Path file = Path.of(storagePath, taskId.toString(), storedFilename);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar el fichero: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
