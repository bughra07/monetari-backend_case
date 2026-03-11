package com.bugra.monetari.service;

import com.bugra.monetari.dto.PriceResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchState {

    private final List<CompletableFuture<PriceResponse>> waitingRequests = new CopyOnWriteArrayList<>();
    private final AtomicBoolean flushed = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledTask;

    public List<CompletableFuture<PriceResponse>> getWaitingRequests() {
        return waitingRequests;
    }

    public AtomicBoolean getFlushed() {
        return flushed;
    }

    public ScheduledFuture<?> getScheduledTask() {
        return scheduledTask;
    }

    public void setScheduledTask(ScheduledFuture<?> scheduledTask) {
        this.scheduledTask = scheduledTask;
    }
}