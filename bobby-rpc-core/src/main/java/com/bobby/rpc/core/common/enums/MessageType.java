package com.bobby.rpc.core.common.enums;

public enum MessageType {
    REQUEST((short) 0),
    RESPONSE((short) 1);

    private final short code;

    MessageType(short code) {
        this.code = code;
    }

    public short getCode() {
        return code;
    }
}
