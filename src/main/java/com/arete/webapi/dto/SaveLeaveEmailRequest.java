package com.arete.webapi.dto;

public class SaveLeaveEmailRequest {

    private String email;
    private FormData formData;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public FormData getFormData() { return formData; }
    public void setFormData(FormData formData) { this.formData = formData; }
}
