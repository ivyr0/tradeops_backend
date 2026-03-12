package com.tradeops.service;

import java.util.concurrent.CompletableFuture;

public interface PackageBuildService {
    CompletableFuture<String> triggerBuild(Long traderId);
}
