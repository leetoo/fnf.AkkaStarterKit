package com.zuehlke.carrera.javapilot.io;

import com.zuehlke.carrera.relayapi.messages.PenaltyMessage;

public class TimedPenaltyMessage extends PenaltyMessage {

    private Long timestamp;
    private PenaltyMessage original;

    public TimedPenaltyMessage () {};

    public TimedPenaltyMessage (PenaltyMessage original, Long timestamp ) {
        this.original = original;
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public PenaltyMessage getOriginal() {
        return original;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setOriginal(PenaltyMessage original) {
        this.original = original;
    }
}
