package org.alfresco.share.site.document;

import static org.alfresco.po.share.site.document.TinyMceEditor.FormatType.BOLD;
import static org.alfresco.po.share.site.document.TinyMceEditor.FormatType.BOLD_EDIT;
import static org.alfresco.po.share.site.document.TinyMceEditor.FormatType.BULLET;
import static org.alfresco.po.share.site.document.TinyMceEditor.FormatType.ITALIC;
import static org.alfresco.po.share.site.document.TinyMceEditor.FormatType.NUMBER;
import static org.alfresco.share.util.RandomUtil.getRandomListString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.alfresco.po.share.RepositoryPage;
import org.alfresco.po.share.enums.TinyMceColourCode;
import org.alfresco.po.share.site.document.AddCommentForm;
import org.alfresco.po.share.site.document.EditCommentForm;
import org.alfresco.po.share.site.document.FileDirectoryInfo;
import org.alfresco.po.share.site.document.FolderDetailsPage;
import org.alfresco.po.share.site.document.PaginationForm;
import org.alfresco.po.share.site.document.TinyMceEditor;
import org.alfresco.share.util.AbstractUtils;
import org.alfresco.share.util.ShareUser;
import org.alfresco.share.util.ShareUserSitePage;
import org.alfresco.share.util.SiteUtil;
import org.alfresco.share.util.api.CreateUserAPI;
import org.alfresco.test.FailedTestListener;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * @author Aliaksei Boole
 */
@Listeners(FailedTestListener.class)
public class FolderDetailsCommentsTest extends AbstractUtils
{
        private static final Logger logger = Logger.getLogger(FolderDetailsCommentsTest.class);

                @Override
                @BeforeClass(alwaysRun = true)
        public void setup() throws Exception
        {
                super.setup();
                testName = this.getClass().getSimpleName();
                logger.info("Start Tests in: " + testName);
        }


        private void precondition(String user, String siteName, String folderName) throws Exception
        {
                // Create OP user.
                CreateUserAPI.CreateActivateUser(drone, ADMIN_USERNAME, user);
                // Any user is logged into the Share
                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                // Any site is created
                SiteUtil.createSite(drone, siteName, siteName, SITE_VISIBILITY_PUBLIC, true);
                // Any Folder is created
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                ShareUserSitePage.createFolder(drone, folderName, folderName);
                //Folder details page is opened
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14916() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14916()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                AddCommentForm addCommentForm = folderDetailsPage.clickAddCommentButton();
                assertTrue(addCommentForm.isDisplay(), "Comment form didn't display.");
                assertTrue(addCommentForm.isAvatarDisplay(), "User avatar didn't display.");
                assertTrue(addCommentForm.isButtonsEnable(), "Add or Cancel button didn't enable.");

                String enteredText = getRandomString(5);
                TinyMceEditor tinyMceEditor = addCommentForm.getTinyMceEditor();
                tinyMceEditor.setText(enteredText);
                assertEquals(tinyMceEditor.getText(), enteredText, "Text didn't enter in MCE box or didn't correct.");

                addCommentForm.clickCancelButton();
                assertFalse(addCommentForm.isDisplay(), "Comment form didn't closed.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14917() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14917()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = "latest";
                folderDetailsPage.addComment(text);
                assertEquals(folderDetailsPage.getComments().size(), 1, "Comment didn't added or more then once");

                text = getRandomString(5);
                folderDetailsPage.addComment(text);
                assertEquals(folderDetailsPage.getComments().size(), 2, "Number of comments or does not meet the expected");
                assertEquals(folderDetailsPage.getComments().get(0), text, "The first is not the last comment.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14918() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14918()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = "";
                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isErrorBalloonMessageDisplay(), "Information message about error didn't show");
                assertEquals(folderDetailsPage.getComments().size(), 0, "Empty comment added.");
                drone.refresh();

                text = "&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;";
                folderDetailsPage.addComment(text);
                assertEquals(folderDetailsPage.getComments().size(), 1, "Comment with spaces didn't add.");
                assertEquals(folderDetailsPage.getComments().get(0), "             ", "Comment with spaces didn't correct.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14919() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14919()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(2000);
                folderDetailsPage.addComment(text);
                assertEquals(folderDetailsPage.getComments().size(), 1, "Too long comment didn't add.");
                assertEquals(folderDetailsPage.getComments().get(0), text, "Comment didn't add correctly.");
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Some wrong with added comment");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14920() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14920()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = "\"|{}+_)(*&^%$#@! <>?~ *";
                folderDetailsPage.addComment(text);
                assertEquals(folderDetailsPage.getComments().size(), 1, "WildCard comment didn't add.");
                assertEquals(folderDetailsPage.getComments().get(0), text, "Comment didn't add correctly.");
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Some wrong with added comment");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14921() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14921()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = "désir Bedürfnis è il あなたの名前は何ですか ¿Cuál";
                folderDetailsPage.addComment(text);
                assertEquals(folderDetailsPage.getComments().size(), 1, "Native comment didn't add.");
                assertEquals(folderDetailsPage.getComments().get(0), text, "Comment didn't add correctly.");
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Some wrong with added comment");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14922() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14922()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                folderDetailsPage.addComment(text);

                EditCommentForm editCommentForm = folderDetailsPage.clickEditCommentButton(text);
                assertTrue(editCommentForm.isDisplay(), "Comment form didn't display.");
                assertTrue(editCommentForm.isAvatarDisplay(), "User avatar didn't display.");
                assertTrue(editCommentForm.isButtonsEnable(), "Save or Cancel button didn't enable.");

                TinyMceEditor tinyMceEditor = editCommentForm.getTinyMceEditor();
                tinyMceEditor.clickTextFormatter(BOLD_EDIT);

                editCommentForm.clickCancelButton();
                assertFalse(editCommentForm.isDisplay(), "Comment form didn't closed.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14923() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14923()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't add.");
                String timeCreation = folderDetailsPage.getCommentChangeTime(text);

                String newText = getRandomString(8);
                folderDetailsPage.editComment(text, newText);
                folderDetailsPage.saveEditComments();
                assertTrue(folderDetailsPage.isCommentCorrect(newText), "Changed comment didn't correct.");

                String timeChange = folderDetailsPage.getCommentChangeTime(newText);
                assertNotEquals(timeChange, timeCreation, "Comment changed time didn't modify.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14924() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14924()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't add.");

                String newText = "";
                folderDetailsPage.editComment(text, newText);
                folderDetailsPage.saveEditComments();
                assertTrue(folderDetailsPage.isErrorBalloonMessageDisplay(), "Information message about error didn't show");
                assertTrue(new EditCommentForm(drone).isDisplay(), "After error Edit comment form didn't display.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14925() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14925()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't add.");

                String newText = "!@#$%^&*()_+|/?.,<>:;\"`=-{}[]";
                folderDetailsPage.editComment(text, newText);
                folderDetailsPage.saveEditComments();
                assertFalse(new EditCommentForm(drone).isDisplay(), "Edit Comment... form didn't hidden");
                assertTrue(folderDetailsPage.isCommentCorrect(newText), "Changed comment didn't correct.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14926() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14926()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't add.");

                String newText = "ésir Bedürfnis è il あなたの名前は何ですか ¿Cuál";
                folderDetailsPage.editComment(text, newText);
                folderDetailsPage.saveEditComments();
                assertFalse(new EditCommentForm(drone).isDisplay(), "Edit Comment... form didn't hidden");
                assertTrue(folderDetailsPage.isCommentCorrect(newText), "Changed comment didn't correct.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14927() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14927()
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);

                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't add.");
                assertTrue(folderDetailsPage.checkConfirmDeleteForm(text), "DeleteConfirmForm didn't correct.");

                folderDetailsPage.cancelDeleteComment();
                assertEquals(folderDetailsPage.getComments().size(), 1, "Comment deleted.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14928() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14928() throws Exception
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                folderDetailsPage.addComment(text);
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't add.");

                drone.refresh();
                int commentCount = folderDetailsPage.getCommentCount();
                folderDetailsPage.removeComment(text);
                drone.refresh();
                assertEquals(folderDetailsPage.getCommentCount(), commentCount - 1, "Comments count didn't changed after remove.");

                List<String> comments = getRandomListString(21, 5);
                for (String comment : comments)
                {
                        folderDetailsPage.addComment(comment);
                }
                drone.refresh();
                commentCount = folderDetailsPage.getCommentCount();
                PaginationForm pagination = folderDetailsPage.getCommentsPagination();
                pagination.clickOnPaginationPage(3);
                folderDetailsPage.removeComment(comments.get(0));
                Thread.sleep(500);
                assertEquals(pagination.getCurrentPageNumber(), 2, "User didn't see previous page.");

                drone.refresh();
                assertEquals(folderDetailsPage.getCommentCount(), commentCount - 1, "Comments count didn't changed after remove.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14929() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);
                List<String> comments = getRandomListString(21, 5);
                for (String comment : comments)
                {
                        folderDetailsPage.addComment(comment);
                }
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14929() throws Exception
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                PaginationForm pagination = folderDetailsPage.getCommentsPagination();

                pagination.clickNext();
                assertEquals(pagination.getPaginationInfo(), "11 - 20 of 21", "Comments page didn't changed.");

                pagination.clickPrevious();
                assertEquals(pagination.getPaginationInfo(), "1 - 10 of 21", "Comments page didn't changed.");

                pagination.clickOnPaginationPage(3);
                assertEquals(pagination.getPaginationInfo(), "21 - 21 of 21", "Comments page didn't changed.");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14930() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14930() throws Exception
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                AddCommentForm commentForm = folderDetailsPage.clickAddCommentButton();
                TinyMceEditor tinyMceEditor = commentForm.getTinyMceEditor();
                tinyMceEditor.setText(text);

                tinyMceEditor.clickTextFormatter(BOLD);
                assertEquals(tinyMceEditor.getContent(), String.format("<p><b>%s</b></p>", text), "The text didn't mark as bold.");

                tinyMceEditor.clickTextFormatter(ITALIC);
                assertEquals(tinyMceEditor.getContent(), String.format("<p><i><b>%s</b></i></p>", text), "The text didn't italic.");

                tinyMceEditor.clickTextFormatter(BULLET);
                assertEquals(tinyMceEditor.getContent(), String.format("<ul><li><i><b>%s</b></i></li></ul>", text), "List didn't display.");

                tinyMceEditor.clickTextFormatter(NUMBER);
                assertEquals(tinyMceEditor.getContent(), String.format("<ol><li><i><b>%s</b></i></li></ol>", text), "Numbered list didn't display.");

                tinyMceEditor.clickColorCode(TinyMceColourCode.BLUE);
                assertEquals(tinyMceEditor.getColourAttribute(), "BLUE", "The text didn't highlight with any color.");

                commentForm.clickAddCommentButton();
                assertTrue(folderDetailsPage.isCommentCorrect(text), "Comment didn't create");
        }

        @Test(groups = "DataPrepFolderDetailsCommentsTest")
        public void dataPrep_AONE_14931() throws Exception
        {
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);
                precondition(user, siteName, folderName);
        }

        @Test(groups = { "Comments", "Enterprise4.2" }, timeOut = 400000)
        public void AONE_14931() throws Exception
        {
                /** Test Data Setup */
                String testName = getTestName();
                String user = getUserNameFreeDomain(testName);
                String siteName = getSiteName(testName);
                String folderName = getFolderName(testName);

                ShareUser.login(drone, user, DEFAULT_PASSWORD);
                ShareUser.openSitesDocumentLibrary(drone, siteName);
                FolderDetailsPage folderDetailsPage = ShareUser.openFolderDetailPage(drone, folderName);

                String text = getRandomString(5);
                RepositoryPage repositoryPage = folderDetailsPage.navigateToParentFolder();
                FileDirectoryInfo folderDirectoryInfo = repositoryPage.getFileDirectoryInfo(folderName);
                folderDetailsPage = (FolderDetailsPage) folderDirectoryInfo.clickCommentsLink();
                int commentCount = folderDetailsPage.getCommentCount();
                folderDetailsPage.addComment(text);
                drone.refresh();
                assertEquals(folderDetailsPage.getCommentCount(), commentCount + 1, "Comments count didn't changed after comment add.");

                repositoryPage = folderDetailsPage.navigateToParentFolder();
                folderDirectoryInfo = repositoryPage.getFileDirectoryInfo(folderName);
                assertEquals(folderDirectoryInfo.getCommentsCount(), commentCount + 1, "Comments count didn't changed after comment add.");

                folderDetailsPage = folderDirectoryInfo.selectViewFolderDetails();
                folderDetailsPage.removeComment(text);
                drone.refresh();
                assertEquals(folderDetailsPage.getCommentCount(), commentCount, "Comments count didn't changed after comment delete.");
        }

        @AfterMethod(alwaysRun = true)
        public void logout()
        {
                ShareUser.logout(drone);
                logger.info("User logged out - drone.");
        }


}
