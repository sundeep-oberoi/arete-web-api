package com.arete.webapi.dto;

public class SaveFormResponse {

    private final String uuid;

    public SaveFormResponse(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
