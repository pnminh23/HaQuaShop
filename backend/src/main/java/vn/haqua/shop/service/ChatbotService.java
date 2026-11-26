package vn.haqua.shop.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import vn.haqua.shop.client.LlmClient;
import vn.haqua.shop.dto.ChatDtos.*;
import vn.haqua.shop.dto.OrderDtos.OrderTrackingDto;
import vn.haqua.shop.entity.ChatConversation;
import vn.haqua.shop.entity.ChatMessage;
import vn.haqua.shop.entity.ChatbotConfig;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.enums.ChatEnums.*;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.mapper.ProductMapper;
import vn.haqua.shop.repository.ChatConversationRepository;
import vn.haqua.shop.repository.ChatMessageRepository;
import vn.haqua.shop.repository.ChatbotConfigRepository;
import vn.haqua.shop.repository.KnowledgeSourceRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.repository.UserRepository;
import vn.haqua.shop.service.chat.ReplyGuide;
import vn.haqua.shop.util.SlugUtil;

/**
 * UC11 – chat với trợ lý AI (phía khách) và UC16/UC17 (nhân viên, cấu hình).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final Pattern ORDER_CODE = Pattern.compile("HQ-\\d{6}-[A-Z0-9]{4}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("0\\d{9}");
    private static final String DISCLAIMER = "AI có thể nhầm lẫn, hãy kiểm tra lại thông tin quan trọng.";
    private static final String SORRY = "Xin lỗi, hiện mình chưa trả lời được. Bạn bấm \"Gặp nhân viên\" để được hỗ trợ trực tiếp nhé.";

    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ChatbotConfigRepository configs;
    private final KnowledgeSourceRepository sources;
    private final KnowledgeRetriever retriever;
    private final LlmClient llm;
    private final OrderService orderService;
    private final ProductRepository products;
    private final ProductMapper productMapper;
    private final UserRepository users;
    private final TransactionTemplate tx;

    // ============================================================ phía khách

    @Transactional
    public ChatState open(String token, Long userId) {
        ChatConversation c = conversation(token, userId);
        return state(c);
    }

    /**
     * Không bọc cả hàm trong một transaction: lời gọi LLM đi qua mạng (có thể vài giây) nên không được giữ kết nối DB.
     * Chia 3 bước: lưu tin + chuẩn bị ngữ cảnh (tx) → gọi LLM (không tx) → lưu câu trả lời (tx).
     */
    public ChatState send(SendRequest req, Long userId) {
        Prepared prepared = tx.execute(status -> prepare(req, userId));
        if (prepared.done() != null) {
            return prepared.done();
        }
        String answer = askLlm(prepared);
        return tx.execute(status -> finish(prepared, answer));
    }

    private record Prepared(ChatState done, Long conversationId, String systemPrompt,
                            List<LlmClient.Message> history, String productRefs) {
        static Prepared answered(ChatState state) {
            return new Prepared(state, null, null, null, null);
        }
    }

    private Prepared prepare(SendRequest req, Long userId) {
        ChatbotConfig cfg = config();
        ChatConversation c = conversation(req.sessionToken(), userId);
        if (c.getStatus() == ConversationStatus.CLOSED) {
            c.setStatus(ConversationStatus.AI);
            c.setTag("AI xử lý");
        }
        save(c, Sender.USER, req.content(), null);

        if (!cfg.isEnabled()) {
            save(c, Sender.BOT, "Trợ lý AI đang tạm tắt. Bạn để lại số điện thoại, nhân viên sẽ gọi lại sớm nhất.",
                    null);
            return Prepared.answered(state(c));
        }
        if (c.getStatus() == ConversationStatus.STAFF || c.getStatus() == ConversationStatus.WAITING_STAFF) {
            return Prepared.answered(state(c)); // nhân viên đang xử lý: AI im lặng
        }
        if (wantsHuman(req.content()) || matchesHandoffRule(cfg, req.content())) {
            handoff(c);
            save(c, Sender.BOT, "Mình đã chuyển cuộc trò chuyện cho nhân viên, bạn vui lòng chờ trong giây lát nhé.",
                    null);
            return Prepared.answered(state(c));
        }
        String orderReply = tryOrderLookup(req.content());
        if (orderReply != null) {
            save(c, Sender.BOT, orderReply, null);
            return Prepared.answered(state(c));
        }

        List<ChatMessage> past = messages.findByConversationIdOrderByIdAsc(c.getId());
        // tìm sản phẩm theo cả mạch hỏi, để "dưới 30 triệu thôi" vẫn nhớ là đang hỏi laptop đồ họa
        List<String> asked = past.stream().filter(m -> m.getSender() == Sender.USER).map(ChatMessage::getContent).toList();
        var retrieved = retriever.retrieve(asked.subList(Math.max(0, asked.size() - 8), asked.size()));
        List<LlmClient.Message> history = past.stream()
                .map(m -> new LlmClient.Message(m.getSender() == Sender.USER ? "user" : "assistant", m.getContent()))
                .toList();
        String refs = retrieved.products().stream().map(p -> String.valueOf(p.getId()))
                .reduce((a, b) -> a + "," + b).orElse(null);
        return new Prepared(null, c.getId(), systemPrompt(cfg) + "\nNGỮ CẢNH:\n" + retrieved.context(),
                ReplyGuide.pin(history, retrieved.products().stream().map(Product::getName).toList()), refs);
    }

    private String askLlm(Prepared p) {
        try {
            return llm.complete(p.systemPrompt(), p.history());
        } catch (RuntimeException e) {
            log.warn("LLM lỗi hoặc quá thời gian: {}", e.toString());
            return SORRY;
        }
    }

    private ChatState finish(Prepared p, String answer) {
        ChatConversation c = byId(p.conversationId());
        save(c, Sender.BOT, answer + "\n\n" + DISCLAIMER, p.productRefs());
        return state(c);
    }

    @Transactional(readOnly = true)
    public ChatState poll(String token, Long afterId) {
        ChatConversation c = conversations.findBySessionToken(token)
                .orElseThrow(() -> ApiException.notFound("Không thấy hội thoại"));
        ChatState full = state(c);
        return new ChatState(full.sessionToken(), full.status(), full.botEnabled(), full.botName(),
                full.messages().stream().filter(m -> m.id() > afterId).toList());
    }

    @Transactional
    public void requestStaff(String token) {
        ChatConversation c = conversations.findBySessionToken(token)
                .orElseThrow(() -> ApiException.notFound("Không thấy hội thoại"));
        handoff(c);
        save(c, Sender.BOT, "Mình đã chuyển cuộc trò chuyện cho nhân viên, bạn vui lòng chờ trong giây lát nhé.", null);
    }

    @Transactional
    public void rate(String token, int score) {
        ChatConversation c = conversations.findBySessionToken(token)
                .orElseThrow(() -> ApiException.notFound("Không thấy hội thoại"));
        c.setCsat(score);
    }

    // ============================================================ nhân viên (UC16)

    @Transactional(readOnly = true)
    public List<ConversationRow> adminList() {
        return conversations.findAllByOrderByLastMessageAtDesc(org.springframework.data.domain.PageRequest.of(0, 50))
                .stream().map(c -> {
                    var last = messages.findByConversationIdOrderByIdAsc(c.getId());
                    return new ConversationRow(c.getId(),
                            c.getUser() == null ? "Khách vãng lai" : c.getUser().getFullName(), c.getStatus(),
                            c.getTag(),
                            last.isEmpty() ? "" : last.get(last.size() - 1).getContent(), c.getLastMessageAt(),
                            c.getAssignedStaff() == null ? null : c.getAssignedStaff().getFullName());
                }).toList();
    }

    @Transactional(readOnly = true)
    public ChatStats stats(long ordersFromChatbotToday) {
        Instant start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        long total = conversations.countSince(start);
        long aiOnly = conversations.countSinceWithStatus(start,
                List.of(ConversationStatus.AI, ConversationStatus.CLOSED));
        Double csat = conversations.averageCsat();
        return new ChatStats(total, total == 0 ? 0 : (int) Math.round(aiOnly * 100.0 / total), ordersFromChatbotToday,
                csat == null ? 0 : Math.round(csat * 10) / 10.0);
    }

    @Transactional(readOnly = true)
    public ConversationDetail adminGet(Long id) {
        ChatConversation c = byId(id);
        return new ConversationDetail(c.getId(), c.getStatus(), c.getTag(),
                c.getAssignedStaff() == null ? null : c.getAssignedStaff().getFullName(), dtos(c));
    }

    @Transactional
    public ConversationDetail takeover(Long id, Long staffId) {
        ChatConversation c = byId(id);
        if (c.getStatus() == ConversationStatus.STAFF && c.getAssignedStaff() != null
                && !c.getAssignedStaff().getId().equals(staffId)) {
            throw ApiException
                    .conflict("Nhân viên " + c.getAssignedStaff().getFullName() + " đang xử lý hội thoại này");
        }
        c.setStatus(ConversationStatus.STAFF);
        c.setAssignedStaff(users.getReferenceById(staffId));
        c.setTag("Nhân viên xử lý");
        return adminGet(id);
    }

    @Transactional
    public ConversationDetail staffReply(Long id, Long staffId, String content) {
        ChatConversation c = byId(id);
        if (c.getStatus() != ConversationStatus.STAFF || c.getAssignedStaff() == null
                || !c.getAssignedStaff().getId().equals(staffId)) {
            throw ApiException.forbidden("Hãy tiếp quản hội thoại trước khi trả lời");
        }
        save(c, Sender.STAFF, content, null);
        return adminGet(id);
    }

    @Transactional
    public ConversationDetail giveBackToAi(Long id) {
        ChatConversation c = byId(id);
        c.setStatus(ConversationStatus.AI);
        c.setAssignedStaff(null);
        c.setTag("AI xử lý");
        return adminGet(id);
    }

    @Transactional
    public ConversationDetail close(Long id) {
        ChatConversation c = byId(id);
        c.setStatus(ConversationStatus.CLOSED);
        c.setTag("Đã chốt đơn".equals(c.getTag()) ? c.getTag() : "Đã đóng");
        return adminGet(id);
    }

    // ============================================================ cấu hình (UC17)

    @Transactional(readOnly = true)
    public ConfigDto getConfig() {
        ChatbotConfig c = config();
        return new ConfigDto(c.getDisplayName(), c.getTone(), c.getHandoffRule(), c.getExtraInstructions(),
                c.isEnabled(),
                sources.findAll().stream()
                        .map(s -> new KnowledgeDto(s.getId(), s.getType(), s.getName(), s.isEnabled())).toList());
    }

    @Transactional
    public ConfigDto saveConfig(ConfigDto req) {
        ChatbotConfig c = config();
        c.setDisplayName(req.displayName());
        c.setTone(req.tone());
        c.setHandoffRule(req.handoffRule());
        c.setExtraInstructions(req.extraInstructions());
        c.setEnabled(req.enabled());
        if (req.sources() != null) {
            req.sources().forEach(s -> sources.findById(s.id()).ifPresent(k -> k.setEnabled(s.enabled())));
        }
        return getConfig();
    }

    // ============================================================ nội bộ

    private ChatbotConfig config() {
        return configs.findAll().stream().findFirst().orElseGet(() -> configs.save(new ChatbotConfig()));
    }

    private ChatConversation conversation(String token, Long userId) {
        if (token != null && !token.isBlank()) {
            var existing = conversations.findBySessionToken(token);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        ChatConversation c = new ChatConversation();
        c.setSessionToken(token == null || token.isBlank() ? UUID.randomUUID().toString() : token);
        if (userId != null) {
            c.setUser(users.getReferenceById(userId));
        }
        c = conversations.save(c);
        save(c, Sender.BOT, "Xin chào! Mình là " + config().getDisplayName()
                + ". Bạn cần tìm laptop, điện thoại hay tai nghe? Hãy cho mình biết nhu cầu và ngân sách nhé.", null);
        return c;
    }

    private ChatConversation byId(Long id) {
        return conversations.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy hội thoại"));
    }

    private ChatMessage save(ChatConversation c, Sender sender, String content, String refs) {
        c.setLastMessageAt(Instant.now());
        return messages.save(new ChatMessage(c, sender, content, refs));
    }

    private void handoff(ChatConversation c) {
        c.setStatus(ConversationStatus.WAITING_STAFF);
        c.setTag("Cần nhân viên");
    }

    private boolean wantsHuman(String text) {
        String t = SlugUtil.unaccent(text);
        return t.contains("gap nhan vien") || t.contains("nguoi that") || t.contains("tu van vien");
    }

    private boolean matchesHandoffRule(ChatbotConfig cfg, String text) {
        // ORDER_OVER_30M: khách nhắc tới giá trị đơn từ 30 triệu trở lên (ví dụ "35
        // triệu")
        if (cfg.getHandoffRule() == HandoffRule.ORDER_OVER_30M) {
            Matcher m = Pattern.compile("(\\d{2,3})\\s*(tr|trieu)").matcher(SlugUtil.unaccent(text));
            return m.find() && Integer.parseInt(m.group(1)) >= 30;
        }
        return false;
    }

    /** Khách hỏi về đơn: cần cả mã đơn và SĐT khớp mới trả trạng thái. */
    private String tryOrderLookup(String text) {
        String t = SlugUtil.unaccent(text);
        Matcher code = ORDER_CODE.matcher(text);
        Matcher phone = PHONE.matcher(text);
        boolean asking = t.contains("don hang") || t.contains("tra cuu") || code.find(0);
        if (!asking || sources.findByTypeAndEnabledTrue(KnowledgeType.ORDER_LOOKUP).isEmpty()) {
            return null;
        }
        if (!code.find(0) || !phone.find()) {
            return "Để tra cứu đơn hàng, bạn gửi giúp mình mã đơn (dạng HQ-251120-AB12) và số điện thoại đặt hàng nhé.";
        }
        try {
            OrderTrackingDto o = orderService.lookup(code.group().toUpperCase(), phone.group());
            return "Đơn " + o.code() + " đang ở trạng thái " + o.status() + ", thanh toán: " + o.paymentStatus()
                    + ", tổng " + String.format("%,d", o.total()).replace(',', '.') + "₫.";
        } catch (ApiException e) {
            return "Không tìm thấy đơn hàng khớp mã và số điện thoại này.";
        }
    }

    private String systemPrompt(ChatbotConfig cfg) {
        String tone = switch (cfg.getTone()) {
            case FRIENDLY -> "Giọng thân thiện, gần gũi.";
            case PROFESSIONAL -> "Giọng chuyên nghiệp, lịch sự.";
            case SHORT -> "Trả lời ngắn gọn, đi thẳng vào ý.";
        };
        return "Bạn là " + cfg.getDisplayName() + ", trợ lý bán hàng của HaQua (laptop, điện thoại, tai nghe). " + tone
                + " Chỉ dựa vào NGỮ CẢNH được cung cấp, không bịa giá, thông số hoặc chính sách. "
                + "Danh sách sản phẩm trong NGỮ CẢNH chỉ là phần lọc theo câu hỏi, không phải toàn bộ kho: "
                + "không được nói shop chỉ bán hay chỉ có thông tin về một loại hàng, kể cả khi câu trả lời trước đó từng nói vậy. "
                + "Khi khách hỏi về sản phẩm và danh sách có sản phẩm thì phải giới thiệu ngay, không hỏi lại trước. "
                + "Giới thiệu đúng 3 sản phẩm đầu danh sách (ít hơn nếu danh sách ít hơn), giữ nguyên thứ tự, không bao giờ quá 3. "
                + "Mỗi sản phẩm nêu giá và lý do hợp nhu cầu; chép nguyên văn tên thông số, không tự sửa hay bổ sung; "
                + "thông số không có thì nói là chưa có thông tin. "
                + "Khung chat hẹp: trình bày bằng danh sách ngắn, không dùng bảng. "
                + "Nếu khách chưa nói ngân sách hoặc nhu cầu thì sau khi giới thiệu mới hỏi thêm một câu ngắn. "
                + (cfg.getExtraInstructions() == null ? "" : cfg.getExtraInstructions());
    }

    private ChatState state(ChatConversation c) {
        ChatbotConfig cfg = config();
        return new ChatState(c.getSessionToken(), c.getStatus(), cfg.isEnabled(), cfg.getDisplayName(), dtos(c));
    }

    private List<MessageDto> dtos(ChatConversation c) {
        List<MessageDto> out = new ArrayList<>();
        for (ChatMessage m : messages.findByConversationIdOrderByIdAsc(c.getId())) {
            List<vn.haqua.shop.dto.CatalogDtos.ProductCardDto> refs = m.getProductRefs() == null ? List.of()
                    : java.util.Arrays.stream(m.getProductRefs().split(",")).map(Long::valueOf)
                            .map(products::findById).flatMap(java.util.Optional::stream).map(productMapper::toCard)
                            .toList();
            out.add(new MessageDto(m.getId(), m.getSender(), m.getContent(), refs, m.getCreatedAt()));
        }
        return out;

    }
}
