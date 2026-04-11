package com.example.student_portal.controller;

import com.example.student_portal.dto.RegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebPageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "student/dashboard";
    }

//    @GetMapping("/courses")
//    public String coursesPage() {
//        return "student/courses";
//    }
//
//    @GetMapping("/enrolments")
//    public String enrolmentsPage() {
//        return "student/enrolments";
//    }
//
//    @GetMapping("/profile")
//    public String profilePage() {
//        return "student/profile";
//    }
//
//    @GetMapping("/graduation")
//    public String graduationPage() {
//        return "student/graduation";
//    }
//
//    @GetMapping("/invoices")
//    public String invoicesPage() {
//        return "student/invoices";
//    }
}