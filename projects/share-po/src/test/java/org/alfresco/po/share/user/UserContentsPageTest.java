package org.alfresco.po.share.user;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.po.share.AbstractTest;
import org.alfresco.po.share.AlfrescoVersion;
import org.alfresco.po.share.DashBoardPage;
import org.alfresco.po.share.FactorySharePage;
import org.alfresco.po.share.ShareUtil;
import org.alfresco.po.share.site.SitePage;
import org.alfresco.po.share.site.UploadFilePage;
import org.alfresco.po.share.site.document.DocumentDetailsPage;
import org.alfresco.po.share.site.document.DocumentLibraryPage;
import org.alfresco.po.share.site.document.EditDocumentPropertiesPage;
import org.alfresco.po.share.util.SiteUtil;
import org.alfresco.test.FailedTestListener;
import org.alfresco.webdrone.WebDrone;
import org.alfresco.webdrone.exception.PageException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Verify user content page
 * 
 * @author Bogdan.Bocancea
 */
@Listeners(FailedTestListener.class)
public class UserContentsPageTest extends AbstractTest
{

    private String siteName1;
    private String userName;
    private AlfrescoVersion version;

    private DashBoardPage dashboardPage;
    private MyProfilePage myprofile;
    private UserContentPage userContentPage;
    private UserContentItems addedContent;
    private UserContentItems modifiedContent;
    private DocumentDetailsPage detailsPage;
    private File testFile;
    private String fileName;
    private static DocumentLibraryPage documentLibPage;

    /**
     * Pre test to create a content with properties set.
     * 
     * @throws Exception
     */
    @BeforeClass(groups = { "alfresco-one" })
    public void prepare() throws Exception
    {
        siteName1 = "UserContentPage" + System.currentTimeMillis();
        userName = "UserContentPage" + System.currentTimeMillis();

        version = drone.getProperties().getVersion();
        if (version.isCloud())
        {
            ShareUtil.loginAs(drone, shareUrl, username, password).render();
        }
        else
        {
            createEnterpriseUser(userName);
            ShareUtil.loginAs(drone, shareUrl, userName, UNAME_PASSWORD).render();
        }

        dashboardPage = FactorySharePage.resolvePage(drone).render();
        myprofile = dashboardPage.getNav().selectMyProfile().render();
        userContentPage = myprofile.getProfileNav().selectContent();
    }

    @AfterClass(groups = { "alfresco-one" })
    public void tearDown()
    {
        SiteUtil.deleteSite(drone, siteName1);
    }

    @Test(groups = { "alfresco-one" })
    public void getContentNoContent()
    {
        assertTrue(userContentPage.isNoContentAddMessagePresent());
        assertTrue(userContentPage.isNoContentModifiedMessagePresent());
    }

    @Test(dependsOnMethods = "getContentNoContent", groups = { "alfresco-one", "TestBug" })
    public void getContents() throws IOException
    {
        SiteUtil.createSite(drone, siteName1, "description", "Public");

        testFile = SiteUtil.prepareFile();
        StringTokenizer st = new StringTokenizer(testFile.getName(), ".");
        fileName = st.nextToken();
        File file = SiteUtil.prepareFile();
        fileName = file.getName();
        SitePage site = drone.getCurrentPage().render();
        documentLibPage = site.getSiteNav().selectSiteDocumentLibrary().render();

        UploadFilePage upLoadPage = documentLibPage.getNavigation().selectFileUpload().render();
        documentLibPage = upLoadPage.uploadFile(file.getCanonicalPath()).render();
        documentLibPage = documentLibPage.getNavigation().selectDetailedView().render();

        // edit the document
        documentLibPage = documentLibPage.getFileDirectoryInfo(fileName).selectEditOffline().render();
        assertTrue(documentLibPage.getFileDirectoryInfo(fileName).isEdited());

        // cancel edit
        documentLibPage = documentLibPage.getFileDirectoryInfo(fileName).selectCancelEditing().render();
        assertFalse(documentLibPage.getFileDirectoryInfo(fileName).isEdited());

        // navigate to my profile -> Content
        myprofile = dashboardPage.getNav().selectMyProfile().render();
        userContentPage = myprofile.getProfileNav().selectContent().render();

        userContentPage = contentRefreshRetry(drone, userContentPage, fileName);

        // verify if the Added Content is present
        List<UserContentItems> userContentsAdded = userContentPage.getContentAdded();
        List<String> contentAddedNames = new ArrayList<String>(2);
        contentAddedNames.add(fileName);

        assertTrue(userContentsAdded.size() >= 1);
        for (UserContentItems contentAdded : userContentsAdded)
        {
            contentAddedNames.contains(contentAdded.getContentName());
        }

        // verify if the modified content is present
        List<UserContentItems> userContentsMod = userContentPage.getContentModified();
        List<String> contentModNames = new ArrayList<String>(2);
        contentModNames.add(fileName);

        assertTrue(userContentsMod.size() >= 1);
        for (UserContentItems contentMod : userContentsMod)
        {
            contentModNames.contains(contentMod.getContentName());
        }
    }

    @Test(dependsOnMethods = "getContents", groups = { "alfresco-one", "TestBug" })
    public void getAddedContentName()
    {
        List<UserContentItems> contentItems = userContentPage.getContentAdded(fileName);
        Assert.assertTrue(contentItems.size() == 1, "There should be only one content with name :" + fileName);
        addedContent = contentItems.get(0);
        assertEquals(addedContent.getContentName(), fileName);
    }

    @Test(dependsOnMethods = "getContents", groups = { "alfresco-one", "TestBug" })
    public void getModifiedContentName()
    {
        List<UserContentItems> contentItemsMod = userContentPage.getContentModified(fileName);
        Assert.assertTrue(contentItemsMod.size() == 1, "There should be only one content with name :" + fileName);
        modifiedContent = contentItemsMod.get(0);
        assertEquals(modifiedContent.getContentName(), fileName);
    }

    @Test(dependsOnMethods = "getAddedContentName", groups = { "alfresco-one", "TestBug" })
    public void clickContentAdded()
    {
        detailsPage = addedContent.clickOnContentName().render();
        assertTrue(detailsPage.isSite(siteName1));
        Assert.assertTrue(detailsPage.isTitlePresent(siteName1));

        EditDocumentPropertiesPage editPage = detailsPage.selectEditProperties().render();
        Assert.assertTrue(editPage.isEditPropertiesVisible());
        Assert.assertEquals(editPage.getName(), fileName);
    }

    @Test(dependsOnMethods = "getModifiedContentName", groups = { "alfresco-one", "TestBug" })
    public void clickcontentModified()
    {
        // navigate to my profile -> Content
        MyProfilePage myprofile1 = dashboardPage.getNav().selectMyProfile().render();
        UserContentPage userContentPage1 = myprofile1.getProfileNav().selectContent().render();

        List<UserContentItems> contentItemsMod = userContentPage1.getContentModified(fileName);
        modifiedContent = contentItemsMod.get(0);

        detailsPage = modifiedContent.clickOnContentName().render();
        assertTrue(detailsPage.isSite(siteName1));
        Assert.assertTrue(detailsPage.isTitlePresent(siteName1));

        EditDocumentPropertiesPage editPage = detailsPage.selectEditProperties().render();
        Assert.assertTrue(editPage.isEditPropertiesVisible());
        Assert.assertEquals(editPage.getName(), fileName);
    }

    /**
     * Searching with retry for content
     * 
     * @param contentPage
     * @param contentName
     * @return contentPage
     */
    public static UserContentPage contentRefreshRetry(WebDrone drone, UserContentPage contentPage, String contentName)
    {
        int counter = 0;
        int waitInMilliSeconds = 2000;
        int retryRefreshCount = 5;
        while (counter < retryRefreshCount)
        {
            List<UserContentItems> contentSearchResults = contentPage.getContentAdded();
            for (UserContentItems userContentItems : contentSearchResults)
            {
                if (userContentItems.getContentName().equalsIgnoreCase(contentName))
                {
                    return contentPage;
                }
            }
            counter++;
            drone.refresh();
            contentPage = drone.getCurrentPage().render();
            // double wait time to not over do solr search
            waitInMilliSeconds = (waitInMilliSeconds * 2);
            synchronized (SiteUtil.class)
            {
                try
                {
                    SiteUtil.class.wait(waitInMilliSeconds);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
        throw new PageException("Content search failed");
    }

}
