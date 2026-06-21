package com.mcpg.web.dto;

import com.mcpg.web.entity.PolicyEntity;

/**
 * Patch body for {@code PUT /api/policies/{id}}.
 *
 * <p>All fields are optional - {@code null} means "leave as-is". This lets
 * the UI toggle {@code enabled} without echoing the rest of the row back.</p>
 */
public class UpdatePolicyRequest {

    private Boolean enabled;
    private PolicyEntity.Severity severity;
    private String configJson;
    private String note;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public PolicyEntity.Severity getSeverity() { return severity; }
    public void setSeverity(PolicyEntity.Severity severity) { this.severity = severity; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
