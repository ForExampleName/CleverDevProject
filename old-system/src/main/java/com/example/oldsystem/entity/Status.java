package com.example.oldsystem.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    ACTIVE((short) 200),
    INACTIVE((short) 210),
    SMTH_ELSE((short) 230);

    private final short code;

    public boolean isActive() {
        return code == ACTIVE.code;
    }

    public static Status getByCode(short code) {
        return switch (code) {
            case 200 -> ACTIVE;
            case 210 -> INACTIVE;
            case 230 -> SMTH_ELSE;
            default -> throw new IllegalArgumentException("Unknown status:" + code);
        };
    }
}
