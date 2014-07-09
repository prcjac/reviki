package net.hillsdon.reviki.wiki.renderer.creole.parser.ast;

import net.hillsdon.fij.text.Escape;
import net.hillsdon.reviki.wiki.renderer.result.LeafResultNode;

public class Plaintext extends LeafResultNode {

  protected String contents;
  
  public Plaintext(String contents) {
    this.contents = contents;
  }

  public String toXHTML() {
    return Escape.html(contents);
  }

}
