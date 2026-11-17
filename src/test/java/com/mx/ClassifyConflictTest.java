package com.mx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassifyConflictTest {

    private static final Path CONFIG_DIR = createConfigDir();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    @Qualifier("classificationExecutor")
    private ExecutorService classificationExecutor;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.config-dir", CONFIG_DIR::toString);
    }

    @Test
    void secondClassifyIsRejected() throws Exception {
        Path bank = Files.createTempDirectory("photo-conflict");
        mockMvc.perform(post("/api/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"冲突仓\",\"path\":\"" + bank + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/photos/classify")).andExpect(status().isOk());
        mockMvc.perform(post("/api/photos/classify")).andExpect(status().isConflict());
    }

    private static Path createConfigDir() {
        try {
            return Files.createTempDirectory("photo-selection-conflict");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
