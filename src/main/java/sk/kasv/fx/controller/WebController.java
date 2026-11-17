package sk.kasv.fx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping({"/", "/sluzby/{id}", "/objednavky", "/profil", "/prihlasenie", "/registracia",
            "/objednavky/{id}", "/moje-sluzby", "/poskytovatelia/{id}", "/admin"})
    public String index() { return "forward:/index.html"; }
}
