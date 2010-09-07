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
package net.hillsdon.reviki.wiki.graph;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import junit.framework.TestCase;
import net.hillsdon.reviki.search.SearchEngine;
import net.hillsdon.reviki.search.SearchMatch;
import net.hillsdon.reviki.vc.impl.CachingPageStore;
import net.hillsdon.reviki.vc.impl.PageReferenceImpl;
import net.hillsdon.reviki.vc.impl.SimplePageStore;

import org.easymock.EasyMock;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class TestWikiGraphImpl extends TestCase {
  public static final Function<String, SearchMatch> FROM_PAGE_NAME = new Function<String, SearchMatch>() {
    public SearchMatch apply(final String in) {
      return new SearchMatch(true, null, in, null, null);
    }
  };

  private CachingPageStore _store;
  private SearchEngine _mockedSearchEngine;

  private WikiGraph _graph;

  @Override
  protected void setUp() throws Exception {
    _store = new SimplePageStore();
    _store.set(new PageReferenceImpl("FooPage"), "", -1, "Foo content", "");
    _mockedSearchEngine = EasyMock.createMock(SearchEngine.class);
    _graph = new WikiGraphImpl(_store, _mockedSearchEngine);
  }

  public void testRemovesNonExistantPagesFromOutgoingLinks() throws Exception {
    expect(_mockedSearchEngine.outgoingLinks("RootPage")).andReturn(ImmutableSet.copyOf(Iterables.transform(ImmutableSet.of("FooPage", "BarPage"), FROM_PAGE_NAME)));
    replay(_mockedSearchEngine);
    assertEquals(ImmutableSet.of(FROM_PAGE_NAME.apply("FooPage")), _graph.outgoingLinks("RootPage"));
  }

  public void testRemovesNonExistantPagesFromIncomingLinks() throws Exception {
    expect(_mockedSearchEngine.incomingLinks("RootPage")).andReturn(ImmutableSet.copyOf(Iterables.transform(ImmutableSet.of("FooPage", "BarPage"), FROM_PAGE_NAME)));
    replay(_mockedSearchEngine);
    assertEquals(ImmutableSet.of(FROM_PAGE_NAME.apply("FooPage")), _graph.incomingLinks("RootPage"));
  }

}
