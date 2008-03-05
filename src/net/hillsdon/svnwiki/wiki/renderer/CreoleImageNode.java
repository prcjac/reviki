package net.hillsdon.svnwiki.wiki.renderer;


/**
 * "{{"..."}}" for inline images.
 * 
 * @author mth
 */
public class CreoleImageNode extends LinkNode {

  public CreoleImageNode(final LinkPartsHandler handler) {
    super("[{][{](.*?)[}][}]", new CreoleLinkContentsSplitter(), handler);
  }
  
}