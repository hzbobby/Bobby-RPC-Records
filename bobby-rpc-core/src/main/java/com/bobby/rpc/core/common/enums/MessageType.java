package com.bobby.rpc.core.common.enums;

public enum MessageType {
    REQUEST((short) 0),
    RESPONSE((short) 1);

    private final short code;

    MessageType(short code) {
        this.code = code;
    }

    public static MessageType fromCode(int messageType) {
        for (MessageType type : MessageType.values()) {
            if (type.code == messageType) {
                return type;
            }
        }
        return null;
    }

    public short getCode() {
        return code;
    }
}
