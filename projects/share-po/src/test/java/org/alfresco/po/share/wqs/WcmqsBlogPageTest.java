package org.alfresco.po.share.wqs;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.alfresco.po.share.AbstractTest;
import org.alfresco.po.share.DashBoardPage;
import org.alfresco.po.share.dashlet.SiteWebQuickStartDashlet;
import org.alfresco.po.share.dashlet.WebQuickStartOptions;
import org.alfresco.po.share.enums.Dashlets;
import org.alfresco.po.share.site.CreateSitePage;
import org.alfresco.po.share.site.CustomiseSiteDashboardPage;
import org.alfresco.po.share.site.SiteDashboardPage;
import org.alfresco.po.share.site.SitePage;
import org.alfresco.po.share.site.document.DocumentLibraryPage;
import org.alfresco.po.share.site.document.EditDocumentPropertiesPage;
import org.alfresco.test.FailedTestListener;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
/**
 * Created by rdorobantu on 1/21/2015.
 */
@Listeners(FailedTestListener.class)
public class WcmqsBlogPageTest extends AbstractTest
{
    private static final Logger logger = Logger.getLogger(WcmqsBlogPageTest.class);
    DashBoardPage dashBoard;
    private String wqsURL;
    private String siteName;
    private String ipAddress;

    @BeforeClass(alwaysRun = true)
    public void prepare() throws Exception
    {
        String testName = this.getClass().getSimpleName();
        siteName = testName;

        String hostName = (shareUrl).replaceAll(".*\\//|\\:.*", "");
        try
        {
            ipAddress = InetAddress.getByName(hostName).toString().replaceAll(".*/", "");
            logger.info("Ip address from Alfresco server was obtained");
        }
        catch (UnknownHostException | SecurityException e)
        {
            logger.error("Ip address from Alfresco server could not be obtained");
        }

        wqsURL = siteName + ":8080/wcmqs";
        logger.info(" wcmqs url : " + wqsURL);
        logger.info("Start Tests from: " + testName);

        // WCM Quick Start is installed; - is not required to be executed automatically
        int columnNumber = 2;
        String SITE_WEB_QUICK_START_DASHLET = "site-wqs";
        dashBoard = loginAs(username, password);

        // Site is created in Alfresco Share;
        CreateSitePage createSitePage = dashBoard.getNav().selectCreateSite().render();
        SitePage site = createSitePage.createNewSite(siteName).render();

        // WCM Quick Start Site Data is imported;
        CustomiseSiteDashboardPage customiseSiteDashboardPage = site.getSiteNav().selectCustomizeDashboard().render();
        SiteDashboardPage siteDashboardPage = customiseSiteDashboardPage.addDashlet(Dashlets.WEB_QUICK_START, columnNumber);
        SiteWebQuickStartDashlet wqsDashlet = siteDashboardPage.getDashlet(SITE_WEB_QUICK_START_DASHLET).render();
        wqsDashlet.selectWebsiteDataOption(WebQuickStartOptions.FINANCE);
        wqsDashlet.clickImportButtton();
        wqsDashlet.waitForImportMessage();

        // Change property for quick start to sitename
        DocumentLibraryPage documentLibraryPage = site.getSiteNav().selectSiteDocumentLibrary().render();
        documentLibraryPage.selectFolder("Alfresco Quick Start");
        EditDocumentPropertiesPage documentPropertiesPage = documentLibraryPage.getFileDirectoryInfo("Quick Start Editorial").selectEditProperties()
            .render();
        documentPropertiesPage.setSiteHostname(siteName);
        documentPropertiesPage.clickSave();

        // Change property for quick start live to ip address
        documentLibraryPage.getFileDirectoryInfo("Quick Start Live").selectEditProperties().render();
        documentPropertiesPage.setSiteHostname(ipAddress);
        documentPropertiesPage.clickSave();

        // setup new entry in hosts to be able to access the new wcmqs site
        String setHostAddress = "cmd.exe /c echo. >> %WINDIR%\\System32\\Drivers\\Etc\\hosts && echo " + ipAddress + " " + siteName
            + " >> %WINDIR%\\System32\\Drivers\\Etc\\hosts";
        Runtime.getRuntime().exec(setHostAddress);

    }

    @AfterClass
    public void tearDown()
    {
        logout(drone);
    }

    @Test
    public void testOpenBlogPost()
    {
        drone.navigateTo(wqsURL);
        String blogMenu = "blog";
        WcmqsHomePage wqsPage = new WcmqsHomePage(drone);
        wqsPage.selectMenu(blogMenu);
        WcmqsBlogPage blogPage = new WcmqsBlogPage(drone);
        String blogTitle = "Ethical funds";
        blogPage.openBlogPost(blogTitle);
        WcmqsBlogPostPage blogPostPage = new WcmqsBlogPostPage(drone);
        Assert.assertEquals(blogPostPage.getTitle(), blogTitle);

    }

    @Test
    public void testIsBlogDisplayed()
    {
        drone.navigateTo(wqsURL);
        String blogMenu = "blog";
        WcmqsHomePage wqsPage = new WcmqsHomePage(drone);
        wqsPage.selectMenu(blogMenu);
        WcmqsBlogPage blogPage = new WcmqsBlogPage(drone);
        String blogTitle = "Ethical funds";
        Assert.assertTrue(blogPage.isBlogDisplayed(blogTitle), "Blog is not displayed.");
    }

    @Test
    public void testIsBlogPostDateDisplayed()
    {
        drone.navigateTo(wqsURL);
        String blogMenu = "blog";
        WcmqsHomePage wqsPage = new WcmqsHomePage(drone);
        wqsPage.selectMenu(blogMenu);
        WcmqsBlogPage blogPage = new WcmqsBlogPage(drone);
        Assert.assertTrue(blogPage.isBlogPostDateDisplayed(), "Date is not displayed.");
    }

    @Test
    public void testIsBlogPostCreatorDisplayed()
    {
        drone.navigateTo(wqsURL);
        String blogMenu = "blog";
        WcmqsHomePage wqsPage = new WcmqsHomePage(drone);
        wqsPage.selectMenu(blogMenu);
        WcmqsBlogPage blogPage = new WcmqsBlogPage(drone);
        Assert.assertTrue(blogPage.isBlogPostCreatorDisplayed(), "Creator of the blog is not displayed.");
    }

    @Test
    public void testIsBlogPostCommentsLinkDisplayed()
    {
        drone.navigateTo(wqsURL);
        String blogMenu = "blog";
        WcmqsHomePage wqsPage = new WcmqsHomePage(drone);
        wqsPage.selectMenu(blogMenu);
        WcmqsBlogPage blogPage = new WcmqsBlogPage(drone);
        Assert.assertTrue(blogPage.isBlogPostCommentsLinkDisplayed(), "Blog comments are not displayed.");
    }

    @Test
    public void testClickReadMoreByBlog()
    {
        drone.navigateTo(wqsURL);
        String blogMenu = "blog";
        WcmqsHomePage wqsPage = new WcmqsHomePage(drone);
        wqsPage.selectMenu(blogMenu);
        WcmqsBlogPage blogPage = new WcmqsBlogPage(drone);
        String blogTitle = "Ethical funds";
        blogPage.clickReadMoreByBlog(blogTitle);
        WcmqsBlogPostPage blogPostPage = new WcmqsBlogPostPage(drone);
        Assert.assertEquals(blogPostPage.getTitle(), blogTitle);
    }
}
