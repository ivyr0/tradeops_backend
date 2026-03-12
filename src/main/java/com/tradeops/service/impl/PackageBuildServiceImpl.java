package com.tradeops.service.impl;

import com.tradeops.annotation.Auditable;
import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.model.entity.PackageArtifact;
import com.tradeops.model.entity.Trader;
import com.tradeops.repo.PackageArtifactRepo;
import com.tradeops.repo.TraderRepo;
import com.tradeops.service.PackageBuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageBuildServiceImpl implements PackageBuildService {

    private final TraderRepo traderRepo;
    private final PackageArtifactRepo packageArtifactRepo;

    @Value("${tradeops.artifacts.dir:/opt/tradeops/artifacts}")
    private String artifactsDir;

    @Value("${tradeops.main.api.baseUrl:https://api.tradeops.kg}")
    private String mainApiBaseUrl;

    @Override
    @Async
    @Auditable(action = "FRONTEND_BUILD_TRIGGERED", entityType = "TRADER")
    public CompletableFuture<String> triggerBuild(Long traderId) {
        log.info("Starting Package Build for Trader ID: {}", traderId);

        Trader trader = traderRepo.findById(traderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found for build"));

        PackageArtifact artifact = new PackageArtifact();
        artifact.setTraderId(traderId);
        artifact.setBuildStatus(PackageArtifact.BuildStatus.PENDING);
        artifact.setArtifactFilePath("");
        artifact = packageArtifactRepo.save(artifact);

        try {
            File dir = new File(artifactsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String zipFileName = "trader_" + traderId + "_package_" + System.currentTimeMillis() + ".zip";
            Path zipFilePath = Paths.get(artifactsDir, zipFileName);

            String envFileContent = generateEnvFile(trader);
            String nginxConfigContent = generateNginxConfig(trader);
            String dockerComposeContent = generateDockerCompose(trader);
            String deployScriptContent = generateDeployScript();

            try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                addStringToZip(zos, ".env", envFileContent);
                addStringToZip(zos, "nginx.conf", nginxConfigContent);
                addStringToZip(zos, "docker-compose.yml", dockerComposeContent);
                addStringToZip(zos, "deploy.sh", deployScriptContent);
            }

            artifact.setArtifactFilePath(zipFilePath.toAbsolutePath().toString());
            artifact.setBuildStatus(PackageArtifact.BuildStatus.SUCCESS);
            packageArtifactRepo.save(artifact);

            log.info("Package Build completed successfully for Trader ID: {}. File: {}", traderId, zipFilePath);
            return CompletableFuture.completedFuture(artifact.getId().toString());

        } catch (Exception e) {
            log.error("Failed to build package for Trader ID: {}", traderId, e);
            artifact.setBuildStatus(PackageArtifact.BuildStatus.FAILED);
            packageArtifactRepo.save(artifact);
            return CompletableFuture.completedFuture("FAILED");
        }
    }

    private void addStringToZip(ZipOutputStream zos, String fileName, String content) throws Exception {
        ZipEntry entry = new ZipEntry(fileName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String generateEnvFile(Trader trader) {
        String apiKey = UUID.randomUUID().toString();
        return "TRADER_ID=" + trader.getId() + "\n" +
               "MAIN_API_BASE_URL=" + mainApiBaseUrl + "\n" +
               "STOREFRONT_API_KEY=" + apiKey + "\n";
    }

    private String generateNginxConfig(Trader trader) {
        return "server {\n" +
               "    listen 80;\n" +
               "    server_name " + trader.getDomain() + ";\n" +
               "    root /usr/share/nginx/html;\n" +
               "    index index.html;\n" +
               "    \n" +
               "    location / {\n" +
               "        try_files $uri $uri/ /index.html;\n" +
               "    }\n" +
               "    \n" +
               "    location /api/v1/ {\n" +
               "        proxy_pass " + mainApiBaseUrl + ";\n" +
               "        proxy_set_header Host $host;\n" +
               "    }\n" +
               "}\n";
    }

    private String generateDockerCompose(Trader trader) {
        return "version: '3.8'\n" +
               "services:\n" +
               "  storefront:\n" +
               "    image: nginx:alpine\n" +
               "    ports:\n" +
               "      - \"80:80\"\n" +
               "    volumes:\n" +
               "      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro\n" +
               "      - ./build:/usr/share/nginx/html:ro\n" +
               "    env_file:\n" +
               "      - .env\n" +
               "    restart: unless-stopped\n";
    }

    private String generateDeployScript() {
        return "#!/bin/bash\n" +
               "echo 'Deploying Trader Package...'\n" +
               "docker-compose down\n" +
               "docker-compose up -d\n" +
               "echo 'Deployment Complete.'\n";
    }
}
