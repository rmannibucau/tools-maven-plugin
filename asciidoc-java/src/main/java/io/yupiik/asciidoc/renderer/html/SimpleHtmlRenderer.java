/*
 * Copyright (c) 2020 - 2023 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.asciidoc.renderer.html;

import io.yupiik.asciidoc.model.Admonition;
import io.yupiik.asciidoc.model.Anchor;
import io.yupiik.asciidoc.model.Code;
import io.yupiik.asciidoc.model.ConditionalBlock;
import io.yupiik.asciidoc.model.DescriptionList;
import io.yupiik.asciidoc.model.Document;
import io.yupiik.asciidoc.model.Element;
import io.yupiik.asciidoc.model.Header;
import io.yupiik.asciidoc.model.LineBreak;
import io.yupiik.asciidoc.model.Link;
import io.yupiik.asciidoc.model.Macro;
import io.yupiik.asciidoc.model.OpenBlock;
import io.yupiik.asciidoc.model.OrderedList;
import io.yupiik.asciidoc.model.PageBreak;
import io.yupiik.asciidoc.model.Paragraph;
import io.yupiik.asciidoc.model.PassthroughBlock;
import io.yupiik.asciidoc.model.Quote;
import io.yupiik.asciidoc.model.Section;
import io.yupiik.asciidoc.model.Table;
import io.yupiik.asciidoc.model.Text;
import io.yupiik.asciidoc.model.UnOrderedList;
import io.yupiik.asciidoc.renderer.Visitor;
import io.yupiik.asciidoc.renderer.uri.DataResolver;

import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Important: as of today it is a highly incomplete implementation but it gives a starting point.
 * <p>
 * Trivial document renderer as HTML.
 */
public class SimpleHtmlRenderer implements Visitor<String> {
    private final StringBuilder builder = new StringBuilder();
    private final Configuration configuration;
    private final boolean dataUri;
    private final DataResolver resolver;

    public SimpleHtmlRenderer() {
        this(new Configuration().setAttributes(Map.of()));
    }

    public SimpleHtmlRenderer(final Configuration configuration) {
        this.configuration = configuration;

        final var dataUriValue = configuration.getAttributes().getOrDefault("data-uri", "false");
        this.dataUri = Boolean.parseBoolean(dataUriValue) || dataUriValue.isBlank();
        this.resolver = dataUri ?
                (configuration.getResolver() == null ? new DataResolver(configuration.getAssetsBase()) : configuration.getResolver()) :
                null;
    }

    @Override
    public void visit(final Document document) {
        final boolean contentOnly = Boolean.parseBoolean(configuration.getAttributes().getOrDefault("noheader", "false"));
        if (!contentOnly) {
            final var attributes = document.header().attributes();

            builder.append("<!DOCTYPE html>\n");
            builder.append("<html");
            if (attr("nolang", attributes) == null) {
                final var lang = attr("lang", attributes);
                builder.append(" lang=\"").append(lang == null ? "en" : lang).append('"');
            }
            builder.append(">\n");
            builder.append("<head>\n");

            final var encoding = attr("encoding", attributes);
            builder.append(" <meta charset=\"").append(encoding == null ? "UTF-8" : encoding).append("\">\n");

            builder.append(" <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");

            final var appName = attr("app-name", attributes);
            if (appName != null) {
                builder.append(" <meta name=\"application-name\" content=\"").append(appName).append("\">\n");
            }
            final var description = attr("description", attributes);
            if (description != null) {
                builder.append(" <meta name=\"description\" content=\"").append(description).append("\">\n");
            }
            final var keywords = attr("keywords", attributes);
            if (keywords != null) {
                builder.append(" <meta name=\"keywords\" content=\"").append(keywords).append("\">\n");
            }
            final var author = attr("author", attributes);
            if (author != null) {
                builder.append(" <meta name=\"author\" content=\"").append(author).append("\">\n");
            }
            final var copyright = attr("copyright", attributes);
            if (copyright != null) {
                builder.append(" <meta name=\"copyright\" content=\"").append(copyright).append("\">\n");
            }

            if (attributes.containsKey("asciidoctor-css")) {
                builder.append(" <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/asciidoctor.js/1.5.9/css/asciidoctor.min.css\" integrity=\"sha512-lb4ZuGfCVoGO2zu/TMakNlBgRA6mPXZ0RamTYgluFxULAwOoNnBIZaNjsdfhnlKlIbENaQbEAYEWxtzjkB8wsQ==\" crossorigin=\"anonymous\" referrerpolicy=\"no-referrer\" />\n");
            }
            Stream.of(attributes.getOrDefault("custom-css", "").split(","))
                    .map(String::strip)
                    .filter(Predicate.not(String::isBlank))
                    .map(i -> " " + i + '\n')
                    .forEach(builder::append);

            // todo: favicon, highlighter, toc support etc...
            builder.append("</head>\n");

            builder.append("<body>\n");

            builder.append(" <div id=\"content\">\n");
        }
        Visitor.super.visit(document);
        if (!contentOnly) {
            builder.append(" </div>\n");

            builder.append("</body>\n");

            Stream.of(document.header().attributes().getOrDefault("custom-js", "").split(","))
                    .map(String::strip)
                    .filter(Predicate.not(String::isBlank))
                    .map(i -> " " + i + '\n')
                    .forEach(builder::append);

            builder.append("</html>\n");
        }
    }

    @Override
    public void visitAdmonition(final Admonition element) {
        // todo: here we need to assume we have icons to render it more elegantly
        builder.append(" <div class=\"admonitionblock ").append(element.level().name().toLowerCase(ROOT)).append("\">\n");
        builder.append("""
                          <table>
                           <tr>
                            <td class="icon">
                        """)
                .append(element.level().name()).append(":").append("\n")
                .append("     </td>\n")
                .append("    <td class=\"content\">\n");
        visitElement(element.content());
        builder.append("    </td>\n")
                .append("   </tr>\n")
                .append("  </table>\n")
                .append(" </div>\n");
    }

    @Override
    public void visitParagraph(final Paragraph element) {
        builder.append(" <div class=\"paragraph\">");
        Visitor.super.visitParagraph(element);
        builder.append(" </div>");
    }

    @Override
    public void visitHeader(final Header header) {
        if (header.attributes().get("notitle") == null && !header.title().isBlank()) {
            builder.append(" <h1>").append(escape(header.title())).append("</h1>\n");
        }

        final var details = new StringBuilder();
        {
            int authorIdx = 1;
            final var mails = header.author().name().split(",");
            for (final var name : header.author().name().split(",")) {
                if (name.isBlank()) {
                    continue;
                }
                details.append("<span class=\"author author-").append(authorIdx).append("\">").append(escape(name)).append("</span>\n");

                final var mail = mails.length > (authorIdx - 1) ? mails[authorIdx - 1] : null;
                if (mail != null) {
                    details.append("<span class=\"email email-").append(authorIdx++).append("\">").append(escape(mail)).append("</span>\n");
                }
                authorIdx++;
            }
        }
        if (!header.revision().number().isBlank()) {
            details.append("<span id=\"revnumber\">").append(escape(header.revision().number())).append("</span>\n");
        }
        if (!header.revision().date().isBlank()) {
            details.append("<span id=\"revdate\">").append(escape(header.revision().date())).append("</span>\n");
        }
        if (!header.revision().revmark().isBlank()) {
            details.append("<span id=\"revremark\">").append(escape(header.revision().revmark())).append("</span>\n");
        }
        if (!details.isEmpty()) {
            builder.append("  <div class=\"details\">\n").append(details.toString().indent(3)).append("  </div>\n");
        }
    }

    @Override
    public void visitSection(final Section element) {
        builder.append(" <div>\n");
        builder.append("  <h").append(element.level());
        writeCommonAttributes(element.options(), null);
        builder.append(">");
        final var titleRenderer = new SimpleHtmlRenderer(configuration);
        titleRenderer.visitElement(element.title() instanceof Text t && t.options().isEmpty() && t.style().isEmpty() ?
                new Text(t.style(), t.value(), Map.of("nowrap", "")) :
                element);
        builder.append(titleRenderer.result());
        builder.append("</h").append(element.level()).append(">\n");
        Visitor.super.visitSection(element);
        builder.append(" </div>\n");
    }

    @Override
    public void visitLineBreak(final LineBreak element) {
        builder.append(" <br>\n");
    }

    @Override
    public void visitPageBreak(final PageBreak element) {
        builder.append(" <div class=\"page-break\"></div>\n");
    }

    @Override
    public void visitLink(final Link element) {
        builder.append(" <a href=\"").append(element.url()).append("\"");

        final var window = element.options().get("window");
        if (window != null) {
            builder.append(" target=\"").append(window).append("\"");
        }

        final var nofollow = element.options().get("nofollow");
        final boolean noopener = "_blank".equals(window) || element.options().get("noopener") != null;
        if (nofollow != null) {
            builder.append(" rel=\"nofollow");
            if (noopener) {
                builder.append(" noopener");
            }
            builder.append("\"");
        } else if (noopener) {
            builder.append(" rel=\"noopener\"");
        }

        var label = element.label();
        if (label.contains("://") && attr("hide-uri-scheme", element.options()) != null) {
            label = label.substring(label.indexOf("://") + "://".length());
        }
        builder.append(">").append(escape(label)).append("</a>\n");
    }

    @Override
    public void visitDescriptionList(final DescriptionList element) {
        if (element.children().isEmpty()) {
            return;
        }
        builder.append(" <dl>\n");
        for (final var elt : element.children().entrySet()) {
            builder.append("  <dt>").append(escape(elt.getKey())).append("</dt>\n");
            builder.append("  <dd>\n");
            visitElement(elt.getValue());
            builder.append("</dd>\n");
        }
        builder.append(" </dl>\n");
    }

    @Override
    public void visitUnOrderedList(final UnOrderedList element) {
        if (element.children().isEmpty()) {
            return;
        }
        builder.append(" <ul>\n");
        visitListElements(element.children());
        builder.append(" </ul>\n");
    }


    @Override
    public void visitOrderedList(final OrderedList element) {
        if (element.children().isEmpty()) {
            return;
        }
        builder.append(" <ol>\n");
        visitListElements(element.children());
        builder.append(" </ol>\n");
    }

    private void visitListElements(final List<Element> element) {
        for (final var elt : element) {
            builder.append("  <li>\n");
            visitElement(elt);
            builder.append("  </li>\n");
        }
    }

    @Override
    public void visitText(final Text element) {
        final var wrap = element.options().get("nowrap") == null && element.style().size() != 1;
        if (wrap) {
            builder.append(" <span");
            writeCommonAttributes(element.options(), null);
            builder.append(">\n");
        }
        final var styleTags = element.style().stream()
                .map(s -> switch (s) {
                    case BOLD -> "b";
                    case ITALIC -> "i";
                    case EMPHASIS -> "em";
                    case SUB -> "sub";
                    case SUP -> "sup";
                    case MARK -> "mark";
                })
                .toList();
        builder.append(styleTags.stream().map(s -> '<' + s + '>').collect(joining()));
        builder.append(escape(element.value().strip()));
        builder.append(styleTags.stream().sorted(Comparator.reverseOrder()).map(s -> "</" + s + '>').collect(joining()));
        if (wrap) {
            builder.append("\n </span>\n");
        }
    }

    @Override
    public void visitCode(final Code element) {
        final var carbonNowBaseUrl = element.options().get("carbonNowBaseUrl");
        if (carbonNowBaseUrl != null) { // consider the code block as an image
            final var frame = "  <iframe\n" +
                    "    src=\"" + (carbonNowBaseUrl.isBlank() || "auto".equals(carbonNowBaseUrl) ?
                    // todo: this needs to be tuned/tunable
                    "https://carbon.now.sh/embed?bg=rgba%28171%2C184%2C195%2C100%29&t=vscode&wt=none&l=text%2Fx-java&width=680&ds=true&dsyoff=20px&dsblur=68px&wc=true&wa=true&pv=48px&ph=32px&ln=true&fl=1&fm=Droid+Sans+Mono&fs=13px&lh=133%25&si=false&es=2x&wm=false&code=" :
                    carbonNowBaseUrl) + URLEncoder.encode(element.value(), UTF_8) + "\"\n" +
                    "    style=\"width: 1024px; height: 473px; border:0; transform: scale(1); overflow:hidden;\"\n" +
                    "    sandbox=\"allow-scripts allow-same-origin\">\n" +
                    "  </iframe>";
            visitPassthroughBlock(new PassthroughBlock(frame, element.options()));
            return;
        }

        builder.append("<pre");
        final var lang = element.options().getOrDefault("lang", element.options().get("language"));
        if (lang != null) {
            builder.append(" data-lang=\"").append(lang).append("\"");
        }
        writeCommonAttributes(element.options(), c -> lang != null ? "language-" + lang + (c != null ? ' ' + c : "") : c);
        builder.append(">\n  <code>\n");
        builder.append(element.value()); // todo: handle callouts - but by default it should render not that bad
        builder.append("  </code>\n </pre>\n");
    }

    @Override
    public void visitTable(final Table element) {
        final var autowidth = element.options().containsKey("autowidth");
        final var classes = "tableblock" +
                " frame-" + attr("frame", "table-frame", "all", element.options()) +
                " grid-" + attr("grid", "table-grid", "all", element.options()) +
                ofNullable(attr("stripes", "table-stripes", null, element.options()))
                        .map(it -> " stripes-" + it)
                        .orElse("") +
                (autowidth && !element.options().containsKey("width") ? " fit-content" : "") +
                ofNullable(element.options().get("tablepcwidth"))
                        .filter(it -> !"100".equals(it))
                        .map(it -> " width=\"" + it + "\"")
                        .orElse(" stretch") +
                (element.options().containsKey("float") ? " float" : "");

        builder.append(" <table");
        writeCommonAttributes(element.options(), c -> classes + (c == null ? "" : (' ' + c)));
        builder.append(">\n");

        final var title = element.options().get("title");
        if (title != null) {
            builder.append("  <caption class=\"title\">").append(escape(title)).append("</caption>\n");
        }

        if (!element.elements().isEmpty()) { // has row(s)
            final var firstRow = element.elements().get(0);
            final var cols = ofNullable(element.options().get("cols"))
                    .map(it -> Stream.of(it.split(",")).map(this::extractNumbers).toList())
                    .orElse(List.of());

            builder.append("  <colgroup>\n");
            if (autowidth) {
                firstRow.forEach(i -> builder.append("   <col>\n"));
            } else {
                IntStream.range(0, cols.size()).forEach(i -> builder.append("   <col width=\"").append(cols.get(i)).append("\">\n"));
            }
            builder.append("  </colgroup>\n");

            // todo: handle headers+classes without assuming first row is headers - update parser - an options would be better pby?
            builder.append("  <thead>\n");
            builder.append("   <tr>\n");
            firstRow.forEach(it -> {
                builder.append("    <th>\n");
                visitElement(it);
                builder.append("    </th>\n");
            });
            builder.append("   </tr>\n");
            builder.append("  </thead>\n");

            if (element.elements().size() > 1) {
                builder.append("  <tbody>\n");
                element.elements().stream().skip(1).forEach(row -> {
                    builder.append("   <tr>\n");
                    row.forEach(col -> {
                        builder.append("    <td>\n");
                        visitElement(col);
                        builder.append("    </td>\n");
                    });
                    builder.append("   </tr>\n");
                });
                builder.append("  </tbody>\n");
            }
        }

        builder.append(" </table>\n");
    }

    @Override
    public void visitQuote(final Quote element) {
        builder.append(" <div");
        writeCommonAttributes(element.options(), null);
        builder.append(">\n");

        writeBlockTitle(element.options());

        builder.append("  <blockquote>\n");
        Visitor.super.visitQuote(element);
        builder.append("  </blockquote>\n");

        final var attribution = ofNullable(element.options().get("attribution"))
                .orElseGet(() -> element.options().get("citetitle"));
        if (attribution != null) {
            builder.append("  <div class=\"attribution\">\n").append(escape(attribution)).append("\n  </div>\n");
        }

        builder.append(" </div>");
    }

    @Override
    public void visitAnchor(final Anchor element) {
        visitLink(new Link("#" + element.value(), element.label() == null || element.label().isBlank() ? element.value() : element.label(), Map.of()));
    }

    @Override
    public void visitPassthroughBlock(final PassthroughBlock element) {
        builder.append("\n");
        builder.append(element.value());
        builder.append("\n");
    }

    @Override
    public void visitOpenBlock(final OpenBlock element) {
        if (element.options().get("abstract") != null) {
            builder.append(" <div");
            writeCommonAttributes(element.options(), c -> "abstract quoteblock" + (c == null ? "" : (' ' + c)));
            builder.append(">\n");
        } else if (element.options().get("partintro") != null) {
            builder.append(" <div");
            writeCommonAttributes(element.options(), c -> "openblock " + (c == null ? "" : (' ' + c)));
            builder.append(">\n");
        }
        writeBlockTitle(element.options());
        builder.append("  <div class=\"content\">\n");
        Visitor.super.visitOpenBlock(element);
        builder.append("  </div>\n");
        builder.append(" </div>\n");
    }

    @Override
    public ConditionalBlock.Context context() {
        return configuration.getAttributes()::get;
    }

    @Override
    public String result() {
        if (resolver != null) {
            resolver.close();
        }
        return builder.toString();
    }

    @Override
    public void visitMacro(final Macro element) {
        switch (element.name()) {
            case "kbd" -> visitKbd(element);
            case "btn" -> visitBtn(element);
            case "stem" -> visitStem(element);
            case "pass" -> visitPassthroughInline(element);
            case "icon" -> visitIcon(element);
            case "image" -> visitImage(element);
            case "audio" -> visitAudio(element);
            case "video" -> visitVideo(element);
            case "xref" -> visitXref(element);
            // todo: menu, doublefootnote, footnote
            default -> onMissingMacro(element); // for future extension point
        }
    }

    // todo: enhance
    protected void visitXref(final Macro element) {
        var target = element.label();
        final int anchor = target.lastIndexOf('#');
        if (anchor > 0) {
            final var page = target.substring(0, anchor);
            if (page.endsWith(".adoc")) {
                target = page.substring(0, page.length() - ".adoc".length()) + ".html" + target.substring(anchor);
            }
        } else if (target.endsWith(".adoc")) {
            target = target.substring(0, target.length() - ".adoc".length()) + ".html";
        }
        final var label = element.options().get("");
        builder.append(" <a href=\"").append(target).append("\">").append(label == null ? element.label() : label).append("</a>\n");
    }

    // todo: enhance
    protected void visitImage(final Macro element) {
        if (dataUri && !element.label().startsWith("data:")) {
            visitImage(new Macro(
                    element.name(), resolver.apply(element.label()).base64(),
                    Stream.of(element.options(), Map.of("", element.label()))
                            .filter(Objects::nonNull)
                            .map(Map::entrySet)
                            .flatMap(Collection::stream)
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a)),
                    element.inline()));
            return;
        }
        builder.append(" <img src=\"").append(element.label())
                .append("\" alt=\"").append(element.options().getOrDefault("", element.options().getOrDefault("alt", element.label())))
                .append("\">\n");
    }

    protected void visitAudio(final Macro element) {
        builder.append(" <div");
        writeCommonAttributes(element.options(), c -> "audioblock" + (c == null ? "" : (' ' + c)));
        builder.append(">\n");
        writeBlockTitle(element.options());
        builder.append("  <audio src=\"").append(element.label()).append("\"")
                .append(element.options().get("autoplay") != null ? " autoplay" : "").
                append(element.options().get("nocontrols") != null ? " nocontrols" : "")
                .append(element.options().get("loop") != null ? " loop" : "")
                .append(">\n");
        builder.append("  Your browser does not support the audio tag.\n");
        builder.append("  </audio>\n");
        builder.append(" </div>\n");
    }

    // todo: support youtube etc? not sure it makes much sense
    protected void visitVideo(final Macro element) {
        builder.append(" <div");
        writeCommonAttributes(element.options(), c -> "videoblock" + (c == null ? "" : (' ' + c)));
        builder.append(">\n");
        writeBlockTitle(element.options());
        builder.append("  <video src=\"").append(element.label()).append("\"")
                .append(element.options().get("autoplay") != null ? " autoplay" : "").
                append(element.options().get("nocontrols") != null ? " nocontrols" : "")
                .append(element.options().get("loop") != null ? " loop" : "")
                .append(">\n");
        builder.append("  Your browser does not support the video tag.\n");
        builder.append("  </video>\n");
        builder.append(" </div>\n");
    }

    protected void visitPassthroughInline(final Macro element) {
        builder.append(element.label());
    }

    protected void visitBtn(final Macro element) {
        builder.append(" <b class=\"button\">").append(escape(element.label())).append("</b>\n");
    }

    protected void visitKbd(final Macro element) {
        builder.append(" <kbd>").append(escape(element.label())).append("</kbd>\n");
    }

    // todo: enhance
    protected void visitIcon(final Macro element) {
        builder.append(" <i class=\"")
                .append(element.label().startsWith("fa") && !element.label().contains(" ") ? "fa " : "")
                .append(element.label())
                .append("\"></i>\n");
    }

    protected void visitStem(final Macro element) {
        throw new IllegalArgumentException("stem not yet supported");
    }

    protected void writeCommonAttributes(final Map<String, String> options, final Function<String, String> classProcessor) {
        var classes = options.get("role");
        if (classProcessor != null) {
            classes = classProcessor.apply(classes);
        }
        if (classes != null && !classes.isBlank()) {
            builder.append(" class=\"").append(classes).append("\"");
        }

        final var id = options.get("id");
        if (id != null && !id.isBlank()) {
            builder.append(" id=\"").append(id).append("\"");
        }
    }

    protected void writeBlockTitle(final Map<String, String> options) {
        final var title = options.get("title");
        if (title != null) {
            builder.append("  <div class=\"title\">").append(escape(title)).append("</div>\n");
        }
    }

    protected void onMissingMacro(final Macro element) {
        Visitor.super.visitMacro(element);
    }

    protected String escape(final String name) {
        return HtmlEscaping.INSTANCE.apply(name);
    }

    protected String attr(final String key, final String defaultKey, final String defaultValue, final Map<String, String> mainMap) {
        return mainMap.getOrDefault(key, configuration.getAttributes().getOrDefault(defaultKey, defaultValue));
    }

    protected String attr(final String key, final Map<String, String> defaultMap) {
        return attr(key, key, null, defaultMap);
    }

    private int extractNumbers(final String col) {
        int i = 0;
        while (col.length() > i && Character.isDigit(col.charAt(i))) {
            i++;
        }
        i--;
        try {
            if (i <= 0) {
                return 1;
            }
            return Integer.parseInt(col.substring(0, i));
        } catch (final NumberFormatException nfe) {
            return 1;
        }
    }

    public static class Configuration {
        private DataResolver resolver;
        private Path assetsBase;
        private Map<String, String> attributes = Map.of();

        public DataResolver getResolver() {
            return resolver;
        }

        public Configuration setResolver(final DataResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Path getAssetsBase() {
            return assetsBase;
        }

        public Configuration setAssetsBase(final Path assetsBase) {
            this.assetsBase = assetsBase;
            return this;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public Configuration setAttributes(final Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }
    }
}
