package com.tradeops;

import com.tradeops.model.entity.Trader;
import com.tradeops.repo.TraderRepo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PackageBuildTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TraderRepo traderRepo;

    @Test
    @WithMockUser(username = "superadmin", authorities = "ROLE_SUPER_ADMIN")
    void testDownloadTraderPackage() throws Exception {
        // 1. Create a dummy trader
        Trader trader = new Trader();
        trader.setDisplayName("Test Store");
        trader.setLegalName("Test Legal Entity");
        trader.setDomain("test-store-" + System.currentTimeMillis() + ".tradeops.kg");
        trader.setStatus(Trader.TraderStatus.ACTIVE);
        trader = traderRepo.save(trader);
        Long traderId = trader.getId();

        // 2. Call the download endpoint
        MvcResult result = mockMvc.perform(post("/api/v1/superadmin/traders/" + traderId + "/package/build"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"trader-" + traderId + "-package.zip\""))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        Assertions.assertTrue(content.length > 0);

        // 3. Verify ZIP contents
        boolean foundEnv = false;
        boolean foundCompose = false;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(".env")) {
                    foundEnv = true;
                    byte[] entryBytes = zis.readAllBytes();
                    String envContent = new String(entryBytes);
                    Assertions.assertTrue(envContent.contains("TRADER_ID=" + traderId));
                    Assertions.assertTrue(envContent.contains("PROJECT_NAME=\"TradeOps Store - Test Store\""));
                } else if (entry.getName().equals("docker-compose.yml")) {
                    foundCompose = true;
                    byte[] entryBytes = zis.readAllBytes();
                    String composeContent = new String(entryBytes);
                    Assertions.assertTrue(composeContent.contains("trader-cms:"));
                }
                zis.closeEntry();
            }
        }

        Assertions.assertTrue(foundEnv, ".env file should be present in ZIP");
        Assertions.assertTrue(foundCompose, "docker-compose.yml should be present in ZIP");
    }
}
