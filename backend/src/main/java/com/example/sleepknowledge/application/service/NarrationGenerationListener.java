package com.example.sleepknowledge.application.service;

import com.example.sleepknowledge.application.event.NarrationGenerationRequested;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 관리자 승인이 commit된 generation만 별도 thread에서 합성합니다. */
@Component
public class NarrationGenerationListener {

    private final NarrationGenerationWorker worker;

    public NarrationGenerationListener(NarrationGenerationWorker worker) {
        this.worker = worker;
    }

    @Async("narrationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterContentCommit(NarrationGenerationRequested request) {
        worker.generate(request);
    }
}
