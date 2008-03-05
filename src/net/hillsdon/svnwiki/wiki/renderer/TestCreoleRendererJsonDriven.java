package net.hillsdon.svnwiki.wiki.renderer;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.hillsdon.svnwiki.vc.PageReference;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JavaTypeMapper;

public class TestCreoleRendererJsonDriven extends TestCase {

  private List<Map<String, String>> _tests;
  private CreoleRenderer _renderer;

  @SuppressWarnings("unchecked")
  public TestCreoleRendererJsonDriven() throws Exception {
    JsonFactory jf = new JsonFactory();
    _tests = (List<Map<String, String>>) new JavaTypeMapper().read(jf.createJsonParser(getClass().getResource("unit-test-data.json")));
    _renderer = new CreoleRenderer(new RenderNode[0]);
  }
  
  public void test() throws Exception {
    final PrintStream err = System.err;
    int errors = 0;
    for (Map<String, String> test : _tests) {
      final String caseName = test.get("name");
      final String bugExplanation = test.get("bug");
      final String expected = test.get("output");
      final String input = test.get("input");
      final String actual = _renderer.render(new PageReference(""), input);
      final boolean match = expected.equals(actual);
      if (bugExplanation != null) {
        assertFalse("You fixed " + caseName, match);
        continue;
      }
      if (!match) {
        errors++;
        err.println("Creole case: " + caseName);
        err.println("Input:\n" + input);
        err.println("Expected:\n" + expected);
        err.println("Actual:\n" + actual);
        err.println();
      }
    }
    if (errors > 0) {
      fail("Creole rendering errors, please see stderr.");
    }
  }
  
}