package org.example.belgianslotclubspring.services;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class ForumAttachmentStorage {

    public static final int MAX_FILES = 5;
    public static final long MAX_BYTES = 8L * 1024 * 1024;

    private static final Pattern SAFE_NAME = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|webp|gif|pdf)$");

    public record StoredFile(String storedName, String originalName, String contentType, long sizeBytes) {
    }

    public List<StoredFile> saveAll(List<MultipartFile> files) {
        List<StoredFile> stored = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return stored;
        }
        try {
            Files.createDirectories(directory());
            int kept = 0;
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                if (kept >= MAX_FILES) {
                    break;
                }
                stored.add(saveOne(file));
                kept++;
            }
            return stored;
        } catch (RuntimeException e) {
            stored.forEach(item -> deleteQuietly(item.storedName()));
            throw e;
        } catch (IOException e) {
            stored.forEach(item -> deleteQuietly(item.storedName()));
            throw new IllegalArgumentException("Impossible d’enregistrer les pièces jointes.");
        }
    }

    public Path resolvePublic(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("Pièce jointe introuvable.");
        }
        Path dir = directory().toAbsolutePath().normalize();
        Path path = dir.resolve(filename).normalize();
        if (!path.startsWith(dir) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Pièce jointe introuvable.");
        }
        return path;
    }

    public MediaType mediaType(String filename, String storedContentType) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg" -> MediaType.IMAGE_JPEG;
            default -> storedContentType == null || storedContentType.isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(storedContentType);
        };
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolvePublic(storedName));
        } catch (Exception ignored) {
            // Fichier déjà absent ou nom invalide.
        }
    }

    private StoredFile saveOne(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "fichier" : file.getOriginalFilename();
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("« " + displayName(original) + " » dépasse 8 Mo.");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("« " + displayName(original) + " » dépasse 8 Mo.");
        }
        Detected detected = detect(bytes, original, file.getContentType());
        if (detected == null) {
            throw new IllegalArgumentException(
                    "« " + displayName(original) + " » n’est pas accepté (JPG, PNG, WebP, GIF ou PDF).");
        }
        String storedName = UUID.randomUUID() + "." + detected.extension();
        Files.write(directory().resolve(storedName), bytes);
        return new StoredFile(storedName, sanitizeOriginalName(original, detected.extension()),
                detected.contentType(), bytes.length);
    }

    static Detected detect(byte[] bytes, String originalName, String contentType) {
        if (bytes == null || bytes.length < 5) {
            return null;
        }
        Detected fromMagic = fromMagic(bytes);
        if (fromMagic != null) {
            return fromMagic;
        }
        Detected fromType = fromContentType(contentType);
        if (fromType != null && !"pdf".equals(fromType.extension())) {
            return fromType;
        }
        Detected fromName = fromName(originalName);
        if (fromName != null && !"pdf".equals(fromName.extension())) {
            return fromName;
        }
        return null;
    }

    private static Detected fromMagic(byte[] bytes) {
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return new Detected("jpg", MediaType.IMAGE_JPEG_VALUE);
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return new Detected("png", MediaType.IMAGE_PNG_VALUE);
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return new Detected("gif", MediaType.IMAGE_GIF_VALUE);
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new Detected("webp", "image/webp");
        }
        if (bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F') {
            return new Detected("pdf", MediaType.APPLICATION_PDF_VALUE);
        }
        return null;
    }

    private static Detected fromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT).split(";")[0].trim()) {
            case "image/jpeg", "image/jpg" -> new Detected("jpg", MediaType.IMAGE_JPEG_VALUE);
            case "image/png" -> new Detected("png", MediaType.IMAGE_PNG_VALUE);
            case "image/webp" -> new Detected("webp", "image/webp");
            case "image/gif" -> new Detected("gif", MediaType.IMAGE_GIF_VALUE);
            case "application/pdf" -> new Detected("pdf", MediaType.APPLICATION_PDF_VALUE);
            default -> null;
        };
    }

    private static Detected fromName(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        return switch (originalName.substring(dot + 1).toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> new Detected("jpg", MediaType.IMAGE_JPEG_VALUE);
            case "png" -> new Detected("png", MediaType.IMAGE_PNG_VALUE);
            case "webp" -> new Detected("webp", "image/webp");
            case "gif" -> new Detected("gif", MediaType.IMAGE_GIF_VALUE);
            case "pdf" -> new Detected("pdf", MediaType.APPLICATION_PDF_VALUE);
            default -> null;
        };
    }

    static String sanitizeOriginalName(String original, String extension) {
        Path name = Paths.get(original == null ? "fichier" : original).getFileName();
        String value = name == null ? "fichier" : name.toString();
        value = value.replace('\u0000', '_').replaceAll("[\\r\\n]", "").trim();
        if (value.isBlank()) {
            value = "fichier." + extension;
        }
        if (value.length() > 120) {
            value = value.substring(0, 120);
        }
        return value;
    }

    private static String displayName(String original) {
        return sanitizeOriginalName(original, "bin");
    }

    private static Path directory() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "forum");
    }

    record Detected(String extension, String contentType) {
    }
}
