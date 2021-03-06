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

import java.io.IOException;
import java.util.NoSuchElementException;

import org.jaxen.JaxenException;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

public class TestEditing extends WebTestSupport {

  private static final String FAKE_SESSION_ID = "53ABE0412468E35DE001355A4EE80822";
  private static final String USER1_EDIT_CONTENT = "user1";
  private static final String USER2_EDIT_CONTENT = "user2";
  private static final String ID_EDIT_FORM = "editForm";


  public void testEditPageIncrementsRevision() throws Exception {
    String name = uniqueWikiPageName("EditPageTest");
    HtmlPage initial = editWikiPage(name, "Initial content", "", "", true);
    long initialRevision = getRevisionNumberFromTitle(initial);
    HtmlPage edited = editWikiPage(name, "Initial content.  Extra content.", "", "", false);
    assertEquals(initialRevision + 1, getRevisionNumberFromTitle(edited));
  }

  public void testCancelEditNewPage() throws Exception {
    String name = uniqueWikiPageName("EditPageTest");
    editThenCancel(name);
  }

  public void testCancelEditExistingPage() throws Exception {
    String name = uniqueWikiPageName("EditPageTest");
    editWikiPage(name, "Whatever", "", "", true);
    editThenCancel(name);
  }

  public void testEditAttributes() throws Exception {
    String name = uniqueWikiPageName("EditPageTest");
    editWikiPage(name, "Initial Status: <<attr:status>>", "", "", true);
    HtmlPage edited = editWikiPage(name, "Initial Status: <<attr:status>>", "status = completed", "", false);
    String editedPageText = edited.asText();
    assertTrue(editedPageText.contains("Initial Status: completed"));
  }

  public void testEditAttributesNewPage() throws Exception {
    String name = uniqueWikiPageName("EditPageTest");
    HtmlPage edited = editWikiPage(name, "Initial Status: <<attr:status>>", "status = completed", "", true);
    String editedPageText = edited.asText();
    assertTrue(editedPageText.contains("Initial Status: completed"));
  }

  public void testDeleteAttributes() throws Exception {
    String name = uniqueWikiPageName("EditPageTest");
    HtmlPage edited = editWikiPage(name, "Initial Status: <<attr:status>>", "status = completed", "", true);
    assertTrue(edited.asText().contains("Initial Status: completed"));
    edited = editWikiPage(name, "Initial Status: <<attr:status>>", "", "", false);
    assertTrue(edited.asText().contains("Initial Status:"));
    assertFalse(edited.asText().contains("Initial Status: completed"));
  }
  
  public void testPreviewDiffBug() throws Exception {
    // https://bugs.corefiling.com/show_bug.cgi?id=13510
    // Old versions of the diff_match_patch package have a bug in the diff_cleanupSemantic method
    // The two comparison strings need to be well crafted to trigger the bug.
    String name = uniqueWikiPageName("PreviewPageTest");
    String content = "=" + NEWLINE_TEXTAREA + "This is a lis of things that someone be doing if" + NEWLINE_TEXTAREA + "===Wiki" + NEWLINE_TEXTAREA;
    String newContent = "||" + NEWLINE_TEXTAREA + "==";
    
    HtmlPage edited = editWikiPage(name, content, "", "", true);
    HtmlPage editPage = clickEditLink(edited);
    HtmlForm form = editPage.getFormByName(ID_EDIT_FORM);
    HtmlTextArea contentArea = form.getTextAreaByName("content");
    contentArea.setText(newContent);
    editPage = (HtmlPage) form.getButtonByName("preview").click();
    
    form = editPage.getFormByName(ID_EDIT_FORM);
    contentArea = form.getTextAreaByName("content");
    assertEquals(newContent + NEWLINE_TEXTAREA, contentArea.getText());
  }
  
  public void testPreview() throws Exception {
    String name = uniqueWikiPageName("PreviewPageTest");
    HtmlPage editPage = clickEditLink(getWikiPage(name));
    HtmlForm form = editPage.getFormByName(ID_EDIT_FORM);
    HtmlInput minorEdit = form.getInputByName("minorEdit");
    assertFalse(minorEdit.isChecked());
    minorEdit.setChecked(true);
    HtmlInput description = form.getInputByName("description");
    String expectedDescription = "My test change";
    description.setValueAttribute(expectedDescription);
    HtmlTextArea content = form.getTextAreaByName("content");
    String expectedContent = "http://www.example.com";
    content.setText(expectedContent);

    // Now if we preview we should get the previewed text rendered, and in
    // the edit area.  The other options should be preserved too.
    editPage = (HtmlPage) form.getButtonByName("preview").click();
    form = editPage.getFormByName(ID_EDIT_FORM);
    minorEdit = form.getInputByName("minorEdit");
    description = form.getInputByName("description");
    content = form.getTextAreaByName("content");
    assertEquals(expectedDescription, description.getValueAttribute());
    assertTrue(minorEdit.isChecked());
    assertEquals(expectedContent + NEWLINE_TEXTAREA, content.getText());
    // This checks for the rendering...
    assertNotNull(editPage.getAnchorByHref(expectedContent));
  }

  public void testPreviewWithChangedAttributes() throws Exception {
    String name = uniqueWikiPageName("PreviewPageTest");
    HtmlPage editPage = clickEditLink(getWikiPage(name));
    HtmlForm form = editPage.getFormByName(ID_EDIT_FORM);
    HtmlTextArea attributes = form.getTextAreaByName("attributes");
    String expectedContent = "SomeContent";
    String expectedAttributes = "\"text\" = \"" + expectedContent + "\"";
    attributes.setText(expectedAttributes);
    HtmlTextArea content = form.getTextAreaByName("content");
    String expectedContentSource = "<<attr:text>>";
    content.setText(expectedContentSource);

    // Now if we preview we should get the previewed text rendered, and in
    // the edit area.
    editPage = (HtmlPage) form.getButtonByName("preview").click();
    editPage.asText().contains(expectedContent);
    form = editPage.getFormByName(ID_EDIT_FORM);
    attributes = form.getTextAreaByName("attributes");
    assertEquals(expectedAttributes + NEWLINE_TEXTAREA, attributes.getText());
    content = form.getTextAreaByName("content");
    assertEquals(expectedContentSource + NEWLINE_TEXTAREA, content.getText());
  }

  private void editThenCancel(final String name) throws Exception {
    final String flagText = "Should not be saved.";
    HtmlPage editPage = clickEditLink(getWikiPage(name));
    HtmlForm form = editPage.getFormByName(ID_EDIT_FORM);
    form.getTextAreaByName("content").setText(flagText);
    HtmlPage viewPage = (HtmlPage) form.getButtonByName("unlock").click();
    assertFalse(viewPage.asText().contains(flagText));
    try {
      viewPage.getFormByName(ID_EDIT_FORM);
      fail("Should be back to view page, not edit form.");
    }
    catch (ElementNotFoundException ignore) {
    }
    assertEquals("Should not be present.", 0,viewPage.getByXPath("id('lockedInfo')").size());
  }

  public void testLoseLockSave() throws Exception {
    HtmlPage pageUser1 = loseLockHelper("save");
    // User 1 Save (again)
    pageUser1 = (HtmlPage) ((HtmlButton) pageUser1.getByXPath("//button[@name='save']").iterator().next()).click();
    // Should be a Save button (content has changed error)
    assertEquals(1, pageUser1.getByXPath("//button[@name='save']").size());
    // User 1 Save (again)
    pageUser1 = (HtmlPage) ((HtmlButton) pageUser1.getByXPath("//button[@name='save']").iterator().next()).click();
    // Should NOT be a Save button (allowed user to merge changes)
    assertEquals(0, pageUser1.getByXPath("//button[@name='save']").size());
    assertTrue(pageUser1.asText().contains(USER1_EDIT_CONTENT));
  }

  public void testLoseLockPreview() throws Exception {
    loseLockHelper("preview");
  }

  private HtmlPage loseLockHelper(final String buttonName) throws Exception, IOException, JaxenException {
    final String name = uniqueWikiPageName("LoseLockPreviewTest");
    // User 1 make page
    editWikiPage(name, "content", "", "", true);
    // User 1 edit
    HtmlPage pageUser1 = clickEditLink(getWikiPage(name));
    // Set the content to "user1"
    ((HtmlTextArea) pageUser1.getByXPath("id('contentArea')").iterator().next()).setText(USER1_EDIT_CONTENT);
    // User 2 unlock and edit
    switchUser();
    HtmlPage pageUser2 = getWikiPage(name);
    pageUser2 = (HtmlPage) ((HtmlSubmitInput) pageUser2.getByXPath("//input[@value='Unlock']").iterator().next()).click();
    pageUser2 = clickEditLink(pageUser2);
    // Set the content to "user2"
    ((HtmlTextArea) pageUser2.getByXPath("id('contentArea')").iterator().next()).setText(USER2_EDIT_CONTENT);
    // User 1 Save/Preview
    switchUser();
    pageUser1 = (HtmlPage) ((HtmlButton) pageUser1.getByXPath("//button[@name='" + buttonName + "']").iterator().next()).click();
    // Should be a Save button
    assertEquals(1, pageUser1.getByXPath("//button[@name='save']").size());
    // Should be a flash with "lock" in the message
    assertTrue(getErrorMessage(pageUser1).toLowerCase().contains("lock"));
    // Should be a diff
    assertTrue(pageUser1.getByXPath("//*[@class='diff']").size() > 0);
    // User 2 Save
    switchUser();
    pageUser2 = (HtmlPage) ((HtmlButton) pageUser2.getByXPath("//button[@name='save']").iterator().next()).click();
    // Should NOT be a Save button
    assertEquals(0, pageUser2.getByXPath("//button[@name='save']").size());
    // Return User 1 page
    switchUser();
    return pageUser1;
  }

  public void testPreviewInvalidSessionId() throws Exception {
    testInvalidSessionIdHelper("preview", uniqueWikiPageName("PreviewInvalidSessionIdTest"), "Preview with invalid session id should not show content", false);
  }

  public void testSaveInvalidSessionId() throws Exception {
    testInvalidSessionIdHelper("save", uniqueWikiPageName("SaveInvalidSessionIdTest"), "Save with invalid session id should not show content", false);
  }

  public void testPreviewInvalidSessionIdFakeAppended() throws Exception {
    testInvalidSessionIdHelper("preview", uniqueWikiPageName("PreviewInvalidSessionIdTest"), "Preview with invalid session id should not show content", true);
  }

  public void testSaveInvalidSessionIdFakeAppended() throws Exception {
    testInvalidSessionIdHelper("save", uniqueWikiPageName("SaveInvalidSessionIdTest"), "Save with invalid session id should not show content", true);
  }

  private void testInvalidSessionIdHelper(final String button, final String name, final String failMsg, final boolean fakeAppendedSessionId) throws IOException, JaxenException {
    HtmlPage editPage = clickEditLink(getWikiPage(name));
    final HtmlForm form = editPage.getFormByName(ID_EDIT_FORM);
    final HtmlInput sessionId = form.getInputByName("sessionId");
    sessionId.setValueAttribute(FAKE_SESSION_ID);
    if (fakeAppendedSessionId) {
      form.setActionAttribute(name + ";jsessionid=" + FAKE_SESSION_ID);
    }
    final HtmlTextArea content = form.getTextAreaByName("content");
    final String expectedContent = "http://www.example.com";
    content.setText(expectedContent);

    editPage = (HtmlPage) form.getButtonByName(button).click();

    try {
      getAnchorByHrefContains(editPage, expectedContent);
      fail(failMsg);
    }
    catch (NoSuchElementException e) {
    }
  }
}
