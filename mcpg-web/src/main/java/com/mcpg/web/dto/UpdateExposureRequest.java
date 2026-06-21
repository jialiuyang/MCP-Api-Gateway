package com.mcpg.web.dto;

import com.mcpg.core.model.ExposureMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /api/exposure}.
 */
public class UpdateExposureRequest {

    @NotNull
    private ExposureMode mode;

    @Size(max = 512)
    private String note;

    public ExposureMode getMode() {
        return mode;
    }

    public void setMode(ExposureMode mode) {
        this.mode = mode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
