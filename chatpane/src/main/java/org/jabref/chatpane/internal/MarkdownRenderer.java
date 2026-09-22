package org.jabref.chatpane.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.Nullable;

import org.jabref.chatpane.MessageRenderer;
import org.jabref.chatpane.TextLine;
import org.jabref.chatpane.TextLine.Kind;
import org.jabref.chatpane.TextSpan;
import org.jabref.chatpane.TextSpan.Style;

/// [MessageRenderer#markdown()]: CommonMark (commonmark-java) plus strikethrough, into lines.
///
/// The coverage follows JabRef's `MarkdownTextFlow` (MIT; `jabgui/…/gui/util/component/`, JabRef
/// `b91795ad59`), whose flexmark visitor this re-cuts for commonmark and for lines instead of
/// `Text` nodes: headings, emphasis, inline and block code, links, `•` bullets and numbered lists
/// with nesting, block quotes, HTML as literal text, a little space between blocks. Unlike
/// CommonMark (and like chat clients), a single line break stays a line break.
// [impl->dsn~message-renderers~1]
public final class MarkdownRenderer implements MessageRenderer {

    public static final MarkdownRenderer INSTANCE = new MarkdownRenderer();

    private static final String BULLET = "• ";
    private static final String RULE = "―".repeat(12);

    /// Thread-safe, per commonmark-java.
    private final Parser parser = Parser.builder().extensions(List.of(StrikethroughExtension.create())).build();

    private MarkdownRenderer() {
    }

    @Override
    public List<TextLine> render(String text) {
        Lines lines = new Lines();
        parser.parse(text).accept(lines);
        return lines.finish();
    }

    /// Walks the syntax tree and collects lines; one instance per render.
    private static final class Lines extends AbstractVisitor {

        /// A list being walked: `-1` for bullets, else the next number.
        private static final int BULLETS = -1;

        private final List<TextLine> lines = new ArrayList<>();
        private final Map<Style, Integer> styles = new EnumMap<>(Style.class);
        private final Deque<Integer> lists = new ArrayDeque<>();
        private @Nullable String link;
        private int quoteDepth;
        /// The marker the next line starts with: an item's bullet or number.
        private @Nullable String marker;
        /// A new block started; its first line gets space above (unless it is the very first).
        private boolean blockPending;

        private @Nullable List<TextSpan> spans;
        private Kind kind = Kind.PARAGRAPH;
        private int level;
        private boolean startsBlock;

        List<TextLine> finish() {
            endLine();
            return lines.isEmpty() ? List.of(TextLine.plain("")) : List.copyOf(lines);
        }

        // --- blocks

        @Override
        public void visit(Paragraph paragraph) {
            if (!(paragraph.getParent() instanceof ListItem)) {
                blockPending = true;
            }
            startLine(contextKind(), contextLevel());
            visitChildren(paragraph);
            endLine();
        }

        @Override
        public void visit(Heading heading) {
            blockPending = true;
            startLine(Kind.HEADING, heading.getLevel());
            visitChildren(heading);
            endLine();
        }

        @Override
        public void visit(FencedCodeBlock code) {
            codeLines(code.getLiteral());
        }

        @Override
        public void visit(IndentedCodeBlock code) {
            codeLines(code.getLiteral());
        }

        private void codeLines(String literal) {
            blockPending = true;
            String body = literal.endsWith("\n") ? literal.substring(0, literal.length() - 1) : literal;
            for (String line : body.split("\n", -1)) {
                startLine(Kind.CODE_BLOCK, 0);
                styled(Style.CODE, () -> add(line));
                endLine();
            }
        }

        @Override
        public void visit(BlockQuote quote) {
            blockPending = true;
            quoteDepth++;
            visitChildren(quote);
            quoteDepth--;
        }

        @Override
        public void visit(BulletList list) {
            listOf(BULLETS, list);
        }

        @Override
        public void visit(OrderedList list) {
            Integer start = list.getMarkerStartNumber();
            listOf(start == null ? 1 : start, list);
        }

        private void listOf(int first, Node list) {
            if (lists.isEmpty()) {
                blockPending = true;
            }
            lists.push(first);
            visitChildren(list);
            lists.pop();
        }

        @Override
        public void visit(ListItem item) {
            int next = lists.pop();
            marker = next == BULLETS ? BULLET : next + ". ";
            lists.push(next == BULLETS ? BULLETS : next + 1);
            visitChildren(item);
            if (marker != null) {
                // An empty item still shows its marker.
                startLine(Kind.LIST_ITEM, lists.size());
                endLine();
            }
        }

        @Override
        public void visit(ThematicBreak rule) {
            blockPending = true;
            startLine(Kind.PARAGRAPH, 0);
            add(RULE);
            endLine();
        }

        @Override
        public void visit(HtmlBlock html) {
            blockPending = true;
            for (String line : html.getLiteral().strip().split("\\R", -1)) {
                startLine(contextKind(), contextLevel());
                add(line);
                endLine();
            }
        }

        // --- inlines

        @Override
        public void visit(Text text) {
            add(text.getLiteral());
        }

        @Override
        public void visit(Code code) {
            styled(Style.CODE, () -> add(code.getLiteral()));
        }

        @Override
        public void visit(Emphasis emphasis) {
            styled(Style.ITALIC, () -> visitChildren(emphasis));
        }

        @Override
        public void visit(StrongEmphasis strong) {
            styled(Style.BOLD, () -> visitChildren(strong));
        }

        @Override
        public void visit(CustomNode node) {
            if (node instanceof Strikethrough) {
                styled(Style.STRIKETHROUGH, () -> visitChildren(node));
            } else {
                visitChildren(node);
            }
        }

        @Override
        public void visit(Link node) {
            linked(node.getDestination(), () -> visitChildren(node));
        }

        @Override
        public void visit(Image image) {
            linked(image.getDestination(), () -> {
                add("[");
                visitChildren(image);
                add("]");
            });
        }

        @Override
        public void visit(HtmlInline html) {
            add(html.getLiteral());
        }

        @Override
        public void visit(SoftLineBreak lineBreak) {
            continueOnNextLine();
        }

        @Override
        public void visit(HardLineBreak lineBreak) {
            continueOnNextLine();
        }

        // --- line building

        private Kind contextKind() {
            if (!lists.isEmpty()) {
                return Kind.LIST_ITEM;
            }
            return quoteDepth > 0 ? Kind.QUOTE : Kind.PARAGRAPH;
        }

        private int contextLevel() {
            return !lists.isEmpty() ? lists.size() : quoteDepth;
        }

        private void startLine(Kind lineKind, int lineLevel) {
            endLine();
            spans = new ArrayList<>();
            kind = lineKind;
            level = lineLevel;
            startsBlock = blockPending && !lines.isEmpty();
            blockPending = false;
            if (marker != null) {
                spans.add(TextSpan.plain(marker));
                marker = null;
            }
        }

        private void continueOnNextLine() {
            Kind lineKind = kind;
            int lineLevel = level;
            endLine();
            startLine(lineKind, lineLevel);
        }

        private void endLine() {
            if (spans != null) {
                lines.add(new TextLine(kind, level, startsBlock, spans));
                spans = null;
            }
        }

        private void add(String text) {
            if (text.isEmpty()) {
                return;
            }
            if (spans == null) {
                startLine(contextKind(), contextLevel());
            }
            spans.add(new TextSpan(text, Set.copyOf(styles.keySet()), link));
        }

        private void styled(Style style, Runnable inside) {
            styles.merge(style, 1, Integer::sum);
            inside.run();
            styles.computeIfPresent(style, (_, depth) -> depth == 1 ? null : depth - 1);
        }

        private void linked(String destination, Runnable inside) {
            String outer = link;
            link = destination;
            inside.run();
            link = outer;
        }
    }
}
