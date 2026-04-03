package com.tradeops.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ResourceUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageBuildServiceImpl implements PackageBuildService {

    private final TraderRepo traderRepo;
    private final PackageArtifactRepo packageArtifactRepo;

    @Value("${tradeops.artifacts.dir:target/artifacts}")
    private String artifactsDir;

    @Value("${tradeops.main.api.baseUrl:https://api.tradeops.kg}")
    private String mainApiBaseUrl;

    @Override
    public byte[] generateTraderPackage(Long traderId) throws IOException {
        log.info("Generating direct package ZIP for Trader ID: {}", traderId);
        Trader trader = traderRepo.findById(traderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found"));

        Path tempDir = Files.createTempDirectory("trader-pkg-");
        try {
            copyTemplateToTempDir(tempDir);

            // 1. Generate the .env file specific to this trader
            String envContent = generateEnvContent(trader);
            Files.writeString(tempDir.resolve(".env"), envContent);

            // 2. Generate the docker-compose.yml to run the Python app
            String dockerComposeContent = generateDockerComposeContent();
            Files.writeString(tempDir.resolve("docker-compose.yml"), dockerComposeContent);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            zipDirectory(tempDir, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate package for Trader ID: {}", traderId, e);
            throw new IOException("Failed to generate package ZIP", e);
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    private String generateEnvContent(Trader trader) {
        return "PROJECT_NAME=\"TradeOps Store - " + trader.getDisplayName() + "\"\n" +
                "TRADER_ID=" + trader.getId() + "\n" +
                "BACKEND_URL=\"" + mainApiBaseUrl + "\"\n" +
                "DATABASE_URL=\"sqlite:///./trader.db\"\n" +
                "SECRET_KEY=\"" + UUID.randomUUID().toString() + "\"\n";
    }

    private String generateDockerComposeContent() {
        return "version: '3.8'\n" +
                "services:\n" +
                "  trader-cms:\n" +
                "    image: tradeops/trader-cms:latest\n" +
                "    ports:\n" +
                "      - \"8000:8000\"\n" +
                "    env_file:\n" +
                "      - .env\n" +
                "    restart: always\n";
    }

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
            String dockerComposeContent = generateDockerCompose(trader);
            String deployScriptContent = generateDeployScript();

            Path tempDir = Files.createTempDirectory("trader-pkg-build-");
            try {
                copyTemplateToTempDir(tempDir);

                Files.writeString(tempDir.resolve(".env"), envFileContent);
                Files.writeString(tempDir.resolve("docker-compose.yml"), dockerComposeContent);
                Files.writeString(tempDir.resolve("deploy.sh"), deployScriptContent);

                try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile())) {
                    zipDirectory(tempDir, fos);
                }
            } finally {
                FileSystemUtils.deleteRecursively(tempDir);
            }

            artifact.setArtifactFilePath(zipFilePath.toAbsolutePath().toString());
            artifact.setBuildStatus(PackageArtifact.BuildStatus.SUCCESS);
            packageArtifactRepo.save(artifact);

            log.info("Package Build completed successfully for Trader ID: {}. File: {}", traderId, zipFilePath);
            return CompletableFuture.completedFuture("BUILD_SUCCESS");

        } catch (Exception e) {
            log.error("Failed to build package for Trader ID: {}", traderId, e);
            artifact.setBuildStatus(PackageArtifact.BuildStatus.FAILED);
            packageArtifactRepo.save(artifact);
            return CompletableFuture.completedFuture("FAILED");
        }
    }

    private void copyTemplateToTempDir(Path tempDir) {
        try {
            File templateDir;
            try {
                templateDir = ResourceUtils.getFile("classpath:templates/trader-cms");
            } catch (FileNotFoundException e) {
                templateDir = new File("src/main/resources/templates/trader-cms");
            }
            if (templateDir.exists() && templateDir.isDirectory()) {
                FileSystemUtils.copyRecursively(templateDir, tempDir.toFile());
            } else {
                log.warn("Template directory not found, using empty directory.");
            }
        } catch (IOException e) {
            log.error("Failed to copy template directory", e);
        }
    }

    private void zipDirectory(Path sourceDir, OutputStream os) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(os);
             Stream<Path> paths = Files.walk(sourceDir)) {
             
            paths.filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString().replace("\\", "/"));
                try {
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to zip file " + path, e);
                }
            });
        }
    }

    private String generateEnvFile(Trader trader) {
        // Генерируем уникальные секретные ключи для сессий и JWT
        String jwtSecret = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String shopJwtSecret = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String sessionSecret = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        return "SHOP_NAME=\"" + trader.getDisplayName() + "\"\n" +
                "TRADER_ID=" + trader.getId() + "\n" +
                "ADMIN_API_BASE_URL=" + mainApiBaseUrl + "\n" +
                "DATABASE_URL=postgresql+asyncpg://postgres:postgres@postgres:5432/shop_data\n" +
                "JWT_SECRET_KEY=" + jwtSecret + "\n" +
                "SHOP_JWT_SECRET_KEY=" + shopJwtSecret + "\n" +
                "SESSION_SECRET_KEY=" + sessionSecret + "\n";
    }

    private String generateDockerCompose(Trader trader) {
        return "version: '3.8'\n" +
                "services:\n" +
                "  postgres:\n" +
                "    image: postgres:15-alpine\n" +
                "    container_name: trader_" + trader.getId() + "_db\n" +
                "    environment:\n" +
                "      POSTGRES_DB: shop_data\n" +
                "      POSTGRES_USER: postgres\n" +
                "      POSTGRES_PASSWORD: postgres\n" +
                "    volumes:\n" +
                "      - postgres_data:/var/lib/postgresql/data\n" +
                "    healthcheck:\n" +
                "      test: [\"CMD-SHELL\", \"pg_isready -U postgres\"]\n" +
                "      interval: 5s\n" +
                "      timeout: 5s\n" +
                "      retries: 5\n" +
                "    restart: always\n\n" +
                "  cms:\n" +
                "    image: tradeops/trader-cms:latest # Укажите здесь реальный Docker-образ вашего CMS\n" +
                "    container_name: trader_" + trader.getId() + "_cms\n" +
                "    env_file: .env\n" +
                "    ports:\n" +
                "      - \"8000:8000\"\n" +
                "    depends_on:\n" +
                "      postgres:\n" +
                "        condition: service_healthy\n" +
                "    volumes:\n" +
                "      - static_uploads:/app/static/uploads\n" +
                "    restart: always\n\n" +
                "  shop:\n" +
                "    image: tradeops/trader-shop:latest # Укажите здесь реальный Docker-образ вашей Витрины\n" +
                "    container_name: trader_" + trader.getId() + "_shop\n" +
                "    env_file: .env\n" +
                "    ports:\n" +
                "      - \"8001:8001\"\n" +
                "    depends_on:\n" +
                "      postgres:\n" +
                "        condition: service_healthy\n" +
                "    restart: always\n\n" +
                "volumes:\n" +
                "  postgres_data:\n" +
                "  static_uploads:\n";
    }

    private String generateDeployScript() {
        return "#!/bin/bash\n" +
                "echo 'Deploying Trader CMS & Shop...'\n" +
                "docker-compose down\n" +
                "docker-compose pull\n" +
                "docker-compose up -d\n" +
                "echo 'Deployment Complete!'\n" +
                "echo 'CMS is running on port 8000'\n" +
                "echo 'Shop is running on port 8001'\n";
    }
}
