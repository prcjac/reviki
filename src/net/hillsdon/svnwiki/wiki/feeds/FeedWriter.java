package net.hillsdon.svnwiki.wiki.feeds;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.hillsdon.svnwiki.text.WikiWordUtils;
import net.hillsdon.svnwiki.vc.ChangeInfo;
import net.hillsdon.svnwiki.wiki.WikiUrls;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Converts changes to syndication formats. 
 * 
 * @author mth
 */
public class FeedWriter {

  static final String ATOM_NS = "http://www.w3.org/2005/Atom";

  private static void addElement(final TransformerHandler handler, final String name, final String content) throws SAXException {
    handler.startElement(ATOM_NS, "", name, null);
    handler.characters(content.toCharArray(), 0, content.length());
    handler.endElement(ATOM_NS, "", name);
  }

  private static void addLink(final TransformerHandler handler, final String href, final String rel) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    attrs.addAttribute("", "", "href", null, href);
    if (rel != null) {
      attrs.addAttribute("", "", "rel", null, rel);
    }
    handler.startElement(ATOM_NS, "", "link", attrs);
    handler.endElement(ATOM_NS, "", "link");
  }

  private static void addUpdated(TransformerHandler handler, Date date) throws SAXException {
    addElement(handler, "updated", new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ").format(date));
  }

  public static void writeAtom(final WikiUrls wikiUrls, final PrintWriter out, final Collection<ChangeInfo> changes) throws TransformerConfigurationException, SAXException {
    StreamResult streamResult = new StreamResult(out);
    SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    TransformerHandler handler = tf.newTransformerHandler();
    Transformer serializer = handler.getTransformer();
    try {
      serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }
    catch (IllegalArgumentException ex) {
      // Oh well, ugly XML then.
    }
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      
    handler.setResult(streamResult);
    handler.startDocument();
    handler.startElement(ATOM_NS, "", "feed", null);
    addElement(handler, "title", "svnwiki feed");
    addLink(handler, wikiUrls.feed(), "self");
    addLink(handler, wikiUrls.root(), null);
    addUpdated(handler, changes.isEmpty() ? new Date(0) : changes.iterator().next().getDate());
    for (ChangeInfo change : changes) { 
      handler.startElement(ATOM_NS, "", "entry", null);
      addElement(handler, "title", WikiWordUtils.pathToTitle(change.getPath()));
      addLink(handler, wikiUrls.page(change.getPath()), null);
      addElement(handler, "id", wikiUrls.page(change.getPath()));
      addElement(handler, "summary", change.getDescription());
      addUpdated(handler, change.getDate());
      handler.endElement(ATOM_NS, "", "entry");
    }
    handler.endElement(ATOM_NS, "", "feed");
    handler.endDocument();
  }

}