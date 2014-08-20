package net.hillsdon.reviki.wiki.renderer;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.uwyn.jhighlight.renderer.Renderer;
import com.uwyn.jhighlight.renderer.XhtmlRendererFactory;

import net.hillsdon.fij.text.Escape;
import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.vc.SimplePageStore;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.wiki.renderer.creole.CreoleBasedRenderer;
import net.hillsdon.reviki.wiki.renderer.creole.LinkParts;
import net.hillsdon.reviki.wiki.renderer.creole.LinkPartsHandler;
import net.hillsdon.reviki.wiki.renderer.creole.ast.*;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;

/**
 * Render to HTML. This is largely a direct translation of the old
 * ASTNode.toXHTML() style of rendering, with few changes.
 *
 * @author msw
 */
public class HtmlRenderer extends CreoleBasedRenderer<String> {
  /**
   * Most elements have a consistent CSS class. Links and images are an
   * exception (as can be seen in their implementation), as their HTML is
   * generated by a link handler.
   */
  public static final String CSS_CLASS_ATTR = "class='wiki-content'";

  public HtmlRenderer(final SimplePageStore pageStore, final LinkPartsHandler linkHandler, final LinkPartsHandler imageHandler, final Supplier<List<Macro>> macros) {
    super(pageStore, linkHandler, imageHandler, macros);
  }

  @Override
  public String render(ASTNode ast, URLOutputFilter urlOutputFilter) {
    HtmlVisitor visitor = new HtmlVisitor(urlOutputFilter);
    return visitor.visit(ast);
  }

  @Override
  public String getContentType() {
    return "text/html; charset=utf-8";
  }

  private static final class HtmlVisitor extends ASTRenderer<String> {
    public HtmlVisitor(URLOutputFilter urlOutputFilter) {
      super("", urlOutputFilter);
    }

    @Override
    protected String combine(final String x1, final String x2) {
      return x1 + x2;
    }

    /**
     * Render a node with a tag.
     */
    public String renderTagged(final String tag, final Optional<? extends ASTNode> node) {
      // Render the tag
      if (!node.isPresent()) {
        return "<" + tag + " " + CSS_CLASS_ATTR + " />";
      }
      else {
        return "<" + tag + " " + CSS_CLASS_ATTR + ">" + visitASTNode(node.get()) + "</" + tag + ">";
      }
    }

    /**
     * Render some syntax-highlighted code.
     */
    public String highlight(final String code, final Optional<Languages> language) {
      Renderer highlighter = null;
      if (language.isPresent()) {
        String lang = null;
        switch (language.get()) {
          case CPLUSPLUS:
            lang = XhtmlRendererFactory.CPLUSPLUS;
            break;
          case JAVA:
            lang = XhtmlRendererFactory.JAVA;
            break;
          case XHTML:
            lang = XhtmlRendererFactory.XHTML;
            break;
          case XML:
            lang = XhtmlRendererFactory.XML;
            break;
        }
        highlighter = XhtmlRendererFactory.getRenderer(lang);
      }

      String highlighted = null;
      if (highlighter == null) {
        highlighted = Escape.html(code);
      }
      else {
        try {
          highlighted = highlighter.highlight("", code, "UTF-8", true);
        }
        catch (IOException e) {
          highlighted = Escape.html(code);
        }
      }

      return highlighted.replace("&nbsp;", " ").replace("<br />", "");
    }

    @Override
    public String visitBold(final Bold node) {
      return renderTagged("strong", Optional.of(node));
    }

    @Override
    public String visitCode(final Code node) {
      String out = "<pre " + CSS_CLASS_ATTR + ">";
      out += highlight(node.getText(), node.getLanguage());
      out += "</pre>";
      return out;
    }

    @Override
    public String visitHeading(final Heading node) {
      return renderTagged("h" + node.getLevel(), Optional.of(node));
    }

    @Override
    public String visitHorizontalRule(final HorizontalRule node) {
      return renderTagged("hr", Optional.<ASTNode>absent());
    }

    @Override
    public String visitImage(final Image node) {
      LinkPartsHandler handler = node.getHandler();
      PageInfo page = node.getPage();
      LinkParts parts = node.getParts();

      try {
        return handler.handle(page, Escape.html(parts.getText()), parts, urlOutputFilter());
      }
      catch (Exception e) {
        return Escape.html(parts.getText());
      }
    }

    @Override
    public String visitInlineCode(final InlineCode node) {
      String out = "<code " + CSS_CLASS_ATTR + ">";
      out += highlight(node.getText(), node.getLanguage());
      out += "</code>";
      return out;
    }

    @Override
    public String visitItalic(final Italic node) {
      return renderTagged("em", Optional.of(node));
    }

    @Override
    public String visitLinebreak(final Linebreak node) {
      return renderTagged("br", Optional.<ASTNode>absent());
    }

    @Override
    public String visitLink(final Link node) {
      LinkPartsHandler handler = node.getHandler();
      PageInfo page = node.getPage();
      LinkParts parts = node.getParts();

      try {
        return handler.handle(page, Escape.html(parts.getText()), parts, urlOutputFilter());
      }
      catch (Exception e) {
        // Special case: render mailto: as a link if it didn't get interwiki'd
        String target = node.getTarget();
        String title = node.getTitle();
        if (target.startsWith("mailto:")) {
          return String.format("<a href='%s'>%s</a>", target, Escape.html(title));
        }
        else {
          return Escape.html(parts.getText());
        }
      }
    }

    @Override
    public String visitListItem(final ListItem node) {
      return renderTagged("li", Optional.of(node));
    }

    @Override
    public String visitMacroNode(final MacroNode node) {
      String tag = node.isBlock() ? "pre" : "code";
      String inner = Escape.html(node.getText());
      return "<" + tag + " " + CSS_CLASS_ATTR + ">" + inner + "</" + tag + ">";
    }

    @Override
    public String visitOrderedList(final OrderedList node) {
      return renderTagged("ol", Optional.of(node));
    }

    @Override
    public String visitParagraph(final Paragraph node) {
      return renderTagged("p", Optional.of(node));
    }

    @Override
    public String visitStrikethrough(final Strikethrough node) {
      return renderTagged("strike", Optional.of(node));
    }

    @Override
    public String visitTable(final Table node) {
      return renderTagged("table", Optional.of(node));
    }

    /** Render a table cell with vertical alignment. */
    protected String valign(final String tag, final ASTNode node) {
      if (!isEnabled(TABLE_ALIGNMENT_DIRECTIVE)) {
        return renderTagged(tag, Optional.of(node));
      }

      try {
        String out = "<" + tag + " " + CSS_CLASS_ATTR;
        out += " style='vertical-align:" + unsafeGetArgs(TABLE_ALIGNMENT_DIRECTIVE).get(0) + "'>";
        out += visitASTNode(node);
        out += "</" + tag + ">";
        return out;
      }
      catch (Exception e) {
        System.err.println("Error when handling directive " + TABLE_ALIGNMENT_DIRECTIVE);
        return renderTagged(tag, Optional.of(node));
      }
    }

    @Override
    public String visitTableCell(final TableCell node) {
      return valign("td", node);
    }

    @Override
    public String visitTableHeaderCell(final TableHeaderCell node) {
      return valign("th", node);
    }

    @Override
    public String visitTableRow(final TableRow node) {
      return renderTagged("tr", Optional.of(node));
    }

    @Override
    public String visitTextNode(final TextNode node) {
      String text = node.getText();
      return node.isEscaped() ? Escape.html(text) : text;
    }

    @Override
    public String visitUnorderedList(final UnorderedList node) {
      return renderTagged("ul", Optional.of(node));
    }
  }
}
