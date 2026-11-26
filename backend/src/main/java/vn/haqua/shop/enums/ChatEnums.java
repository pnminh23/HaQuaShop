package vn.haqua.shop.enums;

public final class ChatEnums {

    private ChatEnums() {
    }

    public enum ConversationStatus { AI, WAITING_STAFF, STAFF, CLOSED }

    public enum Sender { USER, BOT, STAFF }

    public enum Tone { FRIENDLY, PROFESSIONAL, SHORT }

    public enum HandoffRule { ASK_OR_UNSURE, ASK_ONLY, ORDER_OVER_30M }

    public enum KnowledgeType { CATALOG, POLICY, FAQ, ORDER_LOOKUP }
}
