package com.example.student_portal.controller;

import com.example.student_portal.dto.RegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Controller responsible for mapping URLs to Thymeleaf view templates
public class WebPageController {

    @GetMapping("/")
    // Redirect root URL to login page
    public String home() {
        return "redirect:/login";
    }

    // Displays login page
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // Displays registration page and binds empty request object for form
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    // Displays student dashboard
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "student/dashboard";
    }

    // Displays list of available courses
    @GetMapping("/courses")
    public String coursesPage() {
        return "student/courses";
    }

    // Displays course detail page (data fetched via API)
    @GetMapping("/courses/{id}")
    public String courseDetailPage() {
        return "student/course-details";
    }

    // Displays enrolled courses page
    @GetMapping("/my-enrollments")
    public String enrolmentsPage() {
        return "student/my-enrollments";
    }

    // Displays student profile page
    @GetMapping("/student-profile")
    public String profilePage() {
        return "student/student-profile";
    }

    // Displays graduation eligibility page
    @GetMapping("/graduation-eligibility")
    public String graduationPage() {
        return "student/graduation-eligibility";
    }

    // Displays student's invoice list page
    @GetMapping("/my-invoices")
    public String myInvoices() {
        return "student/my-invoices";
    }

    // Displays invoice payment page
    @GetMapping("/pay-invoice")
    public String payInvoicePage() {
        return "student/pay-invoice";
    }
}