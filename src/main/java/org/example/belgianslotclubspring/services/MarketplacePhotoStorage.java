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
public class MarketplacePhotoStorage {

    public static final int MAX_PHOTOS = 6;
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Pattern SAFE_NAME = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|webp|gif)$");

    public List<String> saveAll(List<MultipartFile> files) {
        List<String> stored = new ArrayList<>();
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
                if (kept >= MAX_PHOTOS) {
                    break;
                }
                stored.add(saveOne(file));
                kept++;
            }
            return stored;
        } catch (RuntimeException e) {
            stored.forEach(this::deleteQuietly);
            throw e;
        } catch (IOException e) {
            stored.forEach(this::deleteQuietly);
            throw new IllegalArgumentException("Impossible d’enregistrer les photos.");
        }
    }

    public Path resolvePublic(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("Photo introuvable.");
        }
        Path dir = directory().toAbsolutePath().normalize();
        Path path = dir.resolve(filename).normalize();
        if (!path.startsWith(dir) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Photo introuvable.");
        }
        return path;
    }

    public MediaType mediaType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
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

    private String saveOne(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename();
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("« " + displayName(original) + " » dépasse 5 Mo.");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("« " + displayName(original) + " » dépasse 5 Mo.");
        }
        String ext = detectExtension(bytes, original, file.getContentType());
        if (ext == null) {
            throw new IllegalArgumentException(
                    "« " + displayName(original) + " » n’est pas une photo (JPG, PNG, WebP ou GIF).");
        }
        String storedName = UUID.randomUUID() + "." + ext;
        Files.write(directory().resolve(storedName), bytes);
        return storedName;
    }

    private static String detectExtension(byte[] bytes, String originalName, String contentType) {
        if (bytes.length < 12) {
            return null;
        }
        String fromMagic = extensionFromMagic(bytes);
        if (fromMagic != null) {
            return fromMagic;
        }
        String fromType = extensionFromContentType(contentType);
        if (fromType != null) {
            return fromType;
        }
        return extensionFromName(originalName);
    }

    private static String extensionFromMagic(byte[] bytes) {
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "gif";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT).split(";")[0].trim()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    private static String extensionFromName(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        return switch (originalName.substring(dot + 1).toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "webp" -> "webp";
            case "gif" -> "gif";
            default -> null;
        };
    }

    private static String displayName(String original) {
        Path name = Paths.get(original).getFileName();
        String value = name == null ? original : name.toString();
        return value.length() > 40 ? value.substring(0, 40) + "…" : value;
    }

    private static Path directory() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "marketplace");
    }
}
