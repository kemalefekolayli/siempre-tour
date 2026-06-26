package com.siempretour.Chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siempretour.Chat.Dto.ChatMessage;
import com.siempretour.Chat.Dto.ChatRequest;
import com.siempretour.Chat.Dto.ChatResponse;
import com.siempretour.Tours.TourRepository;
import com.siempretour.Tours.Models.Tour;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Talks to the OpenAI Chat Completions API on behalf of the public chat widget.
 *
 * Grounding strategy (Approach B — function calling): instead of stuffing the
 * whole catalogue into the prompt (which doesn't scale past a handful of tours),
 * the model is given a `search_tours` tool. When the user asks about tours /
 * destinations, the model calls the tool, we run a DB search, return the matches,
 * and the model answers from real data. This scales to the full catalogue
 * (thousands of tours) and stays cheap because only relevant tours are returned.
 *
 * The system prompt is fixed here (not taken from the client) so the assistant
 * stays scoped to travel / Siempre Tour topics and cannot be re-purposed.
 */
@Slf4j
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT =
            "Sen Siempre Tour seyahat acentesinin yardımsever asistanısın. Görevin gezginlere " +
            "seyahat konularında yardımcı olmak. Seyahatle ilgili HER konuda yardım et: ülkeler ve " +
            "destinasyonlar (ör. Japonya, İtalya, Küba), gezilecek yerler, en iyi gezi zamanı, " +
            "kültür, mutfak, gezi önerileri, turlar, tatil, otel, uçuş, vize, pasaport ve Siempre " +
            "Tour hizmetleri. Bir destinasyon hakkında soru sorulursa MUTLAKA faydalı bilgi ver, " +
            "asla reddetme. Sadece seyahatle TAMAMEN alakasız konularda (programlama, matematik, " +
            "siyaset, oyun, ödev vb.) kibarca yardımcı olamayacağını belirtip konuyu seyahate " +
            "yönlendir. Kısa, sıcak ve yardımsever ol. Kullanıcı Türkçe yazıyorsa Türkçe, İngilizce " +
            "yazıyorsa İngilizce cevap ver.\n\n" +
            "Siempre Tour'un SOMUT TURLARI, fiyatları, tarihleri veya bir destinasyona ait turlar " +
            "sorulduğunda MUTLAKA 'search_tours' aracını kullanarak veritabanında ara. Tur " +
            "bilgilerini ASLA uydurma; sadece aracın döndürdüğü sonuçları kullan. Arama sonucu boşsa " +
            "o destinasyon için planlı tur olmadığını söyle, genel seyahat bilgisi ver ve iletişim " +
            "formundan özel tur talep edilebileceğini belirt. Bir tur önerirken adını, kısa " +
            "bilgisini ve link yolunu (template_tour_page.html?id=SLUG) ver.";

    /** Tool exposed to the model. */
    private static final List<Map<String, Object>> TOOLS = List.of(Map.of(
            "type", "function",
            "function", Map.of(
                    "name", "search_tours",
                    "description", "Siempre Tour veritabanında yayında olan turları arar. Kullanıcı " +
                            "belirli bir destinasyon, şehir, tema veya tur tipi sorduğunda kullan. " +
                            "Eşleşen turların adı, destinasyonu, süresi, fiyatı, tarihi ve link " +
                            "bilgisini döndürür.",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "query", Map.of(
                                            "type", "string",
                                            "description", "Aranacak anahtar kelime: destinasyon/ülke " +
                                                    "(ör. 'Japonya', 'Küba'), şehir (ör. 'Kyoto') veya " +
                                                    "tema (ör. 'safari', 'balayı'). Genel 'yaklaşan " +
                                                    "turlar' için boş bırakılabilir."),
                                    "language", Map.of(
                                            "type", "string",
                                            "enum", List.of("tr", "en"),
                                            "description", "Turların dili; kullanıcının diline göre ayarla.")
                            ),
                            "required", List.of("query")
                    )
            )
    ));

    private static final int RESULT_LIMIT = 15;   // tours returned per search
    private static final int MAX_TOOL_ROUNDS = 3; // safety cap on tool-call loops
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RestClient restClient;
    private final TourRepository tourRepository;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public ChatService(
            TourRepository tourRepository,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.max-tokens:500}") int maxTokens) {
        this.tourRepository = tourRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.maxTokens = maxTokens;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ChatResponse chat(ChatRequest req) {
        boolean en = req != null && "en".equalsIgnoreCase(req.getLanguage());
        String defaultLang = en ? "en" : "tr";
        String userMessage = req == null || req.getMessage() == null ? "" : req.getMessage().trim();

        if (userMessage.isEmpty()) {
            return new ChatResponse(en ? "Please type a message." : "Lütfen bir mesaj yazın.");
        }
        if (apiKey.isEmpty()) {
            log.warn("Chat called but openai.api-key is not configured — returning fallback message.");
            return new ChatResponse(en
                    ? "The assistant is not available right now. Please contact us directly."
                    : "Asistan şu anda kullanılamıyor. Lütfen bizimle doğrudan iletişime geçin.");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", SYSTEM_PROMPT));
        if (req.getHistory() != null) {
            for (ChatMessage m : req.getHistory()) {
                if (m == null || m.getContent() == null || m.getContent().isBlank()) continue;
                if (Boolean.TRUE.equals(m.getFailed())) continue; // skip error bubbles
                String role = "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
                if ("user".equals(role) && m.getContent().trim().equals(userMessage)) continue; // de-dupe
                messages.add(msg(role, m.getContent()));
            }
        }
        messages.add(msg("user", userMessage));

        try {
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                JsonNode message = callOpenAi(messages);
                JsonNode toolCalls = message.path("tool_calls");

                if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                    // Echo the assistant's tool-call request back into the thread...
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", message.hasNonNull("content") ? message.get("content").asText() : null);
                    assistantMsg.put("tool_calls", objectMapper.convertValue(toolCalls, List.class));
                    messages.add(assistantMsg);

                    // ...then run each requested tool and append its result.
                    for (JsonNode tc : toolCalls) {
                        String id = tc.path("id").asText();
                        String name = tc.path("function").path("name").asText();
                        String argsJson = tc.path("function").path("arguments").asText("{}");
                        String result = runTool(name, argsJson, defaultLang);
                        Map<String, Object> toolMsg = new HashMap<>();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", id);
                        toolMsg.put("content", result);
                        messages.add(toolMsg);
                    }
                    continue; // ask the model again, now with tool results
                }

                String content = message.path("content").asText("");
                if (content.isBlank()) break;
                return new ChatResponse(content.trim());
            }
            log.warn("Chat exhausted tool rounds without a final answer.");
        } catch (Exception e) {
            log.error("OpenAI chat request failed: {}", e.getMessage());
        }
        return new ChatResponse(en
                ? "Sorry, I couldn't connect right now. Please try again."
                : "Üzgünüm, şu anda bağlanamadım. Lütfen tekrar deneyin.");
    }

    /** One Chat Completions call; returns choices[0].message as a JsonNode. */
    private JsonNode callOpenAi(List<Map<String, Object>> messages) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", 0.7);
        body.put("tools", TOOLS);
        body.put("tool_choice", "auto");

        String raw = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(raw);
        return root.path("choices").path(0).path("message");
    }

    /** Dispatches a tool call from the model. Currently only `search_tours`. */
    private String runTool(String name, String argsJson, String defaultLang) {
        if (!"search_tours".equals(name)) {
            return "Bilinmeyen araç: " + name;
        }
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            String query = args.path("query").asText("").trim();
            String lang = args.path("language").asText(defaultLang);
            if (!"tr".equalsIgnoreCase(lang) && !"en".equalsIgnoreCase(lang)) lang = defaultLang;
            return searchTours(query, lang);
        } catch (Exception e) {
            log.warn("search_tours failed to parse args {}: {}", argsJson, e.getMessage());
            return "Arama yapılamadı.";
        }
    }

    private String searchTours(String query, String lang) {
        var page = PageRequest.of(0, RESULT_LIMIT);
        List<Tour> results = tourRepository.searchForChat(query, lang, page);
        // If nothing in the requested language, retry across all languages.
        if (results.isEmpty()) {
            results = tourRepository.searchForChat(query, null, page);
        }
        if (results.isEmpty()) {
            return "Bu aramaya uygun yayında tur bulunamadı.";
        }
        StringBuilder sb = new StringBuilder("Bulunan turlar (sadece bunları kullan):\n");
        for (Tour t : results) {
            sb.append(tourLine(t)).append('\n');
        }
        return sb.toString();
    }

    private String tourLine(Tour t) {
        StringBuilder line = new StringBuilder("- ");
        line.append(nz(t.getName()));
        if (t.getDestination() != null && !t.getDestination().isBlank()) {
            line.append(" | Destinasyon: ").append(t.getDestination());
        }
        if (t.getDuration() != null) {
            line.append(" | ").append(t.getDuration()).append(" gün");
        }
        BigDecimal price = t.getDiscountedPrice() != null ? t.getDiscountedPrice() : t.getPrice();
        if (price != null) {
            line.append(" | Fiyat: ").append(money(price));
            if (t.getDiscountedPrice() != null && t.getPrice() != null) {
                line.append(" (indirimli, normal ").append(money(t.getPrice())).append(")");
            }
        }
        if (t.getStartDate() != null) {
            line.append(" | Tarih: ").append(t.getStartDate().format(DATE_FMT));
            if (t.getEndDate() != null) {
                line.append("–").append(t.getEndDate().format(DATE_FMT));
            }
        }
        if (t.getDepartureCity() != null && !t.getDepartureCity().isBlank()) {
            line.append(" | Kalkış: ").append(t.getDepartureCity());
        }
        String places = clip(t.getPlacesVisited(), 120);
        if (places != null && !places.isBlank()) {
            line.append(" | Yerler: ").append(places);
        }
        if (t.getSlug() != null && !t.getSlug().isBlank()) {
            line.append(" | Link: template_tour_page.html?id=").append(t.getSlug());
        }
        return line.toString();
    }

    private static Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String money(BigDecimal value) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY); // 1.234,56 style
        DecimalFormat fmt = new DecimalFormat("#,##0.##", sym);
        return fmt.format(value);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /** Strips HTML tags and truncates to maxLen chars. */
    private static String clip(String s, int maxLen) {
        if (s == null) return null;
        String text = s.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen).trim() + "…";
    }
}
