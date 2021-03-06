/*
 * Copyright (C) 2005-2015 Alfresco Software Limited. This file is part of Alfresco Alfresco is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Alfresco is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General
 * Public License along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.po.share.repository;

import java.io.File;
import java.util.Map;

import org.alfresco.po.share.AlfrescoVersion;
import org.alfresco.po.share.RepositoryPage;
import org.alfresco.po.share.site.UploadFilePage;
import org.alfresco.po.share.site.document.AbstractDocumentTest;
import org.alfresco.po.share.site.document.ContentDetails;
import org.alfresco.po.share.site.document.DocumentDetailsPage;
import org.alfresco.po.share.site.document.EditDocumentPropertiesPage;
import org.alfresco.po.share.site.document.FileDirectoryInfo;
import org.alfresco.po.share.steps.AdminActions;
import org.alfresco.po.share.util.SiteUtil;
import org.alfresco.test.FailedTestListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Integration test to verify Repo > Models page is operating correctly.
 *
 * @author Meenal Bhave
 * @since 5.0.2
 */
@Listeners(FailedTestListener.class)
public class ModelsPageTest extends AbstractDocumentTest
{
    
    private final Log logger = LogFactory.getLog(this.getClass());
    
    private static RepositoryPage repoPage;
    
    private static AdminActions adminActions = new AdminActions();
    
    private String modelName;
    private File modelFileDraft;
    private File modelFileToActivate;
    private String activeModel;
    private ContentDetails modelFileContents = new ContentDetails();

    /**
     * Pre test setup of a dummy file to upload.
     *
     * @throws Exception
     */
    @BeforeClass(groups={"alfresco-one"})
    public void prepare() throws Exception
    {
        modelName = "1model" + System.currentTimeMillis();
        loginAs(drone, shareUrl, username, password).render();                     
        modelFileDraft = SiteUtil.prepareFile(modelName,  "<xml>modelName</xml>", ".xml");
        modelFileContents.setName(modelName);
        
        modelFileToActivate = SiteUtil.prepareFile("Alfresco456");
        activeModel = "NewModel";
    }

    @AfterClass(groups={"alfresco-one"})
    public void teardown()
    {
        
    }
    
    /**
     * Test navigating to model works correctly for Ent and Cloud AlfrescoVersions
     *
     * @throws Exception
     */
    @Test(enabled = true, groups = "alfresco-one", priority = 1)
    public void testNavigateToModels() throws Exception
    {               
        try
        {
            ModelsPage modelsPage = adminActions.openRepositoryModelsPage(drone).render();
            
            modelsPage = modelsPage.getNavigation().selectSimpleView().render();
            
            Assert.assertNotNull(modelsPage);  

            UploadFilePage uploadForm = modelsPage.getNavigation().selectFileUpload().render();
            uploadForm.uploadFile(modelFileDraft.getCanonicalPath()).render();
        }        
        catch(UnsupportedOperationException uop)
        {
            // For Cloud, expected UnsupportedOperationException
            Assert.assertEquals(drone.getProperties().getVersion(), AlfrescoVersion.MyAlfresco);
        }        
    }

    @Test(dependsOnMethods="testNavigateToModels", groups = "alfresco-one", priority = 2)
    public void testSimpleView() throws Exception
    {
        ModelsPage modelsPage = adminActions.openRepositoryModelsPage(drone).render();
        
        FileDirectoryInfo file = modelsPage.getFileDirectoryInfo(modelFileDraft.getName());
        Assert.assertNotNull(file);
        Assert.assertEquals(file.getName(), modelFileDraft.getName());
        Assert.assertTrue(file.isModelInfoPresent());
        Assert.assertFalse(file.isModelActive());
    }
    
    @Test(groups = "alfresco-one", priority = 3)
    public void testDetailedView() throws Exception
    {        
        ModelsPage modelsPage = getModelsPage();
        modelsPage = modelsPage.getNavigation().selectDetailedView().render();
        
        FileDirectoryInfo file = modelsPage.getFileDirectoryInfo(modelFileDraft.getName());
        Assert.assertNotNull(file);
        Assert.assertEquals(file.getName(), modelFileDraft.getName());
        Assert.assertTrue(file.isModelInfoPresent());
        Assert.assertFalse(file.isModelActive());

// TODO: This is to be uncommented when capability to activate a model is implemented and there is an active model in the system  
//        file = modelsPage.getFileDirectoryInfo(activeModel);
//        Assert.assertNotNull(file);
//        Assert.assertTrue(file.isModelInfoPresent());
//        Assert.assertTrue(file.isModelActive());
//        Assert.assertNotNull(file.getModelName());
//        Assert.assertNotNull(file.getModelDesription());
       
    }
    
    @Test(groups = "alfresco-one", priority = 4, expectedExceptions=UnsupportedOperationException.class)
    public void testTableView() throws Exception
    {        
        ModelsPage modelsPage = getModelsPage();
        modelsPage = modelsPage.getNavigation().selectTableView().render();
        
        FileDirectoryInfo file = modelsPage.getFileDirectoryInfo(modelFileDraft.getName());
        Assert.assertNotNull(file);
        Assert.assertEquals(file.getName(), modelFileDraft.getName());
        Assert.assertFalse(file.isModelInfoPresent());
        Assert.assertFalse(file.isModelActive());
    }
    
    @Test(groups = "alfresco-one", priority = 5, expectedExceptions=UnsupportedOperationException.class)
    public void testGalleryView() throws Exception
    {        
        ModelsPage modelsPage = getModelsPage();
        modelsPage = modelsPage.getNavigation().selectGalleryView().render();
        
        FileDirectoryInfo file = modelsPage.getFileDirectoryInfo(modelFileDraft.getName());
        Assert.assertNotNull(file);
        Assert.assertEquals(file.getName(), modelFileDraft.getName());
        Assert.assertFalse(file.isModelInfoPresent());
        Assert.assertFalse(file.isModelActive());        
    }
    
    @Test(groups = "alfresco-one", priority = 6, expectedExceptions=UnsupportedOperationException.class)
    public void testFilmStripView() throws Exception
    {        
        ModelsPage modelsPage = getModelsPage();
        modelsPage = modelsPage.getNavigation().selectFilmstripView().render();
        
        FileDirectoryInfo file = modelsPage.getFileDirectoryInfo(modelFileDraft.getName());
        Assert.assertNotNull(file);
        Assert.assertEquals(file.getName(), modelFileDraft.getName());
        Assert.assertFalse(file.isModelInfoPresent());
        Assert.assertFalse(file.isModelActive());
    }
    
    @Test(groups = "alfresco-one", priority = 7, expectedExceptions=UnsupportedOperationException.class)
    public void testNotModelsPage() throws Exception
    {        
        repoPage = adminActions.openRepositoryDataDictionaryPage(drone).render();
        repoPage = repoPage.getNavigation().selectDetailedView().render();
        
        FileDirectoryInfo file = repoPage.getFileDirectoryInfo("Messages");
        Assert.assertNotNull(file);
        Assert.assertFalse(file.isModelInfoPresent());
        Assert.assertFalse(file.isModelActive());
    }
    
    @Test(groups = "alfresco-one", priority = 8)
    public void testEditPropertiesPopup() throws Exception
    {        
        ModelsPage modelsPage = adminActions.openRepositoryModelsPage(drone).render();
        modelsPage = modelsPage.getNavigation().selectDetailedView().render();
        
        Assert.assertNotNull(modelsPage);
        
        FileDirectoryInfo file = modelsPage.getFileDirectoryInfo(modelFileDraft.getName());
        Assert.assertNotNull(file);
        
        EditDocumentPropertiesPage editPropPage = file.selectEditProperties().render();
        Assert.assertFalse(editPropPage.isModelActive());
        
        editPropPage.setModelActive();
        Assert.assertTrue(editPropPage.isModelActive());
        
        editPropPage.setModelActive();
        Assert.assertFalse(editPropPage.isModelActive());    
        
        modelsPage = editPropPage.selectCancel().render();
        Assert.assertNotNull(modelsPage);
    }
    
    @Test(groups = "alfresco-one", priority = 9)
    public void testModelDetailsPage() throws Exception
    {        
        ModelsPage modelsPage = getModelsPage();        
        Assert.assertNotNull(modelsPage);
        
        DocumentDetailsPage modelDetails = modelsPage.selectFile(modelFileDraft.getName()).render();
        Assert.assertNotNull(modelDetails);
        
        Map<String, Object> modelProps = modelDetails.getProperties();
        Assert.assertEquals(modelProps.get("ModelActive"), "No");                
    }
    
    private ModelsPage getModelsPage() throws Exception
    {
        ModelsPage modelsPage = drone.getCurrentPage().render();
        modelsPage = modelsPage.getNavigation().selectSimpleView().render();
        return modelsPage;
    }
}