package sk.kasv.fx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnabledIfSystemProperty(named = "runDemoTests", matches = "true")
class DemoFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void realApiSupportsBookingAndProviderCompletion() throws Exception {
        mvc.perform(get("/api/info")).andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(true));
        JsonNode catalog = body(mvc.perform(get("/api/services").param("q", "Montáž")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(catalog.get("totalElements").asInt()).isGreaterThan(0);
        long serviceId = catalog.get("content").get(0).get("id").asLong();
        String customerToken = login("zakaznik@demo.sk");
        String providerToken = login("majster@demo.sk");
        String request = json.writeValueAsString(Map.of("serviceId", serviceId,
                "scheduledAt", LocalDateTime.now(ZoneId.of("Europe/Bratislava")).plusDays(3).withNano(0).toString(),
                "quantity", 2, "address", "Školská 1, Košice", "note", "Ukážka"));
        JsonNode order = body(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                .contentType("application/json").content(request)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(51.0)).andReturn().getResponse().getContentAsString());
        long orderId = order.get("id").asLong();
        mvc.perform(post("/api/orders/" + orderId + "/messages").header("Authorization", "Bearer " + customerToken)
                .contentType("application/json").content("{\"text\":\"Dobrý deň, zvoňte dvakrát.\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/orders/" + orderId + "/messages").header("Authorization", "Bearer " + providerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].senderRole").value("CUSTOMER"))
                .andExpect(jsonPath("$[0].text").value("Dobrý deň, zvoňte dvakrát."));
        for (String state : new String[]{"ACCEPTED", "IN_PROGRESS", "COMPLETED"}) {
            mvc.perform(patch("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + providerToken)
                    .contentType("application/json").content(json.writeValueAsString(Map.of("status", state))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
        }
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/providers/me/stats").header("Authorization", "Bearer " + providerToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersByStatus.COMPLETED").value(1)).andExpect(jsonPath("$.completedTotal").value(1))
                .andExpect(jsonPath("$.earningsTotal").value(51.0)).andExpect(jsonPath("$.ordersTotal").value(1));
        mvc.perform(get("/api/providers/me/stats").header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/my")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders/" + orderId + "/review").header("Authorization", "Bearer " + customerToken)
                .contentType("application/json").content("{\"rating\":5,\"comment\":\"Výborne\"}")).andExpect(status().isCreated());
        JsonNode byRating = body(mvc.perform(get("/api/services").param("sort", "rating")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(byRating.get("content").get(0).get("provider").get("rating").asDouble()).isEqualTo(5.0);
        assertThat(byRating.get("content").get(0).get("provider").get("reviewCount").asInt()).isEqualTo(1);
        JsonNode byPrice = body(mvc.perform(get("/api/services").param("sort", "price_asc")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(byPrice.get("content").get(0).get("price").asDouble())
                .isLessThanOrEqualTo(byPrice.get("content").get(1).get("price").asDouble());
        LocalDateTime when = LocalDateTime.now(ZoneId.of("Europe/Bratislava")).plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);
        mvc.perform(put("/api/providers/me/working-hours").header("Authorization", "Bearer " + providerToken).contentType("application/json")
                .content("{\"hours\":[{\"dayOfWeek\":" + when.getDayOfWeek().getValue() + ",\"start\":\"08:00\",\"end\":\"16:00\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workingHours[0].start").value("08:00:00"));
        String late = json.writeValueAsString(Map.of("serviceId", serviceId, "scheduledAt", when.withHour(18).toString(),
                "quantity", 1, "address", "Školská 1, Košice"));
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken).contentType("application/json").content(late))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/providers/me/days-off").header("Authorization", "Bearer " + providerToken).contentType("application/json")
                .content("{\"date\":\"" + when.toLocalDate() + "\"}")).andExpect(status().isOk());
        String onDayOff = json.writeValueAsString(Map.of("serviceId", serviceId, "scheduledAt", when.toString(),
                "quantity", 1, "address", "Školská 1, Košice"));
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken).contentType("application/json").content(onDayOff))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/providers/" + byRating.get("content").get(0).get("provider").get("id").asLong() + "/availability"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.daysOff[0]").value(when.toLocalDate().toString()));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        JsonNode withPhoto = body(mvc.perform(multipart("/api/providers/me/photo").file(new org.springframework.mock.web.MockMultipartFile("file", "me.png", "image/png", png))
                .header("Authorization", "Bearer " + providerToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String photoUrl = withPhoto.get("profileImage").asText();
        mvc.perform(get(photoUrl.substring(0, photoUrl.indexOf('?')))).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andExpect(content().bytes(png))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=86400")));
        String adminToken = login("admin@demo.sk");
        mvc.perform(patch("/api/admin/users/" + order.get("customerId").asLong()).header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content("{\"blocked\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit").param("action", "USER_BLOCKED").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].actorEmail").value("admin@demo.sk"))
                .andExpect(jsonPath("$.content[0].targetId").value(order.get("customerId").asLong()));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
        JsonNode filtered = body(mvc.perform(get("/api/services").param("q", "Nastavenie").param("location", "Košice"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(filtered.get("totalElements").asInt()).isEqualTo(1);
    }
    private String login(String email) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", "DemoHeslo123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private JsonNode body(String value) throws Exception { return json.readTree(value); }
}
