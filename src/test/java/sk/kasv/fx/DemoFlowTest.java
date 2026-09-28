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
        for (String state : new String[]{"ACCEPTED", "IN_PROGRESS", "COMPLETED"}) {
            mvc.perform(patch("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + providerToken)
                    .contentType("application/json").content(json.writeValueAsString(Map.of("status", state))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
        }
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/orders/my")).andExpect(status().isUnauthorized());
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
