package com.android.nfc_extras;

public class EeAlreadyOpenException extends EeIOException {
    public EeAlreadyOpenException() {
    }

    public EeAlreadyOpenException(String message) {
        super(message);
    }
}
