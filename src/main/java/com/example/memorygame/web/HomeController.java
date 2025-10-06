package com.example.memorygame.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    // Startseite (Lobby mit Host- und Join-Bereich)
    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html
    }

    // Host-Ansicht (großer Bildschirm)
    @GetMapping("/host/{pin}")
    public String host(@PathVariable String pin, Model model) {
        model.addAttribute("pin", pin); // optional für Thymeleaf-Nutzung
        return "host"; // templates/host.html
    }

    // Spieler-Ansicht (Handy)
    @GetMapping("/player/{pin}")
    public String player(@PathVariable String pin, Model model) {
        model.addAttribute("pin", pin); // optional für Thymeleaf-Nutzung
        return "player"; // templates/player.html
    }
}
