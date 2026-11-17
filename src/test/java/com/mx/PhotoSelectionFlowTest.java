package com.mx;

import com.mx.logic.storage.ClassificationStorage;
import com.mx.model.bo.WarehouseBO;
import com.mx.service.IWarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhotoSelectionFlowTest {

    private static final Path CONFIG_DIR = createConfigDir();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassificationStorage classificationService;

    @Autowired
    private IWarehouseService warehouseService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.config-dir", CONFIG_DIR::toString);
    }

    @Test
    void pagingSelectionSwitchAndClassify() throws Exception {
        Path bank = Files.createTempDirectory("photo-bank");
        writeJpeg(bank.resolve("IMG_1.JPG"), 0x111111);
        writeJpeg(bank.resolve("IMG_2.JPG"), 0x222222);
        writeJpeg(bank.resolve("IMG_3.JPG"), 0x333333);
        Files.createDirectories(bank.resolve("RAW"));
        Files.writeString(bank.resolve("RAW").resolve("IMG_1.CR2"), "raw-1");
        Files.writeString(bank.resolve("RAW").resolve("IMG_10.CR2"), "raw-10");
        Path outside = bank.getParent().resolve("outside-shot.jpg");
        writeJpeg(outside, 0xff00ff);
        Files.createSymbolicLink(bank.resolve("escape.jpg"), outside);

        String warehouseId = addWarehouse("主仓", bank);

        MvcResult pageOne = mockMvc.perform(get("/api/photos").param("page", "1").param("pageSize", "1").param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].fileName").value("IMG_1.JPG"))
                .andReturn();
        mockMvc.perform(get("/api/photos").param("page", "2").param("pageSize", "1").param("sort", "name"))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].fileName").value("IMG_2.JPG"));
        mockMvc.perform(get("/api/photos").param("page", "9").param("pageSize", "1").param("sort", "name"))
                .andExpect(jsonPath("$.data.list.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(3));

        String firstId = com.jayway.jsonpath.JsonPath.read(pageOne.getResponse().getContentAsString(), "$.data.list[0].id");
        mockMvc.perform(post("/api/photos/" + firstId + "/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"selected\"}"))
                .andExpect(status().isOk());
        assertTrue(Files.exists(bank.resolve("选片").resolve("JPG").resolve("IMG_1.JPG")));
        assertTrue(Files.exists(bank.resolve("选片").resolve("RAW").resolve("IMG_1.CR2")));
        assertFalse(Files.exists(bank.resolve("选片").resolve("RAW").resolve("IMG_10.CR2")));
        String statusJson = Files.readString(bank.resolve(".selection_status.json"));
        assertTrue(statusJson.contains("selected"));
        assertTrue(statusJson.contains("IMG_1.JPG"));

        mockMvc.perform(post("/api/photos/" + firstId + "/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"unmarked\"}"))
                .andExpect(status().isOk());
        assertFalse(Files.exists(bank.resolve("选片").resolve("JPG").resolve("IMG_1.JPG")));
        assertFalse(Files.exists(bank.resolve("选片").resolve("RAW").resolve("IMG_1.CR2")));
        assertTrue(Files.exists(bank.resolve("RAW").resolve("IMG_1.CR2")));

        mockMvc.perform(post("/api/photos/batch-mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"" + firstId + "\",\"status\":\"nope\"},{\"id\":\"missing\",\"status\":\"selected\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(0))
                .andExpect(jsonPath("$.data.failed.length()").value(2));

        mockMvc.perform(get("/api/photos/" + firstId + "/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=")));

        mockMvc.perform(get("/api/photos/" + firstId + "/thumbnail").param("w", "10"))
                .andExpect(status().isOk());
        assertTrue(Files.exists(bank.resolve(".thumbnails").resolve("200")));
        assertFalse(Files.exists(bank.resolve(".thumbnails").resolve("10")));

        Path empty = Files.createTempDirectory("photo-empty");
        String emptyId = addWarehouse("空仓", empty);
        mockMvc.perform(put("/api/warehouse/" + emptyId + "/active"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/photos").param("size", "0"))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(post("/api/photos/" + firstId + "/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"selected\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/warehouse/" + warehouseId + "/active")).andExpect(status().isOk());
        Path keepJpg = bank.resolve("JPG").resolve("KEEP.jpg");
        writeJpeg(keepJpg, 0x444444);
        byte[] keepBefore = Files.readAllBytes(keepJpg);
        writeJpeg(bank.resolve("KEEP.jpg"), 0x555555);

        mockMvc.perform(post("/api/photos/classify")).andExpect(status().isOk());
        awaitClassification();
        assertTrue(sameBytes(keepBefore, Files.readAllBytes(keepJpg)));
        assertTrue(Files.exists(bank.resolve("KEEP.jpg")));
        String savedPath = warehouseService.listWarehouses().stream()
                .filter(item -> item.getId().equals(warehouseId))
                .map(WarehouseBO::getPath)
                .findFirst()
                .orElseThrow();
        assertTrue(savedPath.endsWith("/JPG"), savedPath);
        mockMvc.perform(get("/api/photos/classify/progress"))
                .andExpect(jsonPath("$.data.progress").value(org.hamcrest.Matchers.lessThanOrEqualTo(100)));

        mockMvc.perform(post("/api/warehouse").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"path\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/warehouse/missing-id")).andExpect(status().isNotFound());
    }

    private String addWarehouse(String name, Path path) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"path\":\"" + path + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private void awaitClassification() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (!classificationService.isClassifying()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("分类没有结束: " + classificationService.getCurrentPhase());
    }

    private static Path createConfigDir() {
        try {
            return Files.createTempDirectory("photo-selection-config");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeJpeg(Path file, int color) throws Exception {
        BufferedImage image = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                image.setRGB(x, y, color);
            }
        }
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "jpg", file.toFile());
    }

    private static boolean sameBytes(byte[] left, byte[] right) {
        return java.util.Arrays.equals(left, right);
    }
}
