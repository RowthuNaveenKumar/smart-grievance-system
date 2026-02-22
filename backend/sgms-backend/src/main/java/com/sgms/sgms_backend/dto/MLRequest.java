package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MLRequest {

    private String complaint_text;
    private String title;
}

