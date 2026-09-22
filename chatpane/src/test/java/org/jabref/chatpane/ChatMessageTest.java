package org.jabref.chatpane;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

// [utest->dsn~chat-message-model~3]
class ChatMessageTest {

    @Test
    @SuppressWarnings("DataFlowIssue")
    void rejectsMissingParts() {
        Instant now = Instant.now();
        assertThatNullPointerException().isThrownBy(() -> new ChatMessage(null, "text", now, INCOMING)).withMessage("sender");
        assertThatNullPointerException().isThrownBy(() -> new ChatMessage("alice", null, now, INCOMING)).withMessage("text");
        assertThatNullPointerException().isThrownBy(() -> new ChatMessage("alice", "text", null, INCOMING)).withMessage("sentAt");
        assertThatNullPointerException().isThrownBy(() -> new ChatMessage("alice", "text", now, null)).withMessage("direction");
        assertThatNullPointerException().isThrownBy(() -> new ChatMessage("alice", "text", now, INCOMING, null)).withMessage("status");
    }

    @Test
    void isSentUnlessToldOtherwiseAndChangesByCopy() {
        ChatMessage message = new ChatMessage("ai", "The", Instant.EPOCH, INCOMING);
        assertThat(message.status()).isEqualTo(ChatMessage.Status.SENT);
        ChatMessage grown = message.withStatus(ChatMessage.Status.PENDING).withText("The answer");
        assertThat(grown).isEqualTo(new ChatMessage("ai", "The answer", Instant.EPOCH, INCOMING, ChatMessage.Status.PENDING));
        assertThat(message.text()).as("unchanged").isEqualTo("The");
    }
}
