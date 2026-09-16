package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpSession;
import org.example.belgianslotclubspring.services.AccountService;
import org.example.belgianslotclubspring.services.AccountService.AccountView;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AccountModelAdvice {

    @ModelAttribute("account")
    public AccountView account(HttpSession session) {
        return AccountService.current(session);
    }
}
