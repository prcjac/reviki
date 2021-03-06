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
package net.hillsdon.reviki.web.urls;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Can create links to external wikis given a wiki name and page name.
 *
 * @author mth
 */
public class InterWikiLinker {

  private final Map<String, String> _links = new LinkedHashMap<String, String>();

  /**
   * @param wikiName Wiki name.  Will overwrite any previous entry with the same wiki name.
   * @param formatString Absolute URI template generally with one or more %s tokens which will be replaced by the page name when creating links.
   */
  public void addWiki(final String wikiName, final String formatString) {
    _links.put(wikiName, formatString);
  }

  /**
   * @param wikiName Wiki name.
   * @param pageName Page name/
   * @return A link.
   * @throws UnknownWikiException If wikiName is unknown.
   * @throws URISyntaxException 
   * @see #addWiki(String, String)
   */
  public URI uri(final String wikiName, final String pageName, final String fragment) throws UnknownWikiException, URISyntaxException {
    String formatString = _links.get(wikiName);
    if (formatString == null) {
      throw new UnknownWikiException();
    }
    // Sigh - format strings and otherwise-encoded URIs don't mix
    final URI template = new URI(formatString.replace("%s", "%25s"));
    
    // Replace occurrances of %s (now %25s) on a per-component basis since they have different encoding.
    // If the template has a fragment it is:
    //  replaced with the specified fragment if it doesn't contain %s
    //  %s replaced with fragment if specified
    //  %s replaced with pageName if fragment isn't specified
    //  otherwise left alone
    final String newPath = template.getPath() != null ? template.getPath().replace("%s", pageName) : null;
    final String newQuery = template.getQuery() != null ? template.getQuery().replace("%s", pageName) : null;
    String newFragment = template.getFragment();
    if (newFragment != null && newFragment.contains("%s")) {
      if (fragment != null) {
        newFragment = newFragment.replace("%s", fragment);
      }
      else {
        newFragment = newFragment.replace("%s", pageName);
      }
    }
    else {
      if (fragment != null) {
        newFragment = fragment;
      }
    }
    
    return new URI(template.getScheme(), template.getUserInfo(), template.getHost(), template.getPort(), newPath, newQuery, newFragment);
  }

  /**
   * Exposed for testing.
   * @return Unmodifiable map from wiki name for format string as provided in {@link #addWiki(String, String)}.
   */
  public Map<String, String> getWikiToFormatStringMap() {
    return Collections.unmodifiableMap(_links);
  }

}
