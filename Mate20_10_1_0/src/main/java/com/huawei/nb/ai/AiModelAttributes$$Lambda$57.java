package com.huawei.nb.ai;

import com.huawei.nb.model.aimodel.AiModel;
import java.util.function.Consumer;

final /* synthetic */ class AiModelAttributes$$Lambda$57 implements Consumer {
    private final AiModel arg$1;

    private AiModelAttributes$$Lambda$57(AiModel aiModel) {
        this.arg$1 = aiModel;
    }

    static Consumer get$Lambda(AiModel aiModel) {
        return new AiModelAttributes$$Lambda$57(aiModel);
    }

    @Override // java.util.function.Consumer
    public void accept(Object obj) {
        this.arg$1.setSerial_number((String) obj);
    }
}
