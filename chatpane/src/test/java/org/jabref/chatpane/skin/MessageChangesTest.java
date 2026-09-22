package org.jabref.chatpane.skin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// [utest->dsn~message-changes~2]
class MessageChangesTest {

    /// What [MessageChanges#appendedFrom] says about the change `edit` makes to `a, b, c`.
    private static int classify(Consumer<ObservableList<String>> edit) {
        return classify(edit, MessageChanges::appendedFrom);
    }

    private static int classify(Consumer<ObservableList<String>> edit, ToIntFunction<ListChangeListener.Change<?>> classifier) {
        ObservableList<String> list = FXCollections.observableArrayList("a", "b", "c");
        List<Integer> results = new ArrayList<>();
        list.addListener((ListChangeListener<String>) change -> results.add(classifier.applyAsInt(change)));
        edit.accept(list);
        assertThat(results).as("one change event").hasSize(1);
        return results.getFirst();
    }

    @Test
    void appendsAreFromTheOldEnd() {
        assertThat(classify(list -> list.add("d"))).isEqualTo(3);
        assertThat(classify(list -> list.addAll("d", "e"))).isEqualTo(3);
    }

    @Test
    void anythingElseIsNotAnAppend() {
        assertThat(classify(list -> list.addFirst("z"))).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> list.add(1, "z"))).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> list.removeLast())).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> list.set(2, "z"))).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> list.setAll("x", "y", "z", "w"))).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> FXCollections.reverse(list))).isEqualTo(MessageChanges.NOT_AN_APPEND);
    }

    @Test
    void oneReplacedMessageIsFoundByIndex() {
        assertThat(classify(list -> list.set(1, "B"), MessageChanges::replacedAt)).isEqualTo(1);
        assertThat(classify(list -> list.set(2, "C"), MessageChanges::replacedAt)).isEqualTo(2);
        assertThat(classify(list -> list.add("d"), MessageChanges::replacedAt)).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> list.remove(1), MessageChanges::replacedAt)).isEqualTo(MessageChanges.NOT_AN_APPEND);
        assertThat(classify(list -> list.setAll("x", "y", "z"), MessageChanges::replacedAt)).isEqualTo(MessageChanges.NOT_AN_APPEND);
    }

    @Test
    void leavesTheChangeReadableForOtherListeners() {
        ObservableList<String> list = FXCollections.observableArrayList("a");
        List<String> seenByTheNext = new ArrayList<>();
        list.addListener((ListChangeListener<String>) change -> MessageChanges.appendedFrom(change));
        list.addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                seenByTheNext.addAll(change.getAddedSubList());
            }
        });
        list.add("b");
        assertThat(seenByTheNext).containsExactly("b");
    }
}
