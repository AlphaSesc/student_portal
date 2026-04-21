package com.example.student_portal.dto.library;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO used to communicate with Library service for creating a student library account
public class CreateLibraryAccountRequest {
    private String studentId;
}