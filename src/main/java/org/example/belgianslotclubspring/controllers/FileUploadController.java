package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.ImportAuthService;
import org.example.belgianslotclubspring.services.SaveExcelFilleService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur permettant l'upload de fichiers Excel et leur traitement.
 * Les fichiers sont stockés localement et analysés pour enregistrer les données.
 */
@Controller
public class FileUploadController {

    private final SaveExcelFilleService saveExcelFilleService;
    private final ImportAuthService importAuthService;

    public FileUploadController(SaveExcelFilleService saveExcelFilleService,
                                ImportAuthService importAuthService) {
        this.saveExcelFilleService = saveExcelFilleService;
        this.importAuthService = importAuthService;
    }

    /**
     * Upload d'un ou plusieurs fichiers Excel pour un club.
     */
    @PostMapping("/upload")
    public String handleFileUpload(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile singleFile,
            @RequestParam("club") String club,
            @RequestParam(value = "password", required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        String clubCode = Club.requireCode(club);
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error",
                    "Mot de passe incorrect. L'import a été refusé.");
            return "redirect:/selectRace/" + clubCode;
        }
        List<MultipartFile> toProcess = collectFiles(files, singleFile);

        if (toProcess.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucun fichier sélectionné.");
            return "redirect:/selectRace/" + clubCode;
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            redirectAttributes.addFlashAttribute("error", "Impossible de créer le dossier d'upload.");
            return "redirect:/selectRace/" + clubCode;
        }

        int ok = 0;
        List<String> failures = new ArrayList<>();

        for (MultipartFile file : toProcess) {
            String original = file.getOriginalFilename();
            String displayName = (original == null || original.isBlank()) ? "fichier" : Paths.get(original).getFileName().toString();

            if (file.isEmpty()) {
                failures.add(displayName + " (vide)");
                continue;
            }
            if (!isExcelName(displayName)) {
                failures.add(displayName + " (format non Excel)");
                continue;
            }

            File destinationFile = new File(uploadDir + displayName);
            try {
                file.transferTo(destinationFile);
                saveExcelFilleService.saveExcelFile(destinationFile.getAbsolutePath(), clubCode);
                ok++;
                if (destinationFile.exists() && !destinationFile.delete()) {
                    System.out.println("Impossible de supprimer le fichier Excel : " + destinationFile.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
                failures.add(displayName + " (upload : " + shortMessage(e) + ")");
            } catch (Exception e) {
                e.printStackTrace();
                failures.add(displayName + " (" + shortMessage(e) + ")");
                // conserve le fichier en cas d'échec de traitement pour analyse
            }
        }

        if (ok > 0 && failures.isEmpty()) {
            redirectAttributes.addFlashAttribute("success",
                    ok == 1 ? "1 course importée." : ok + " courses importées.");
        } else if (ok > 0) {
            redirectAttributes.addFlashAttribute("success",
                    ok + " importé(s), " + failures.size() + " en échec : "
                            + String.join(", ", failures));
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Échec de l'import : " + String.join(", ", failures));
        }

        return "redirect:/selectRace/" + clubCode;
    }

    private static List<MultipartFile> collectFiles(MultipartFile[] files, MultipartFile singleFile) {
        List<MultipartFile> list = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    list.add(f);
                }
            }
        }
        if (list.isEmpty() && singleFile != null && !singleFile.isEmpty()) {
            list.add(singleFile);
        }
        return list;
    }

    private static boolean isExcelName(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

    private static String shortMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        message = message.replace('\n', ' ').trim();
        return message.length() > 180 ? message.substring(0, 177) + "…" : message;
    }
}
