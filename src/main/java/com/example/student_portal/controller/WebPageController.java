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

    @GetMapping("/courses")
    public String coursesPage() {
        return "student/courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetailPage() {
        return "student/course-details";
    }

    @GetMapping("/my-enrollments")
    public String enrolmentsPage() {
        return "student/my-enrollments";
    }

    @GetMapping("/student-profile")
    public String profilePage() {
        return "student/student-profile";
    }

    @GetMapping("/graduation-eligibility")
    public String graduationPage() {
        return "student/graduation-eligibility";
    }
//
//    @GetMapping("/invoices")
//    public String invoicesPage() {
//        return "student/invoices";
//    }
}