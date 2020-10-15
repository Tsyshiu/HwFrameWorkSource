package com.android.server.accessibility;

abstract class BaseEventStreamTransformation implements EventStreamTransformation {
    private EventStreamTransformation mNext;

    BaseEventStreamTransformation() {
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void setNext(EventStreamTransformation next) {
        this.mNext = next;
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public EventStreamTransformation getNext() {
        return this.mNext;
    }
}
