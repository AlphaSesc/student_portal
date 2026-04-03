package com.example.student_portal.util;

import java.util.UUID;

public final class StudentIdGenerator {

    private StudentIdGenerator() {
    }

    public static String generate() {
        return "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}