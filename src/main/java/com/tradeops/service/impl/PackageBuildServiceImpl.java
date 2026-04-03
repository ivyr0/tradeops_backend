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
            cloneTemplateToTempDir(tempDir);

            // 1. Generate the .env file specific to this trader
            String envContent = generateEnvContent(trader);
            Files.writeString(tempDir.resolve(".env"), envContent);

            // 2. Generate the docker-compose.yml to run the Python app
            String dockerComposeContent = generateDockerCompose(trader);
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
                cloneTemplateToTempDir(tempDir);

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

    private void cloneTemplateToTempDir(Path tempDir) {
        try {
            log.info("Cloning trader-cms repository into temporary directory...");
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "clone", "https://github.com/user31133/trader-cms.git", "."
            );
            pb.directory(tempDir.toFile());
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("Failed to clone repository. Exit code: {}", exitCode);
            } else {
                log.info("Successfully cloned trader-cms repository.");
            }
            
            // Clean up the .git directory so it doesn't get zipped
            FileSystemUtils.deleteRecursively(tempDir.resolve(".git"));
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while cloning repository", e);
            Thread.currentThread().interrupt();
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
        return """
# =============================================================================
# SHOP TEMPLATE - Docker Compose Configuration
# =============================================================================
# Before running:
# 1. Copy .env.example to .env
# 2. Configure SHOP_NAME, TRADER_ID, and other settings
# 3. Start main backend first: cd ../online_shop-backend && docker compose up -d
# 4. Run: docker compose up --build

services:
  # PostgreSQL Database (local for shop data)
  postgres:
    image: postgres:15-alpine
    container_name: ${SHOP_NAME:-shop}-db
    environment:
      POSTGRES_DB: shop_data
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  # CMS (Admin Panel for Traders)
  cms:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ${SHOP_NAME:-shop}-cms
    environment:
      DATABASE_URL: postgresql+asyncpg://postgres:postgres@postgres:5432/shop_data
      ADMIN_API_BASE_URL: ${ADMIN_API_BASE_URL:-http://shopbackend:8080}
      JWT_SECRET_KEY: ${JWT_SECRET_KEY:-change-this-secret-key-in-production}
      SESSION_SECRET_KEY: ${SESSION_SECRET_KEY:-change-this-session-secret-in-production}
      SHOP_NAME: ${SHOP_NAME:-My Shop}
      TRADER_ID: ${TRADER_ID:-1}
      JWT_ALGORITHM: HS256
      ACCESS_TOKEN_EXPIRE_MINUTES: 30
      REFRESH_TOKEN_EXPIRE_DAYS: 7
    ports:
      - "${CMS_PORT:-8000}:8000"
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - .:/app
      - static_uploads:/app/static/uploads
    command: uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
    networks:
      - default
      - backend

  # Shop (Customer Storefront)
  shop:
    build:
      context: ./shop
      dockerfile: Dockerfile
    container_name: ${SHOP_NAME:-shop}-storefront
    environment:
      DATABASE_URL: postgresql+asyncpg://postgres:postgres@postgres:5432/shop_data
      ADMIN_API_BASE_URL: ${ADMIN_API_BASE_URL:-http://shopbackend:8080}
      JWT_SECRET_KEY: ${SHOP_JWT_SECRET_KEY:-change-this-shop-secret-key}
      JWT_ALGORITHM: HS256
      ACCESS_TOKEN_EXPIRE_MINUTES: 30
      REFRESH_TOKEN_EXPIRE_DAYS: 7
      SESSION_SECRET_KEY: ${SESSION_SECRET_KEY:-change-this-session-secret-key}
      TRADER_ID: ${TRADER_ID:-1}
      SHOP_NAME: ${SHOP_NAME:-My Shop}
    ports:
      - "${SHOP_PORT:-8001}:8001"
    depends_on:
      postgres:
        condition: service_healthy
      cms:
        condition: service_started
    volumes:
      - ./shop:/app
      - ./app:/trader-cms/app:ro
    networks:
      - default
      - backend

volumes:
  postgres_data:
  static_uploads:

networks:
  default:
  backend:
    external: true
    name: online_shop-backend_default
""";
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
