package travelassistant.controller;

import travelassistant.config.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Value("${app.login.username}")
    private String loginUsername;

    @Value("${app.login.password}")
    private String loginPassword;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute(AuthInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        if (loginUsername.equals(username) && loginPassword.equals(password)) {
            session.setAttribute(AuthInterceptor.SESSION_USER, username);
            return "redirect:/";
        }
        model.addAttribute("error", "Invalid username or password");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
