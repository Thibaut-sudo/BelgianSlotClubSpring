package org.example.belgianslotclubspring.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/contact")
//represents a constructor with required arguments
@RequiredArgsConstructor
public class adminContactController {


    //private final JavaMailSender mailSender;



    @GetMapping
    public String showContactForm() {
        return "pages/contact.html";
    }

    @PostMapping("/send")
    public String sendEmail(@RequestParam String name, @RequestParam String email, @RequestParam String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo("admin@example.com"); // Remplacez par l'adresse de l'admin
            mailMessage.setSubject("Nouveau message de contact");
            mailMessage.setText("Nom: " + name + "\nEmail: " + email + "\nMessage: " + message);

            //mailSender.send(mailMessage);
            return "Email envoyé avec succès !";
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
        return "redirect:/pages/contact.html";
    }
}
