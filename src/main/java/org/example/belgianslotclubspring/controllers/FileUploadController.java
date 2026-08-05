package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.SaveExcelFilleService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * Contrôleur permettant l'upload de fichiers Excel et leur traitement.
 * Les fichiers sont stockés localement et analysés pour enregistrer les données.
 */
@Controller
public class FileUploadController {

    private final SaveExcelFilleService saveExcelFilleService;

    /**
     * Constructeur injectant le service de sauvegarde des fichiers Excel.
     *
     * @param saveExcelFilleService Service permettant de sauvegarder et traiter un fichier Excel.
     */
    public FileUploadController(SaveExcelFilleService saveExcelFilleService) {
        this.saveExcelFilleService = saveExcelFilleService;
    }

    /**
     * Endpoint permettant d'uploader un fichier Excel et d'enregistrer ses données.
     *
     * @param file Le fichier Excel à uploader.
     * @param club Le nom du club associé aux données du fichier.
     * @return Une redirection vers la page de sélection du club ou un message d'erreur en cas d'échec.
     */
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("club") String club) {
        String clubCode = Club.requireCode(club);

        if (file == null || file.isEmpty()) {
            return "redirect:/selectRace/" + clubCode + "?error=Fichier vide.";
        }

        try {
            // Définition du répertoire d'upload
            String uploadDir = System.getProperty("user.dir") + "/uploads/"; // Chemin absolu
            File directory = new File(uploadDir);

            // Création du répertoire s'il n'existe pas
            if (!directory.exists()) {
                boolean dirCreated = directory.mkdirs();
                System.out.println("Dossier uploads créé : " + dirCreated);
            }

            // Création du fichier de destination
            File destinationFile = new File(uploadDir + file.getOriginalFilename());

            // Transfert du fichier uploadé vers le répertoire défini
            file.transferTo(destinationFile);

            try {
                // Enregistrement et traitement du fichier Excel via le service
                saveExcelFilleService.saveExcelFile(destinationFile.getAbsolutePath(), clubCode);

                if (destinationFile.exists()) {
                    boolean deleted = destinationFile.delete();
                    if (deleted) {
                        System.out.println("Fichier Excel supprimé avec succès : " + destinationFile.getName());
                    } else {
                        System.out.println("Impossible de supprimer le fichier Excel : " + destinationFile.getName());
                    }
                }

                return "redirect:/selectRace/" + clubCode;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur lors du traitement du fichier Excel. Le fichier est conservé pour analyse.");
                return "redirect:/selectRace/" + clubCode + "?error=Erreur lors du traitement du fichier Excel.";
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de l'upload du fichier.");
            return "redirect:/selectRace/" + clubCode + "?error=Erreur lors de l'upload du fichier.";
        }
    }
}
