package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.SaveExcelFilleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * Contrôleur REST permettant l'upload de fichiers Excel et leur traitement.
 * Les fichiers sont stockés localement et analysés pour enregistrer les données.
 */
@RestController
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
        // Vérification si le fichier est vide
        if (file.isEmpty()) {
            return "Fichier vide.";
        }

        // Extraction du nom du club (prend uniquement le premier élément si une liste est passée)
        club = club.split(",")[0];

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

            // Enregistrement et traitement du fichier Excel via le service
            saveExcelFilleService.saveExcelFile(destinationFile.getAbsolutePath(), club);

            // Redirection vers la page du club après un upload réussi
            return "redirect:/selectClub/" + club;

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de l'upload du fichier.");
            return "redirect:/selectClub/Erreur lors de l'upload du fichier.";
        }
    }
}
