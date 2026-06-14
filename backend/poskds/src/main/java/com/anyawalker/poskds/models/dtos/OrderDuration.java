package com.anyawalker.poskds.models.dtos;

public enum OrderDuration {
    FAST("fast"),
    MEDIUM("medium"),
    HEAVY("heavy");
    private final String value;
    OrderDuration(String value){
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
