package com.tradeops.service;

import java.util.concurrent.CompletableFuture;

public interface PackageBuildService {
    CompletableFuture<String> triggerBuild(Long traderId);

    byte[] generateTraderPackage(Long traderId) throws java.io.IOException;
}
