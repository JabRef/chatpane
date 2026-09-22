package org.jabref.chatpane.skin;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.jabref.chatpane.ChatMessage;

import static org.jabref.chatpane.ChatMessage.Direction.INCOMING;
import static org.jabref.chatpane.ChatMessage.Direction.OUTGOING;
import static org.assertj.core.api.Assertions.assertThat;

// [utest->dsn~message-grouping~1]
class MessageGroupingTest {

    private static final Instant T0 = Instant.parse("2026-09-22T10:00:00Z");

    private static ChatMessage incoming(String sender, Instant sentAt) {
        return new ChatMessage(sender, "text", sentAt, INCOMING);
    }

    @Test
    void firstMessageStartsAGroup() {
        assertThat(MessageGrouping.continuesGroup(null, incoming("alice", T0))).isFalse();
    }

    @Test
    void sameSenderWithinTheGapContinues() {
        assertThat(MessageGrouping.continuesGroup(incoming("alice", T0), incoming("alice", T0.plusSeconds(30)))).isTrue();
        assertThat(MessageGrouping.continuesGroup(incoming("alice", T0), incoming("alice", T0.plus(MessageGrouping.MAX_GAP)))).isTrue();
    }

    @Test
    void longerGapStartsAGroup() {
        assertThat(MessageGrouping.continuesGroup(incoming("alice", T0),
                incoming("alice", T0.plus(MessageGrouping.MAX_GAP).plusSeconds(1)))).isFalse();
    }

    @Test
    void otherSenderStartsAGroup() {
        assertThat(MessageGrouping.continuesGroup(incoming("alice", T0), incoming("bob", T0.plusSeconds(1)))).isFalse();
    }

    @Test
    void otherDirectionStartsAGroup() {
        ChatMessage outgoing = new ChatMessage("alice", "text", T0.plusSeconds(1), OUTGOING);
        assertThat(MessageGrouping.continuesGroup(incoming("alice", T0), outgoing)).isFalse();
    }

    @Test
    void messageOlderThanItsPredecessorStartsAGroup() {
        assertThat(MessageGrouping.continuesGroup(incoming("alice", T0), incoming("alice", T0.minusSeconds(1)))).isFalse();
    }
}
