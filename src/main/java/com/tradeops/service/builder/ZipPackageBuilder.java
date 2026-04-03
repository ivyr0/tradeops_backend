package com.tradeops.service.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZipPackageBuilder implements the Builder design pattern 
 * to fluently construct ZIP archives with a base template and added files.
 */
public class ZipPackageBuilder implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ZipPackageBuilder.class);
    private final ZipOutputStream zos;
    private final Set<String> ignoredTemplateFiles;

    public ZipPackageBuilder(OutputStream os) {
        this.zos = new ZipOutputStream(os);
        // We ignore these files from the template to override them with dynamically generated ones
        this.ignoredTemplateFiles = new HashSet<>(Arrays.asList(".env", "docker-compose.yml", "deploy.sh"));
    }

    public ZipPackageBuilder withTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            log.warn("Template zip '{}' not found, creating archive only with generated files.", templatePath);
            return this;
        }

        try (ZipInputStream zis = new ZipInputStream(resource.getInputStream())) {
            ZipEntry sourceEntry;
            byte[] buffer = new byte[8192];
            while ((sourceEntry = zis.getNextEntry()) != null) {
                if (ignoredTemplateFiles.contains(sourceEntry.getName())) {
                    zis.closeEntry();
                    continue;
                }

                ZipEntry targetEntry = new ZipEntry(sourceEntry.getName());
                zos.putNextEntry(targetEntry);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                zis.closeEntry();
            }
        }
        return this;
    }

    public ZipPackageBuilder withFile(String fileName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        return this;
    }

    @Override
    public void close() throws IOException {
        if (zos != null) {
            zos.close();
        }
    }
}
