package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.SaveExcelFilleService;
import org.example.belgianslotclubspring.utils.ExcelReader;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
public class FileUploadController {



    private final SaveExcelFilleService saveExcelFilleService;

    public FileUploadController(SaveExcelFilleService saveExcelFilleService) {
        this.saveExcelFilleService = saveExcelFilleService;
    }


    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("club") String club) {
        if (file.isEmpty()) {
            return "Fichier vide.";
        }

        club = club.split(",")[0];
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/"; // Chemin absolu
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                boolean dirCreated = directory.mkdirs();
                System.out.println("Dossier uploads créé : " + dirCreated);
            }

            File destinationFile = new File(uploadDir + file.getOriginalFilename());
            file.transferTo(destinationFile);



            saveExcelFilleService.saveExcelFile(destinationFile.getAbsolutePath(),club);




            return "redirect:/selectClub/" + club;



        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de l'upload du fichier.");
            return "redirect:/selectClub/Erreur lors de l'upload du fichier." ;
        }
    }
}
