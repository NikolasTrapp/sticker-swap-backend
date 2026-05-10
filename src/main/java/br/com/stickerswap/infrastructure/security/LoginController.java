package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AppProperties appProperties;

    @GetMapping("/login")
    public String loginPage(Model model) {
        String frontendLoginUrl = appProperties.security().frontendLoginUrl();
        model.addAttribute("passwordResetUrl", appProperties.security().passwordResetUrl());
        model.addAttribute("registerUrl", frontendLoginUrl.replace("/login", "/register"));
        return "login";
    }
}
