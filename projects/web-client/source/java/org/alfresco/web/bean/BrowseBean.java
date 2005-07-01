/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.bean;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.IBreadcrumbHandler;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIBreadcrumb;
import org.alfresco.web.ui.common.component.UIModeList;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.renderer.data.RichListRenderer;
import org.alfresco.web.ui.repo.component.UINodeDescendants;
import org.alfresco.web.ui.repo.component.UISimpleSearch;
import org.apache.log4j.Logger;

/**
 * Bean providing properties and behaviour for the main folder/document browse screen and
 * search results screens.
 * 
 * @author Kevin Roast
 */
public class BrowseBean implements IContextListener
{
   /**
    * Default Constructor
    */
   public BrowseBean()
   {
      UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
      
      // TODO: use ConfigService to get default view mode
   }
   
   
   // ------------------------------------------------------------------------------
   // Bean property getters and setters 
   
   /**
    * @return Returns the NodeService.
    */
   public NodeService getNodeService()
   {
      return this.nodeService;
   }

   /**
    * @param nodeService The NodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }
   
   /**
    * @return Returns the Searcher service.
    */
   public SearchService getSearchService()
   {
      return this.searchService;
   }

   /**
    * @param searchService The Searcher to set.
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
   
   /**
    * @return Returns the Lock Service.
    */
   public LockService getLockService()
   {
      return lockService;
   }
   
   /**
    * @param lockService The Lock Service to set.
    */
   public void setLockService(LockService lockService)
   {
      this.lockService = lockService;
   }
   
   /**
    * @return Returns the navigation bean instance.
    */
   public NavigationBean getNavigator()
   {
      return this.navigator;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }
   
   /**
    * @return Returns the browse View mode. See UIRichList
    */
   public String getBrowseViewMode()
   {
      return this.browseViewMode;
   }
   
   /**
    * @param browseViewMode      The browse View mode to set. See UIRichList.
    */
   public void setBrowseViewMode(String browseViewMode)
   {
      this.browseViewMode = browseViewMode;
   }
   
   /**
    * @return Returns the browsePageSize.
    */
   public int getBrowsePageSize()
   {
      return browsePageSize;
   }
   
   /**
    * @param browsePageSize The browsePageSize to set.
    */
   public void setBrowsePageSize(int browsePageSize)
   {
      this.browsePageSize = browsePageSize;
   }
   
   /**
    * @return Returns the Space Node being used for the current browse screen action.
    */
   public Node getActionSpace()
   {
      return this.actionSpace;
   }
   
   /**
    * @param actionSpace     Set the Space Node to be used for the current browse screen action.
    */
   public void setActionSpace(Node actionSpace)
   {
      this.actionSpace = actionSpace;
   }
   
   /**
    * @return The document node being used for the current operation
    */
   public Node getDocument()
   {
      return document;
   }

   /**
    * @param document The document node to be used for the current operation
    */
   public void setDocument(Node document)
   {
      this.document = document;
   }

   /**
    * @param contentRichList The contentRichList to set.
    */
   public void setContentRichList(UIRichList browseRichList)
   {
      this.contentRichList = browseRichList;
   }
   
   /**
    * @return Returns the contentRichList.
    */
   public UIRichList getContentRichList()
   {
      return this.contentRichList;
   }
   
   /**
    * @param spacesRichList The spacesRichList to set.
    */
   public void setSpacesRichList(UIRichList detailsRichList)
   {
      this.spacesRichList = detailsRichList;
   }
   
   /**
    * @return Returns the spacesRichList.
    */
   public UIRichList getSpacesRichList()
   {
      return this.spacesRichList;
   }
   
   /**
    * Page accessed bean method to get the container nodes currently being browsed
    * 
    * @return List of container Node objects for the current browse location
    */
   public List<Node> getNodes()
   {
      // the references to container nodes and content nodes are transient for one use only
      // we do this so we only query/search once - as we cannot distinguish between node types
      // until after the query. The logic is a bit confusing but otherwise we would need to
      // perform the same query or search twice for every screen refresh.
      if (this.containerNodes == null)
      {
         if (this.navigator.getSearchContext() == null)
         {
            queryBrowseNodes(this.navigator.getCurrentNodeId());
         }
         else
         {
            searchBrowseNodes(this.navigator.getSearchContext());
         }
      }
      List<Node> result = this.containerNodes;
      this.containerNodes = null;
      
      return result;
   }
   
   /**
    * Page accessed bean method to get the content nodes currently being browsed
    * 
    * @return List of content Node objects for the current browse location
    */
   public List<Node> getContent()
   {
      // see comment in getNodes() above for reasoning here
      if (this.contentNodes == null)
      {
         if (this.navigator.getSearchContext() == null)
         {
            queryBrowseNodes(this.navigator.getCurrentNodeId());
         }
         else
         {
            searchBrowseNodes(this.navigator.getSearchContext());
         }
      }
      List<Node> result = this.contentNodes;
      this.contentNodes = null;
      
      return result;
   }
   
   
   // ------------------------------------------------------------------------------
   // IContextListener implementation 
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
    */
   public void contextUpdated()
   {
      logger.debug("contextUpdated() listener called");
      invalidateComponents();
   }
   
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers 
   
   /**
    * Change the current view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void viewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // set the page size based on the style of display
      if (viewMode.equals(RichListRenderer.DetailsViewRenderer.VIEWMODEID))
      {
         setBrowsePageSize(10);
      }
      else if (viewMode.equals(RichListRenderer.IconViewRenderer.VIEWMODEID))
      {
         setBrowsePageSize(9);
      }
      else if (viewMode.equals(RichListRenderer.ListViewRenderer.VIEWMODEID))
      {
         setBrowsePageSize(10);
      }
      else
      {
         // in-case another view mode appears
         setBrowsePageSize(10);
      }
      if (logger.isDebugEnabled())
         logger.debug("Browse view page size set to: " + getBrowsePageSize());
      
      // push the view mode into the lists
      setBrowseViewMode(viewMode);
   }
   
   
   // ------------------------------------------------------------------------------
   // Helper methods
   
   /**
    * Query a list of nodes for the specified parent node Id
    * 
    * @param parentNodeId     Id of the parent node or null for the root node
    */
   private void queryBrowseNodes(String parentNodeId)
   {
      long startTime = 0;
      if (logger.isDebugEnabled())
         startTime = System.currentTimeMillis();
      
      UserTransaction tx = null;
      try
      {
         FacesContext context = FacesContext.getCurrentInstance();
         tx = Repository.getUserTransaction(context);
         tx.begin();
         
         NodeRef parentRef;
         if (parentNodeId == null)
         {
            // no specific parent node specified - use the root node
            parentRef = this.nodeService.getRootNode(Repository.getStoreRef());
         }
         else
         {
            // build a NodeRef for the specified Id and our store
            parentRef = new NodeRef(Repository.getStoreRef(), parentNodeId);
         }
         
         // TODO: can we improve the Get here with an API call for children of a specific type?
         List<ChildAssociationRef> childRefs = this.nodeService.getChildAssocs(parentRef);
         this.containerNodes = new ArrayList<Node>(childRefs.size());
         this.contentNodes = new ArrayList<Node>(childRefs.size());
         for (ChildAssociationRef ref: childRefs)
         {
            // create our Node representation from the NodeRef
            NodeRef nodeRef = ref.getChildRef();
            
            // find it's type so we can see if it's a node we are interested in
            QName type = this.nodeService.getType(nodeRef);
            
            // look for Space or File nodes
            if (type.equals(ContentModel.TYPE_FOLDER))
            {
               // TODO: Build a specific impl of a MapNode - one that matches certain props
               //       to return them dynamically - this is to reduce pulling back the entire
               //       set of props and aspects for all nodes - even if they are not displayed!
               //       Will this help? As we need to get at least Name etc. for sorting purposes,
               //       if the props are always needed then it's better to get them here. At least
               //       the aspects are not always requried - e.g. only need lock info if visible.
               
               // create our Node representation
               MapNode node = new MapNode(nodeRef, this.nodeService, true);
               
               this.containerNodes.add(node);
            }
            else if (type.equals(ContentModel.TYPE_CONTENT))
            {
               // create our Node representation
               MapNode node = new MapNode(nodeRef, this.nodeService, true);
               
               setupDataBindingProperties(node);
               
               this.contentNodes.add(node);
            }
         }
         
         // commit the transaction
         tx.commit();
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_NODEREF, new Object[] {parentNodeId}) );
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (Exception err)
      {
         Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_GENERIC, err.getMessage()), err );
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to query and build map nodes: " + (endTime - startTime) + "ms");
      }
   }
   
   /**
    * Search for a list of nodes using the specific search context
    * 
    * @param searchContext    To use to perform the search
    */
   private void searchBrowseNodes(SearchContext searchContext)
   {
      long startTime = 0;
      if (logger.isDebugEnabled())
         startTime = System.currentTimeMillis();
      
      // get the searcher object and perform the search of the root node
      String query = searchContext.buildQuery();
      
      UserTransaction tx = null;
      try
      {
         tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
         tx.begin();
         
         if (logger.isDebugEnabled())
            logger.debug("Searching using query: " + query);
         ResultSet results = this.searchService.query(
               Repository.getStoreRef(), 
               "lucene", query, null, null);
         if (logger.isDebugEnabled())
            logger.debug("Search results returned: " + results.length());
         
         // create a list of items from the results
         this.containerNodes = new ArrayList<Node>(results.length());
         this.contentNodes = new ArrayList<Node>(results.length());
         if (results.length() != 0)
         {
            for (ResultSetRow row: results)
            {
               NodeRef nodeRef = row.getNodeRef();
               
               // find it's type so we can see if it's a node we are interested in
               QName type = this.nodeService.getType(nodeRef);
               
               // look for Space or File nodes
               if (type.equals(ContentModel.TYPE_FOLDER))
               {
                  // create our Node representation
                  MapNode node = new MapNode(nodeRef, this.nodeService, true);
                  
                  // construct the path to this Node
                  node.addPropertyResolver("displayPath", this.resolverDisplayPath);
                  
                  this.containerNodes.add(node);
               }
               else if (type.equals(ContentModel.TYPE_CONTENT))
               {
                  // create our Node representation
                  MapNode node = new MapNode(nodeRef, this.nodeService, true);
                  
                  setupDataBindingProperties(node);
                  
                  // construct the path to this Node
                  node.addPropertyResolver("displayPath", this.resolverDisplayPath);
                  
                  this.contentNodes.add(node);
               }
            }
         }
         
         // commit the transaction
         tx.commit();
      }
      catch (Exception err)
      {
         logger.info("Search failed for: " + query);
         Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_SEARCH, new Object[] {err.getMessage()}), err );
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to query and build map nodes: " + (endTime - startTime) + "ms");
      }
   }
   
   /**
    * Setup the additional properties required at data-binding time.
    * <p>
    * These are properties used by components on the page when iterating over the nodes.
    * Information such as whether the node is locked, a working copy, download URL etc.
    * <p>
    * We use a set of annoymous inner classes to provide the implemention for the property
    * getters. The interfaces are only called when the properties are first required. 
    * 
    * @param node       MapNode to add the properties too
    */
   private void setupDataBindingProperties(MapNode node)
   {
      // special properties to be used by the value binding components on the page
      node.addPropertyResolver("locked", this.resolverlocked);
      node.addPropertyResolver("workingCopy", this.resolverWorkingCopy);
      node.addPropertyResolver("url", this.resolverUrl);
      node.addPropertyResolver("fileType16", this.resolverFileType16);
      node.addPropertyResolver("fileType32", this.resolverFileType32);
   }
   
   private NodePropertyResolver resolverlocked = new NodePropertyResolver() {
      public Object get(MapNode node) {
         return Repository.isNodeLocked(node, lockService);
      }
   };
   
   private NodePropertyResolver resolverWorkingCopy = new NodePropertyResolver() {
      public Object get(MapNode node) {
         return node.hasAspect(ContentModel.ASPECT_WORKING_COPY);
      }
   };
   
   private NodePropertyResolver resolverUrl = new NodePropertyResolver() {
      public Object get(MapNode node) {
         return DownloadContentServlet.generateDownloadURL(node.getNodeRef(), node.getName());
      }
   };
   
   private NodePropertyResolver resolverFileType16 = new NodePropertyResolver() {
      public Object get(MapNode node) {
         return Repository.getFileTypeImage(node, true);
      }
   };
   
   private NodePropertyResolver resolverFileType32 = new NodePropertyResolver() {
      public Object get(MapNode node) {
         return Repository.getFileTypeImage(node, false);
      }
   };
   
   private NodePropertyResolver resolverDisplayPath = new NodePropertyResolver() {
      public Object get(MapNode node) {
         return Repository.getDisplayPath( nodeService.getPath(node.getNodeRef()) );
      }
   };
   
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers
   
   /**
    * Action called from the Simple Search component.
    * Sets up the SearchContext object with the values from the simple search menu.
    */
   public void search(ActionEvent event)
   {
      // setup the search text string on the top-level navigation handler
      UISimpleSearch search = (UISimpleSearch)event.getComponent();
      this.navigator.setSearchContext(search.getSearchContext());
      
      navigateBrowseScreen();
   }
   
   /**
    * Action called to Close the search dialog by returning to the last view node Id
    */
   public void closeSearch(ActionEvent event)
   {
      // set the current node Id ready for page refresh
      this.navigator.setCurrentNodeId( this.navigator.getCurrentNodeId() );
   }
   
   /**
    * Action called when a folder space is clicked.
    * Navigate into the space.
    */
   public void clickSpace(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         try
         {
            NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
            
            // refresh UI based on node selection
            refreshUI(ref, link);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_NODEREF, new Object[] {id}) );
         }
      }
   }
   
   /**
    * Action called when a folders direct descendant (in the 'list' browse mode) is clicked.
    * Navigate into the the descendant space.
    */
   public void clickDescendantSpace(ActionEvent event)
   {
      UINodeDescendants.NodeSelectedEvent nodeEvent = (UINodeDescendants.NodeSelectedEvent)event;
      NodeRef nodeRef = nodeEvent.NodeReference;
      if (nodeRef == null)
      {
         throw new IllegalStateException("NodeRef returned from UINodeDescendants.NodeSelectedEvent cannot be null!");
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Selected noderef Id: " + nodeRef.getId());
      
      try
      {
         // user can either select a descendant of a node display on the page which means we
         // must add the it's parent and itself to the breadcrumb
         List<IBreadcrumbHandler> location = this.navigator.getLocation();
         ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
         
         if (logger.isDebugEnabled())
         {
            logger.debug("Selected item getPrimaryParent().getChildRef() noderef Id:  " + parentAssocRef.getChildRef().getId());
            logger.debug("Selected item getPrimaryParent().getParentRef() noderef Id: " + parentAssocRef.getParentRef().getId());
            logger.debug("Current value getNavigator().getCurrentNodeId() noderef Id: " + getNavigator().getCurrentNodeId());
         }
         
         if (nodeEvent.IsParent == false)
         {
            // a descendant of the displayed node was selected
            // first refresh based on the parent and add to the breadcrumb
            refreshUI(parentAssocRef.getParentRef(), event.getComponent());
            
            // now add our selected node
            refreshUI(nodeRef, event.getComponent());
         }
         else
         {
            // else the parent ellipses i.e. the displayed node was selected
            refreshUI(nodeRef, event.getComponent());
         }
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_NODEREF, new Object[] {nodeRef.getId()}) );
      }
   }
   
   /**
    * Action event called by all Browse actions that need to setup a Space context
    * before an action page/wizard is called. The context will be a Node in setActionSpace() which
    * can be retrieved on the action page from BrowseBean.getActionSpace().
    * 
    * @param event   ActionEvent
    */
   public void setupSpaceAction(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         if (logger.isDebugEnabled())
            logger.debug("Setup for action, setting current space to: " + id);
         
         try
         {
            // create the node ref, then our node representation
            NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
            Node node = new Node(ref, this.nodeService);
            
            // prepare a node for the action context
            setActionSpace(node);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_NODEREF, new Object[] {id}) );
         }
      }
      else
      {
         setActionSpace(null);
      }
      
      // clear the UI state in preparation for finishing the next action
      invalidateComponents();
   }
   
   /**
    * Action event called by all actions that need to setup a Content Document context on the 
    * BrowseBean before an action page/wizard is called. The context will be a Node in
    * setDocument() which can be retrieved on the action page from BrowseBean.getDocument().
    * 
    * @param event   ActionEvent
    */
   public void setupContentAction(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         if (logger.isDebugEnabled())
            logger.debug("Setup for action, setting current document to: " + id);
         
         try
         {
            // create the node ref, then our node representation
            NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
            Node node = new Node(ref, this.nodeService);
            
            // store the URL to for downloading the content
            String name = node.getName();
            node.getProperties().put("url", DownloadContentServlet.generateDownloadURL(ref, name));
            
            // remember the document
            setDocument(node);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_NODEREF, new Object[] {id}) );
         }
      }
      else
      {
         setDocument(null);
      }
      
      // clear the UI state in preparation for finishing the next action
      invalidateComponents();
   }
   
   /**
    * Handler called upon the completion of the Delete Space page
    * 
    * @return outcome
    */
   public String deleteSpaceOK()
   {
      String outcome = null;
      
      Node node = getActionSpace();
      if (node != null)
      {
         try
         {
            if (logger.isDebugEnabled())
               logger.debug("Trying to delete space Id: " + node.getId());
            
            this.nodeService.deleteNode(node.getNodeRef());
            
            // remove this node from the breadcrumb if required
            List<IBreadcrumbHandler> location = navigator.getLocation();
            IBreadcrumbHandler handler = location.get(location.size() - 1);
            if (handler instanceof BrowseBreadcrumbHandler)
            {
               // see if the current breadcrumb location is our node 
               if ( ((BrowseBreadcrumbHandler)handler).getNodeId().equals(node.getId()) == true )
               {
                  location.remove(location.size() - 1);
                  
                  // now work out which node to set the list to refresh against
                  if (location.size() != 0)
                  {
                     handler = location.get(location.size() - 1);
                     if (handler instanceof BrowseBreadcrumbHandler)
                     {
                        // change the current node Id
                        navigator.setCurrentNodeId(((BrowseBreadcrumbHandler)handler).getNodeId());
                     }
                     else
                     {
                        // TODO: shouldn't do this - but for now the user home dir is the root!
                        navigator.setCurrentNodeId(Application.getCurrentUser(FacesContext.getCurrentInstance()).getHomeSpaceId());
                     }
                  }
               }
            }
            
            // clear action context
            setActionSpace(null);
            
            // setting the outcome will show the browse view again
            outcome = "browse";
         }
         catch (Throwable err)
         {
            Utils.addErrorMessage("Unable to delete Space due to system error: " + err.getMessage(), err);
         }
      }
      else
      {
         logger.warn("WARNING: deleteSpaceOK called without a current Space!");
      }
      
      return outcome;
   }
   
   /**
    * Handler called upon the completion of the Delete File page
    * 
    * @return outcome
    */
   public String deleteFileOK()
   {
      String outcome = null;
      
      Node node = getDocument();
      if (node != null)
      {
         try
         {
            if (logger.isDebugEnabled())
               logger.debug("Trying to delete content node Id: " + node.getId());
            
            this.nodeService.deleteNode(node.getNodeRef());
            
            // clear action context
            setDocument(null);
            
            // setting the outcome will show the browse view again
            outcome = "browse";
         }
         catch (Throwable err)
         {
            Utils.addErrorMessage("Unable to delete File due to system error: " + err.getMessage(), err);
         }
      }
      else
      {
         logger.warn("WARNING: deleteFileOK called without a current Document!");
      }
      
      return outcome;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private helpers
   
   /**
    * Refresh the UI after a Space selection change. Adds the selected space to the breadcrumb
    * location path and also updates the list components in the UI.
    * 
    * @param ref     NodeRef of the selected space
    * @param comp    UIComponent responsible for the UI update
    */
   private void refreshUI(NodeRef ref, UIComponent comp)
   {
      // get the current breadcrumb location and append a new handler to it
      // our handler know the ID of the selected node and the display label for it
      List<IBreadcrumbHandler> location = this.navigator.getLocation();
      String name = Repository.getNameForNode(this.nodeService, ref);
      location.add(new BrowseBreadcrumbHandler(ref.getId(), name));
      
      // set the current node Id ready for page refresh
      this.navigator.setCurrentNodeId(ref.getId());
   }
   
   /**
    * Invalidate list component state after an action which changes the UI context
    */
   private void invalidateComponents()
   {
      if (logger.isDebugEnabled())
         logger.debug("Invalidating UI List Components...");
      
      // clear the value for the list components - will cause re-bind to it's data and refresh
      if (this.contentRichList != null)
      {
         this.contentRichList.setValue(null);
      }
      if (this.spacesRichList != null)
      {
         this.spacesRichList.setValue(null);
      }
   }
   
   /**
    * @return whether the current View ID is the "browse" screen
    */
   private boolean isViewCurrent()
   {
      return (FacesContext.getCurrentInstance().getViewRoot().getViewId().equals(BROWSE_VIEW_ID));
   }
   
   /**
    * Perform navigation to the browse screen if it is not already the current View
    */
   private void navigateBrowseScreen()
   {
      if (isViewCurrent() == false)
      {
         FacesContext fc = FacesContext.getCurrentInstance();
         fc.getApplication().getNavigationHandler().handleNavigation(fc, null, "browse");
      }
   }
   

   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class to handle breadcrumb interaction for Browse pages
    */
   private class BrowseBreadcrumbHandler implements IBreadcrumbHandler
   {
      private static final long serialVersionUID = 3833183653173016630L;
      
      /**
       * Constructor
       * 
       * @param nodeId     The nodeID for this browse navigation element
       * @param label      Element label
       */
      public BrowseBreadcrumbHandler(String nodeId, String label)
      {
         this.label = label;
         this.nodeId = nodeId;
      }
      
      /**
       * @see java.lang.Object#toString()
       */
      public String toString()
      {
         return this.label;
      }

      /**
       * @see org.alfresco.web.ui.common.component.IBreadcrumbHandler#navigationOutcome(org.alfresco.web.ui.common.component.UIBreadcrumb)
       */
      public String navigationOutcome(UIBreadcrumb breadcrumb)
      {
         // All browse breadcrumb element relate to a Node Id - when selected we
         // set the current node id
         getNavigator().setCurrentNodeId(this.nodeId);
         getNavigator().setLocation( (List)breadcrumb.getValue() );
         
         // return to browse page if required
         return (isViewCurrent() ? null : "browse"); 
      }
      
      public String getNodeId()
      {
         return this.nodeId;
      }
      
      private String nodeId;
      private String label;
   }

   
   // ------------------------------------------------------------------------------
   // Private data
   
   public static final String BROWSE_VIEW_ID = "/jsp/browse/browse.jsp";
   
   private static Logger logger = Logger.getLogger(BrowseBean.class);
   
   /** The NodeService to be used by the bean */
   private NodeService nodeService;
   
   /** The SearchService to be used by the bean */
   private SearchService searchService;
   
   /** The LockService to be used by the bean */
   private LockService lockService;
   
   /** The NavigationBean reference */
   private NavigationBean navigator;
   
   /** Component references */
   private UIRichList spacesRichList;
   private UIRichList contentRichList;
   
   /** Transient lists */
   private List<Node> containerNodes = null;
   private List<Node> contentNodes = null;
   
   /** The current space and it's properties - if any */
   private Node actionSpace;
   
   /** The current document */
   private Node document;
   
   /** The current browse view mode - set to a well known IRichListRenderer name */
   private String browseViewMode = "list";
   
   /** The current browse view page size */
   private int browsePageSize = 10;
}
