/**
 * Copyright 2008 Matthew Hillsdon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hillsdon.reviki.webtests;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Iterator;
import org.jaxen.JaxenException;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TestRecentChanges extends WebTestSupport {

  /**
   * Create two new pages.
   * Ensure they appear at the top of RecentChanges with the second page first.
   * Edit the first page.
   * Ensure it it now at the top of recent changes.
   */
  public void testRecentChanges() throws Exception {
    String createdFirst = uniqueWikiPageName("RecentChangesTestFirst");
    editWikiPage(createdFirst, "Content", "", "", true);
    String createdSecond = uniqueWikiPageName("RecentChangesTestSecond");
    editWikiPage(createdSecond, "Content", "", "", true);

    Iterator<HtmlAnchor> links = getRecentChangesLinks();
    HtmlAnchor first = links.next();
    links.next();
    HtmlAnchor second = links.next();
    assertEquals(first.asText(), createdSecond);
    assertEquals(second.asText(), createdFirst);

    String descriptionOfChange = format("Bump %s up to top.", createdFirst);
    editWikiPage(createdFirst, "Different content", "", descriptionOfChange, false);
    links = getRecentChangesLinks();
    first = links.next();
    links.next();
    second = links.next();
    assertEquals(first.asText(), createdFirst);
    assertEquals(second.asText(), createdSecond);

    HtmlPage recentChanges = getWikiPage("RecentChanges");
    String recentChangesAsText = recentChanges.asText();
    assertTrue(recentChanges.asText().contains(descriptionOfChange));
    // Test spacing between page name and revision number https://bugs.corefiling.com/show_bug.cgi?id=40660
    assertTrue(recentChangesAsText.contains(createdFirst + " r"));
  }

  @SuppressWarnings("unchecked")
  private Iterator<HtmlAnchor> getRecentChangesLinks() throws IOException, JaxenException {
    HtmlPage recentChanges = getWikiPage("RecentChanges");
    Iterator<HtmlAnchor> links = (Iterator<HtmlAnchor>) recentChanges.getByXPath("//tr/td[position() = 2]/a").iterator();
    return links;
  }
}
