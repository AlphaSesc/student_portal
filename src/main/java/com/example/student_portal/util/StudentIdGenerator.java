package com.example.student_portal.util;

import java.util.UUID;

// Utility class for generating unique student identifiers
public final class StudentIdGenerator {

    // Prevent instantiation of utility class
    private StudentIdGenerator() {
    }

    // Generates a short, readable unique student ID (e.g., STU-XXXXXXX)
    public static String generate() {
        return "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}