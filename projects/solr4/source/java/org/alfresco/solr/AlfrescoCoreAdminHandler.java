/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.Duration;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.ContentTracker;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.ModelTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.Tracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.alfresco.util.CachingDateFormat;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlfrescoCoreAdminHandler extends CoreAdminHandler
{
    protected final static Logger log = LoggerFactory.getLogger(AlfrescoCoreAdminHandler.class);
    
    private static final String ARG_ACLTXID = "acltxid";
    private static final String ARG_TXID = "txid";
    private static final String ARG_ACLID = "aclid";
    private static final String ARG_NODEID = "nodeid";
    
    private SolrTrackerScheduler scheduler = null;
    private TrackerRegistry trackerRegistry = new TrackerRegistry();
    private ConcurrentHashMap<String, InformationServer> informationServers = new ConcurrentHashMap<String, InformationServer>();
    
    public AlfrescoCoreAdminHandler()
    {
        super();
    }

    /**
     * @param coreContainer
     */
    public AlfrescoCoreAdminHandler(CoreContainer coreContainer)
    {
        super(coreContainer);

        this.scheduler = new SolrTrackerScheduler(this);

        initResourceBasedLogging("log4j.properties");
        initResourceBasedLogging("log4j-solr.properties");
    }

    private void initResourceBasedLogging(String resource)
    {
        try
        {
            Class<?> clazz = Class.forName("org.apache.log4j.PropertyConfigurator");
            Method method = clazz.getMethod("configure", Properties.class);
            InputStream is = openResource(coreContainer, resource);
            Properties p = new Properties();
            p.load(is);
            method.invoke(null, p);
        }
        catch (ClassNotFoundException e)
        {
            return;
        }
        catch (Exception e)
        {
            log.info("Failed to load " + resource, e);
        }
    }

    private InputStream openResource(CoreContainer coreContainer, String resource)
    {
        InputStream is = null;
        try
        {
            File f0 = new File(resource);
            File f = f0;
            if (!f.isAbsolute())
            {
                // try $CWD/$configDir/$resource
                f = new File(coreContainer.getSolrHome() + resource);
            }
            if (f.isFile() && f.canRead())
            {
                return new FileInputStream(f);
            }
            else if (f != f0)
            { // no success with $CWD/$configDir/$resource
                if (f0.isFile() && f0.canRead()) return new FileInputStream(f0);
            }
            // delegate to the class loader (looking into $INSTANCE_DIR/lib jars)
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error opening " + resource, e);
        }
        if (is == null) { throw new RuntimeException("Can't find resource '" + resource + "' in classpath or '"
                    + coreContainer.getSolrHome() + "', cwd=" + System.getProperty("user.dir")); }
        return is;
    }


    protected void handleCustomAction(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        SolrParams params = req.getParams();
        String cname = params.get(CoreAdminParams.CORE);
        String a = params.get(CoreAdminParams.ACTION);
        try
        {
            if (a.equalsIgnoreCase("TEST"))
            {
                new AlfrescoCoreAdminTester(this).runTests(req, rsp);
            }
            else if (a.equalsIgnoreCase("AUTHTEST"))
            {
                new AlfrescoCoreAdminTester(this).runAuthTest(req, rsp);
            }
            else if (a.equalsIgnoreCase("CMISTEST"))
            {
                new AlfrescoCoreAdminTester(this).runCmisTests(req, rsp);
            }
            else if (a.equalsIgnoreCase("newCore"))
            {
                newCore(req, rsp);
            }
            else if (a.equalsIgnoreCase("updateCore"))
            {
                updateCore(req, rsp);
            }
            else if (a.equalsIgnoreCase("removeCore"))
            {
                removeCore(req, rsp);
            }
            else if (a.equalsIgnoreCase("CHECK"))
            {
                actionCHECK(cname);
            }
            else if (a.equalsIgnoreCase("NODEREPORT"))
            {
                actionNODEREPORTS(rsp, params, cname);
            }
            else if (a.equalsIgnoreCase("ACLREPORT"))
            {
                actionACLREPORT(rsp, params, cname);
            }
            else if (a.equalsIgnoreCase("TXREPORT"))
            {
                actionTXREPORT(rsp, params, cname);
            }
            else if (a.equalsIgnoreCase("ACLTXREPORT"))
            {
                actionACLTXREPORT(rsp, params, cname);
            }
            else if (a.equalsIgnoreCase("REPORT"))
            {
                actionREPORT(rsp, params, cname);
            }
            else if (a.equalsIgnoreCase("PURGE"))
            {
                if (cname != null)
                {
                    actionPURGE(params, cname);
                }
                else
                {
                    for (String coreName : getTrackerRegistry().getCoreNames())
                    {
                        actionPURGE(params, coreName);
                    }
                }
            }
            else if (a.equalsIgnoreCase("REINDEX"))
            {
                if (cname != null)
                {
                    actionREINDEX(params, cname);
                }
                else
                {
                    for (String coreName : getTrackerRegistry().getCoreNames())
                    {
                        actionREINDEX(params, coreName);
                    }
                }
            }
            else if (a.equalsIgnoreCase("RETRY"))
            {
                if (cname != null)
                {
                    actionRETRY(rsp, cname);
                }
                else
                {
                    for (String coreName : getTrackerRegistry().getCoreNames())
                    {
                        actionRETRY(rsp, coreName);
                    }
                }
            }
            else if (a.equalsIgnoreCase("INDEX"))
            {
                if (cname != null)
                {
                    actionINDEX(params, cname);
                }
                else
                {
                    for (String coreName : getTrackerRegistry().getCoreNames())
                    {
                        actionINDEX(params, coreName);
                    }
                }
            }
            else if (a.equalsIgnoreCase("FIX"))
            {
                if (cname != null)
                {
                    actionFIX(cname);
                }
                else
                {
                    for (String coreName : getTrackerRegistry().getCoreNames())
                    {
                        actionFIX(coreName);
                    }
                }
            }
            else if (a.equalsIgnoreCase("SUMMARY"))
            {
                if (cname != null)
                {
                    NamedList<Object> report = new SimpleOrderedMap<Object>();
                    actionSUMMARY(params, report, cname);
                    rsp.add("Summary", report);
                }
                else
                {
                    NamedList<Object> report = new SimpleOrderedMap<Object>();
                    for (String coreName : getTrackerRegistry().getCoreNames())
                    {
                        actionSUMMARY(params, report, coreName);
                    }
                    rsp.add("Summary", report);
                }
            }
            else if (a.equalsIgnoreCase("LOG4J"))
            {
                String resource = "log4j-solr.properties";
                if (params.get("resource") != null)
                {
                    resource = params.get("resource");
                }
                initResourceBasedLogging(resource);
            }
            else
            {
                handleCustomAction(req, rsp);
            }
        }
        catch (Exception ex)
        {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                        "Error executing implementation of admin request " + a, ex);
        }
    }
    

    private boolean newCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        try
        {
            String store = "";
            SolrParams params = req.getParams();
            if (params.get("storeRef") != null)
            {
                store = params.get("storeRef");
            }

            if ((store == null) || (store.length() == 0)) { return false; }

            String templateName = "store";
            if (params.get("template") != null)
            {
                templateName = params.get("template");
            }

            StoreRef storeRef = new StoreRef(store);
            String coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
            if (params.get("coreName") != null)
            {
                coreName = params.get("coreName");
            }

            // copy core from template
            File solrHome = new File(coreContainer.getSolrHome());
            File templates = new File(solrHome, "templates");
            File template = new File(templates, templateName);

            File newCore = new File(solrHome, coreName);

            copyDirectory(template, newCore, false);

            // fix configuration properties
            File config = new File(newCore, "conf/solrcore.properties");
            Properties properties = new Properties();
            properties.load(new FileInputStream(config));
            properties.setProperty("data.dir.root", newCore.getCanonicalPath());
            properties.setProperty("data.dir.store", coreName);
            properties.setProperty("alfresco.stores", store);

            for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); /**/)
            {
                String paramName = it.next();
                if (paramName.startsWith("property."))
                {
                    properties.setProperty(paramName.substring("property.".length()), params.get(paramName));
                }
            }

            properties.store(new FileOutputStream(config), null);

            // add core
            CoreDescriptor dcore = new CoreDescriptor(coreContainer, coreName, newCore.toString());
//            dcore.setCoreProperties(null);
            SolrCore core = coreContainer.create(dcore);
            coreContainer.register(coreName, core, false);
            rsp.add("core", core.getName());

            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }


    private boolean updateCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        try
        {
            String store = "";
            SolrParams params = req.getParams();
            if (params.get("storeRef") != null)
            {
                store = params.get("storeRef");
            }

            if ((store == null) || (store.length() == 0)) { return false; }

            StoreRef storeRef = new StoreRef(store);
            String coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
            if (params.get("coreName") != null)
            {
                coreName = params.get("coreName");
            }

            File solrHome = new File(coreContainer.getSolrHome());

            File newCore = new File(solrHome, coreName);

            // fix configuration properties

            File config = new File(newCore, "conf/solrcore.properties");
            Properties properties = new Properties();
            properties.load(new FileInputStream(config));

            for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); /**/)
            {
                String paramName = it.next();
                if (paramName.startsWith("property."))
                {
                    properties.setProperty(paramName.substring("property.".length()), params.get(paramName));
                }
            }

            properties.store(new FileOutputStream(config), null);

            coreContainer.reload(coreName);

            return false;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }


    private boolean removeCore(SolrQueryRequest req, SolrQueryResponse rsp)
    {
        try
        {
            String store = "";
            SolrParams params = req.getParams();
            if (params.get("storeRef") != null)
            {
                store = params.get("storeRef");
            }

            if ((store == null) || (store.length() == 0)) { return false; }

            StoreRef storeRef = new StoreRef(store);
            String coreName = storeRef.getProtocol() + "-" + storeRef.getIdentifier();
            if (params.get("coreName") != null)
            {
                coreName = params.get("coreName");
            }

            File solrHome = new File(coreContainer.getSolrHome());
            File newCore = new File(solrHome, coreName);

            // remove core

            SolrCore done = coreContainer.remove(coreName);
            if (done != null)
            {
                done.close();
            }

            AlfrescoCoreAdminHandler.deleteDirectory(newCore);

            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }



    private void actionFIX(String coreName) throws AuthenticationException, IOException, JSONException
    {
        // Gets Metadata health and fixes any problems
        MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
        IndexHealthReport indexHealthReport = metadataTracker.checkIndex(null, null, null, null);
        IOpenBitSet toReindex = indexHealthReport.getTxInIndexButNotInDb();
        toReindex.or(indexHealthReport.getDuplicatedTxInIndex());
        toReindex.or(indexHealthReport.getMissingTxFromIndex());
        long current = -1;
        // Goes through problems in the index
        while ((current = toReindex.nextSetBit(current + 1)) != -1)
        {
            metadataTracker.addTransactionToReindex(current);
        }
        
        // Gets the Acl health and fixes any problems
        AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
        indexHealthReport = aclTracker.checkIndex(null, null, null, null);
        toReindex = indexHealthReport.getAclTxInIndexButNotInDb();
        toReindex.or(indexHealthReport.getDuplicatedAclTxInIndex());
        toReindex.or(indexHealthReport.getMissingAclTxFromIndex());
        current = -1;
        // Goes through the problems in the index
        while ((current = toReindex.nextSetBit(current + 1)) != -1)
        {
            aclTracker.addAclChangeSetToReindex(current);
        }
    }

    private void actionCHECK(String cname)
    {
        if (cname != null)
        {
            for (Tracker tracker : trackerRegistry.getTrackersForCore(cname))
            {
                tracker.getTrackerState().setCheck(true);
            }
        }
        else
        {
            for (String core : trackerRegistry.getCoreNames())
            {
                for (Tracker tracker : trackerRegistry.getTrackersForCore(core))
                {
                    tracker.getTrackerState().setCheck(true);
                }
            }
        }
    }

    private void actionACLREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException,
                JSONException
    {
        if (params.get(ARG_ACLID) == null)
        {
            throw new AlfrescoRuntimeException("No aclid parameter set");
        }
        
        if (cname != null)
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            AclTracker tracker = trackerRegistry.getTrackerForCore(cname, AclTracker.class);
            report.add(cname, buildAclReport(tracker, aclid));
            rsp.add("report", report);
        }
        else
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                report.add(coreName, buildAclReport(tracker, aclid));
            }
            rsp.add("report", report);
        }
    }


    private NamedList<Object> buildAclTxReport(String coreName, AclTracker tracker, Long acltxid) 
                throws AuthenticationException, IOException, JSONException
    {
        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("TXID", acltxid);
        nr.add("transaction", buildTrackerReport(coreName, 0l, 0l, acltxid, acltxid, null, null));
        NamedList<Object> nodes = new SimpleOrderedMap<Object>();
        // add node reports ....
        List<Long> dbAclIds = tracker.getAclsForDbAclTransaction(acltxid);
        for (Long aclid : dbAclIds)
        {
            nodes.add("ACLID " + aclid, buildAclReport(tracker, aclid));
        }
        nr.add("aclTxDbAclCount", dbAclIds.size());
        nr.add("nodes", nodes);
        return nr;
    }
    

    private NamedList<Object> buildAclReport(AclTracker tracker, Long aclid) throws IOException, JSONException
    {
        AclReport aclReport = tracker.checkAcl(aclid);

        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("Acl Id", aclReport.getAclId());
        nr.add("Acl doc in index", aclReport.getIndexAclDoc());
        if (aclReport.getIndexAclDoc() != null)
        {
            nr.add("Acl tx in Index", aclReport.getIndexAclTx());
        }

        return nr;
    }

    private NamedList<Object> buildTxReport(String coreName, MetadataTracker tracker, Long txid) 
                throws AuthenticationException, IOException, JSONException
    {
        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("TXID", txid);
        nr.add("transaction", buildTrackerReport(coreName, txid, txid, 0l, 0l, null, null));
        NamedList<Object> nodes = new SimpleOrderedMap<Object>();
        // add node reports ....
        List<Node> dbNodes = tracker.getFullNodesForDbTransaction(txid);
        for (Node node : dbNodes)
        {
            nodes.add("DBID " + node.getId(), buildNodeReport(tracker, node));
        }

        nr.add("txDbNodeCount", dbNodes.size());
        nr.add("nodes", nodes);
        return nr;
    }
    


    private NamedList<Object> buildNodeReport(MetadataTracker tracker, Node node) throws IOException, JSONException
    {
        NodeReport nodeReport = tracker.checkNode(node);

        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("Node DBID", nodeReport.getDbid());
        nr.add("DB TX", nodeReport.getDbTx());
        nr.add("DB TX status", nodeReport.getDbNodeStatus().toString());
        if (nodeReport.getIndexLeafDoc() != null)
        {
            nr.add("Leaf tx in Index", nodeReport.getIndexLeafTx());
        }
        if (nodeReport.getIndexAuxDoc() != null)
        {
            nr.add("Aux tx in Index", nodeReport.getIndexAuxTx());
        }
        nr.add("Indexed Node Doc Count", nodeReport.getIndexedNodeDocCount());
        return nr;
    }

    private NamedList<Object> buildNodeReport(MetadataTracker tracker, Long dbid) throws IOException, JSONException
    {
        NodeReport nodeReport = tracker.checkNode(dbid);

        NamedList<Object> nr = new SimpleOrderedMap<Object>();
        nr.add("Node DBID", nodeReport.getDbid());
        nr.add("DB TX", nodeReport.getDbTx());
        nr.add("DB TX status", nodeReport.getDbNodeStatus().toString());
        if (nodeReport.getIndexLeafDoc() != null)
        {
            nr.add("Leaf tx in Index", nodeReport.getIndexLeafTx());
        }
        if (nodeReport.getIndexAuxDoc() != null)
        {
            nr.add("Aux tx in Index", nodeReport.getIndexAuxTx());
        }
        nr.add("Indexed Node Doc Count", nodeReport.getIndexedNodeDocCount());
        return nr;
    }

    private NamedList<Object> buildTrackerReport(String coreName, Long fromTx, Long toTx, Long fromAclTx, Long toAclTx,
                Long fromTime, Long toTime) throws IOException, JSONException, AuthenticationException
    {
        // ACL
        AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
        IndexHealthReport aclReport = aclTracker.checkIndex(toTx, toAclTx, fromTime, toTime);
        NamedList<Object> ihr = new SimpleOrderedMap<Object>();
        ihr.add("Alfresco version", aclTracker.getAlfrescoVersion());
        ihr.add("DB acl transaction count", aclReport.getDbAclTransactionCount());
        ihr.add("Count of duplicated acl transactions in the index", aclReport.getDuplicatedAclTxInIndex()
                    .cardinality());
        if (aclReport.getDuplicatedAclTxInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate acl tx", aclReport.getDuplicatedAclTxInIndex().nextSetBit(0L));
        }
        ihr.add("Count of acl transactions in the index but not the DB", aclReport.getAclTxInIndexButNotInDb()
                    .cardinality());
        if (aclReport.getAclTxInIndexButNotInDb().cardinality() > 0)
        {
            ihr.add("First acl transaction in the index but not the DB", aclReport.getAclTxInIndexButNotInDb()
                        .nextSetBit(0L));
        }
        ihr.add("Count of missing acl transactions from the Index", aclReport.getMissingAclTxFromIndex()
                    .cardinality());
        if (aclReport.getMissingAclTxFromIndex().cardinality() > 0)
        {
            ihr.add("First acl transaction missing from the Index", aclReport.getMissingAclTxFromIndex()
                        .nextSetBit(0L));
        }
        ihr.add("Index acl transaction count", aclReport.getAclTransactionDocsInIndex());
        ihr.add("Index unique acl transaction count", aclReport.getAclTransactionDocsInIndex());
        TrackerState aclState = aclTracker.getTrackerState();
        ihr.add("Last indexed change set commit time", aclState.getLastIndexedChangeSetCommitTime());
        Date lastChangeSetDate = new Date(aclState.getLastIndexedChangeSetCommitTime());
        ihr.add("Last indexed change set commit date", CachingDateFormat.getDateFormat().format(lastChangeSetDate));
        ihr.add("Last changeset id before holes", aclState.getLastIndexedChangeSetIdBeforeHoles());

        // Metadata
        MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
        IndexHealthReport metaReport = metadataTracker.checkIndex(toTx, toAclTx, fromTime, toTime);
        ihr.add("DB transaction count", metaReport.getDbTransactionCount());
        ihr.add("Count of duplicated transactions in the index", metaReport.getDuplicatedTxInIndex()
                    .cardinality());
        if (metaReport.getDuplicatedTxInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate", metaReport.getDuplicatedTxInIndex().nextSetBit(0L));
        }
        ihr.add("Count of transactions in the index but not the DB", metaReport.getTxInIndexButNotInDb()
                    .cardinality());
        if (metaReport.getTxInIndexButNotInDb().cardinality() > 0)
        {
            ihr.add("First transaction in the index but not the DB", metaReport.getTxInIndexButNotInDb()
                        .nextSetBit(0L));
        }
        ihr.add("Count of missing transactions from the Index", metaReport.getMissingTxFromIndex().cardinality());
        if (metaReport.getMissingTxFromIndex().cardinality() > 0)
        {
            ihr.add("First transaction missing from the Index", metaReport.getMissingTxFromIndex()
                        .nextSetBit(0L));
        }
        ihr.add("Index transaction count", metaReport.getTransactionDocsInIndex());
        ihr.add("Index unique transaction count", metaReport.getTransactionDocsInIndex());
        ihr.add("Index node count", metaReport.getLeafDocCountInIndex());
        ihr.add("Count of duplicate nodes in the index", metaReport.getDuplicatedLeafInIndex().cardinality());
        if (metaReport.getDuplicatedLeafInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate node id in the index", metaReport.getDuplicatedLeafInIndex().nextSetBit(0L));
        }
        ihr.add("Index error count", metaReport.getErrorDocCountInIndex());
        ihr.add("Count of duplicate error docs in the index", metaReport.getDuplicatedErrorInIndex()
                    .cardinality());
        if (metaReport.getDuplicatedErrorInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate error in the index", SolrInformationServer.PREFIX_ERROR
                        + metaReport.getDuplicatedErrorInIndex().nextSetBit(0L));
        }
        ihr.add("Index unindexed count", metaReport.getUnindexedDocCountInIndex());
        ihr.add("Count of duplicate unindexed docs in the index", metaReport.getDuplicatedUnindexedInIndex()
                    .cardinality());
        if (metaReport.getDuplicatedUnindexedInIndex().cardinality() > 0)
        {
            ihr.add("First duplicate unindexed in the index", 
                        metaReport.getDuplicatedUnindexedInIndex().nextSetBit(0L));
        }
        TrackerState metaState = metadataTracker.getTrackerState();
        ihr.add("Last indexed transaction commit time", metaState.getLastIndexedTxCommitTime());
        Date lastTxDate = new Date(metaState.getLastIndexedTxCommitTime());
        ihr.add("Last indexed transaction commit date", CachingDateFormat.getDateFormat().format(lastTxDate));
        ihr.add("Last TX id before holes", metaState.getLastIndexedTxIdBeforeHoles());
        
        InformationServer srv = informationServers.get(coreName);
        srv.addFTSStatusCounts(ihr);
        
        return ihr;
    }
    
    private void actionTXREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws AuthenticationException,
                IOException, JSONException
    {
        if (params.get(ARG_TXID) == null)
        {
            throw new AlfrescoRuntimeException("No txid parameter set");
        }
        
        if (cname != null)
        {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname, MetadataTracker.class);
            Long txid = Long.valueOf(params.get(ARG_TXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            report.add(cname, buildTxReport(cname, tracker, txid));
            rsp.add("report", report);
        }
        else
        {
            Long txid = Long.valueOf(params.get(ARG_TXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname,
                            MetadataTracker.class);
                report.add(coreName, buildTxReport(coreName, tracker, txid));
            }
            rsp.add("report", report);
        }
    }

    private void actionACLTXREPORT(SolrQueryResponse rsp, SolrParams params, String cname)
                throws AuthenticationException, IOException, JSONException
    {
        if (params.get(ARG_ACLTXID) == null)
        {
            throw new AlfrescoRuntimeException("No acltxid parameter set");
        }
        
        if (cname != null)
        {
            AclTracker tracker = trackerRegistry.getTrackerForCore(cname, AclTracker.class);
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            report.add(cname, buildAclTxReport(cname, tracker, acltxid));
            rsp.add("report", report);
        }
        else
        {
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
                report.add(coreName, buildAclTxReport(coreName, tracker, acltxid));
            }
            rsp.add("report", report);
        }
    }

    private void actionREPORT(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException,
                JSONException, AuthenticationException
    {
        Long fromTime = getSafeLong(params, "fromTime");
        Long toTime = getSafeLong(params, "toTime");
        Long fromTx = getSafeLong(params, "fromTx");
        Long toTx = getSafeLong(params, "toTx");
        Long fromAclTx = getSafeLong(params, "fromAclTx");
        Long toAclTx = getSafeLong(params, "toAclTx");
        
        if (cname != null)
        {
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            if (trackerRegistry.hasTrackersForCore(cname))
            {
                report.add(cname, buildTrackerReport(cname, fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime));
                rsp.add("report", report);
            }
            else 
            {
                report.add(cname, "Core unknown");
            }
        }
        else
        {
            NamedList<Object> report = new SimpleOrderedMap<Object>();
            for (String coreName : trackerRegistry.getCoreNames())
            {
                if (trackerRegistry.hasTrackersForCore(coreName))
                {
                    report.add(coreName, buildTrackerReport(coreName, fromTx, toTx, fromAclTx, toAclTx, fromTime, toTime));
                }
                else 
                {
                    report.add(coreName, "Core unknown");
                }
            }
            rsp.add("report", report);
        }
    }


    private boolean getSafeBoolean(SolrParams params, String paramName)
    {
        boolean paramValue = false;
        if (params.get(paramName) != null)
        {
            paramValue = Boolean.valueOf(params.get(paramName));
        }
        return paramValue;
    }

    private Long getSafeLong(SolrParams params, String paramName)
    {
        Long paramValue = null;
        if (params.get(paramName) != null)
        {
            paramValue = Long.valueOf(params.get(paramName));
        }
        return paramValue;
    }

    private void actionNODEREPORTS(SolrQueryResponse rsp, SolrParams params, String cname) throws IOException,
                JSONException
    {
        if (cname != null)
        {
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(cname,
                        MetadataTracker.class);
            Long dbid = null;
            if (params.get(ARG_NODEID) != null)
            {
                dbid = Long.valueOf(params.get(ARG_NODEID));
                NamedList<Object> report = new SimpleOrderedMap<Object>();
                report.add(cname, buildNodeReport(tracker, dbid));
                rsp.add("report", report);

            }
            else
            {
                throw new AlfrescoRuntimeException("No dbid parameter set");
            }
        }
        else
        {
            Long dbid = null;
            if (params.get(ARG_NODEID) != null)
            {
                dbid = Long.valueOf(params.get(ARG_NODEID));
                NamedList<Object> report = new SimpleOrderedMap<Object>();
                for (String coreName : trackerRegistry.getCoreNames())
                {
                    MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName,
                                MetadataTracker.class);
                    report.add(coreName, buildNodeReport(tracker, dbid));
                }
                rsp.add("report", report);
            }
            else
            {
                throw new AlfrescoRuntimeException("No dbid parameter set");
            }

        }
    }

    private void actionSUMMARY(SolrParams params, NamedList<Object> report, String coreName) throws IOException
    {
        boolean detail = getSafeBoolean(params, "detail");
        boolean hist = getSafeBoolean(params, "hist");
        boolean values = getSafeBoolean(params, "values");
        boolean reset = getSafeBoolean(params, "reset");
        
        InformationServer srv = informationServers.get(coreName);
        if (srv != null)
        {
            addCoreSummary(coreName, detail, hist, values, srv, report);

            if (reset)
            {
                srv.getTrackerStats().reset();
            }
        }
        else
        {
            report.add(coreName, "Core unknown");
        }
    }


    /**
     * @param cname
     * @param detail
     * @param hist
     * @param values
     * @param tracker
     * @param report
     * @throws IOException
     */
    private void addCoreSummary(String cname, boolean detail, boolean hist, boolean values,
                InformationServer srv, NamedList<Object> report) throws IOException
    {
        NamedList<Object> coreSummary = new SimpleOrderedMap<Object>();
        coreSummary.addAll((SimpleOrderedMap<Object>) srv.getCoreStats());
        
        MetadataTracker metaTrkr = trackerRegistry.getTrackerForCore(cname, MetadataTracker.class);
        TrackerState metadataTrkrState = metaTrkr.getTrackerState();
        long lastIndexTxCommitTime = metadataTrkrState.getLastIndexedTxCommitTime();
        
        long lastIndexedTxId = metadataTrkrState.getLastIndexedTxId();
        long lastTxCommitTimeOnServer = metadataTrkrState.getLastTxCommitTimeOnServer();
        long lastTxIdOnServer = metadataTrkrState.getLastTxIdOnServer();
        Date lastIndexTxCommitDate = new Date(lastIndexTxCommitTime);
        Date lastTxOnServerDate = new Date(lastTxCommitTimeOnServer);
        long transactionsToDo = lastTxIdOnServer - lastIndexedTxId;
        if (transactionsToDo < 0)
        {
            transactionsToDo = 0;
        }

        AclTracker aclTrkr = trackerRegistry.getTrackerForCore(cname, AclTracker.class);
        TrackerState aclTrkrState = aclTrkr.getTrackerState();
        long lastIndexChangeSetCommitTime = aclTrkrState.getLastIndexedChangeSetCommitTime();
        long lastIndexedChangeSetId = aclTrkrState.getLastIndexedChangeSetId();
        long lastChangeSetCommitTimeOnServer = aclTrkrState.getLastChangeSetCommitTimeOnServer();
        long lastChangeSetIdOnServer = aclTrkrState.getLastChangeSetIdOnServer();
        Date lastIndexChangeSetCommitDate = new Date(lastIndexChangeSetCommitTime);
        Date lastChangeSetOnServerDate = new Date(lastChangeSetCommitTimeOnServer);
        long changeSetsToDo = lastChangeSetIdOnServer - lastIndexedChangeSetId;
        if (changeSetsToDo < 0)
        {
            changeSetsToDo = 0;
        }

        long nodesToDo = 0;
        long remainingTxTimeMillis = 0;
        if (transactionsToDo > 0)
        {
            // We now use the elapsed time as seen by the single thread farming out metadata indexing
            double meanDocsPerTx = srv.getTrackerStats().getMeanDocsPerTx();
            double meanNodeElaspedIndexTime = srv.getTrackerStats().getMeanNodeElapsedIndexTime();
            nodesToDo = (long)(transactionsToDo * meanDocsPerTx);
            remainingTxTimeMillis = (long) (nodesToDo * meanNodeElaspedIndexTime);
        }
        Date now = new Date();
        Date end = new Date(now.getTime() + remainingTxTimeMillis);
        Duration remainingTx = new Duration(now, end);

        long remainingChangeSetTimeMillis = 0;
        if (changeSetsToDo > 0)
        {
         // We now use the elapsed time as seen by the single thread farming out alc indexing
            double meanAclsPerChangeSet = srv.getTrackerStats().getMeanAclsPerChangeSet();
            double meanAclElapsedIndexTime = srv.getTrackerStats().getMeanAclElapsedIndexTime();
            remainingChangeSetTimeMillis = (long) (changeSetsToDo * meanAclsPerChangeSet * meanAclElapsedIndexTime);
        }
        now = new Date();
        end = new Date(now.getTime() + remainingChangeSetTimeMillis);
        Duration remainingChangeSet = new Duration(now, end);
        
        NamedList<Object> ftsSummary = new SimpleOrderedMap<Object>();
        long remainingContentTimeMillis = 0;
        srv.addFTSStatusCounts(ftsSummary);
        long cleanCount = ((Long)ftsSummary.get("Node count with FTSStatus Clean")).longValue();
        long dirtyCount = ((Long)ftsSummary.get("Node count with FTSStatus Dirty")).longValue();
        long newCount = ((Long)ftsSummary.get("Node count with FTSStatus New")).longValue();
        long nodesInIndex = ((Long)coreSummary.get("Alfresco Nodes in Index"));
        long contentYetToSee = nodesInIndex > 0 ? nodesToDo * (cleanCount + dirtyCount + newCount)/nodesInIndex  : 0;;
        if (dirtyCount + newCount + contentYetToSee > 0)
        {
            // We now use the elapsed time as seen by the single thread farming out alc indexing
            double meanContentElapsedIndexTime = srv.getTrackerStats().getMeanContentElapsedIndexTime();
            remainingContentTimeMillis = (long) ((dirtyCount + newCount + contentYetToSee) * meanContentElapsedIndexTime);
        }
        now = new Date();
        end = new Date(now.getTime() + remainingContentTimeMillis);
        Duration remainingContent = new Duration(now, end);
        coreSummary.add("FTS",ftsSummary);

        Duration txLag = new Duration(lastIndexTxCommitDate, lastTxOnServerDate);
        if (lastIndexTxCommitDate.compareTo(lastTxOnServerDate) > 0)
        {
            txLag = new Duration();
        }
        long txLagSeconds = (lastTxCommitTimeOnServer - lastIndexTxCommitTime) / 1000;
        if (txLagSeconds < 0)
        {
            txLagSeconds = 0;
        }

        Duration changeSetLag = new Duration(lastIndexChangeSetCommitDate, lastChangeSetOnServerDate);
        if (lastIndexChangeSetCommitDate.compareTo(lastChangeSetOnServerDate) > 0)
        {
            changeSetLag = new Duration();
        }
        long changeSetLagSeconds = (lastChangeSetCommitTimeOnServer - lastIndexChangeSetCommitTime) / 1000;
        if (txLagSeconds < 0)
        {
            txLagSeconds = 0;
        }

        ContentTracker contentTrkr = trackerRegistry.getTrackerForCore(cname, ContentTracker.class);
        TrackerState contentTrkrState = contentTrkr.getTrackerState();
        // Leave ModelTracker out of this check, because it is common
        boolean aTrackerIsRunning = aclTrkrState.isRunning() || metadataTrkrState.isRunning()
                    || contentTrkrState.isRunning();
        coreSummary.add("Active", aTrackerIsRunning);
        
        ModelTracker modelTrkr = trackerRegistry.getModelTracker();
        TrackerState modelTrkrState = modelTrkr.getTrackerState();
        coreSummary.add("ModelTracker Active", modelTrkrState.isRunning());
        coreSummary.add("ContentTracker Active", contentTrkrState.isRunning());
        coreSummary.add("MetadataTracker Active", metadataTrkrState.isRunning());
        coreSummary.add("AclTracker Active", aclTrkrState.isRunning());

        // TX

        coreSummary.add("Last Index TX Commit Time", lastIndexTxCommitTime);
        coreSummary.add("Last Index TX Commit Date", lastIndexTxCommitDate);
        coreSummary.add("TX Lag", txLagSeconds + " s");
        coreSummary.add("TX Duration", txLag.toString());
        coreSummary.add("Timestamp for last TX on server", lastTxCommitTimeOnServer);
        coreSummary.add("Date for last TX on server", lastTxOnServerDate);
        coreSummary.add("Id for last TX on server", lastTxIdOnServer);
        coreSummary.add("Id for last TX in index", lastIndexedTxId);
        coreSummary.add("Approx transactions remaining", transactionsToDo);
        coreSummary.add("Approx transaction indexing time remaining", remainingTx.largestComponentformattedString());

        // Change set

        coreSummary.add("Last Index Change Set Commit Time", lastIndexChangeSetCommitTime);
        coreSummary.add("Last Index Change Set Commit Date", lastIndexChangeSetCommitDate);
        coreSummary.add("Change Set Lag", changeSetLagSeconds + " s");
        coreSummary.add("Change Set Duration", changeSetLag.toString());
        coreSummary.add("Timestamp for last Change Set on server", lastChangeSetCommitTimeOnServer);
        coreSummary.add("Date for last Change Set on server", lastChangeSetOnServerDate);
        coreSummary.add("Id for last Change Set on server", lastChangeSetIdOnServer);
        coreSummary.add("Id for last Change Set in index", lastIndexedChangeSetId);
        coreSummary.add("Approx change sets remaining", changeSetsToDo);
        coreSummary.add("Approx change set indexing time remaining",
                    remainingChangeSet.largestComponentformattedString());
        
        coreSummary.add("Approx content indexing time remaining",
                remainingContent.largestComponentformattedString());

        // Stats

        coreSummary.add("Model sync times (ms)",
                    srv.getTrackerStats().getModelTimes().getNamedList(detail, hist, values));
        coreSummary.add("Acl index time (ms)",
                    srv.getTrackerStats().getAclTimes().getNamedList(detail, hist, values));
        coreSummary.add("Node index time (ms)",
                    srv.getTrackerStats().getNodeTimes().getNamedList(detail, hist, values));
        coreSummary.add("Docs/Tx", srv.getTrackerStats().getTxDocs().getNamedList(detail, hist, values));
        coreSummary.add("Doc Transformation time (ms)", srv.getTrackerStats().getDocTransformationTimes()
                    .getNamedList(detail, hist, values));

        // Model

        Map<String, Set<String>> modelErrors = srv.getModelErrors();
        if (modelErrors.size() > 0)
        {
            NamedList<Object> errorList = new SimpleOrderedMap<Object>();
            for (Map.Entry<String, Set<String>> modelNameToErrors : modelErrors.entrySet())
            {
                errorList.add(modelNameToErrors.getKey(), modelNameToErrors.getValue());
            }
            coreSummary.add("Model changes are not compatible with the existing data model and have not been applied",
                        errorList);
        }

        report.add(cname, coreSummary);
    }


    private void actionINDEX(SolrParams params, String coreName)
    {
        if (params.get(ARG_TXID) != null)
        {
            Long txid = Long.valueOf(params.get(ARG_TXID));
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            tracker.addTransactionToIndex(txid);
        }
        if (params.get(ARG_ACLTXID) != null)
        {
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            tracker.addAclChangeSetToIndex(acltxid);
        }
        if (params.get(ARG_NODEID) != null)
        {
            Long nodeid = Long.valueOf(params.get(ARG_NODEID));
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            tracker.addNodeToIndex(nodeid);
        }
        if (params.get(ARG_ACLID) != null)
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            tracker.addAclToIndex(aclid);
        }
    }

    private void actionRETRY(SolrQueryResponse rsp, String coreName) throws IOException
    {
        MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
        InformationServer srv = informationServers.get(coreName);
        Set<Long> errorDocIds = srv.getErrorDocIds();
        for (Long nodeid : errorDocIds)
        {
            tracker.addNodeToReindex(nodeid);
        }
        rsp.add(coreName, errorDocIds);
    }

    private void actionREINDEX(SolrParams params, String coreName)
    {
        if (params.get(ARG_TXID) != null)
        {
            Long txid = Long.valueOf(params.get(ARG_TXID));
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            tracker.addTransactionToReindex(txid);
        }
        if (params.get(ARG_ACLTXID) != null)
        {
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            tracker.addAclChangeSetToReindex(acltxid);
        }
        if (params.get(ARG_NODEID) != null)
        {
            Long nodeid = Long.valueOf(params.get(ARG_NODEID));
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            tracker.addNodeToReindex(nodeid);
        }
        if (params.get(ARG_ACLID) != null)
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            tracker.addAclToReindex(aclid);
        }
    }

    private void actionPURGE(SolrParams params, String coreName)
    {
        if (params.get(ARG_TXID) != null)
        {
            Long txid = Long.valueOf(params.get(ARG_TXID));
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            tracker.addTransactionToPurge(txid);
        }
        if (params.get(ARG_ACLTXID) != null)
        {
            Long acltxid = Long.valueOf(params.get(ARG_ACLTXID));
            AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            tracker.addAclChangeSetToPurge(acltxid);
        }
        if (params.get(ARG_NODEID) != null)
        {
            Long nodeid = Long.valueOf(params.get(ARG_NODEID));
            MetadataTracker tracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            tracker.addNodeToPurge(nodeid);
        }
        if (params.get(ARG_ACLID) != null)
        {
            Long aclid = Long.valueOf(params.get(ARG_ACLID));
            AclTracker tracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
            tracker.addAclToPurge(aclid);
        }
    }

    
    
    /**
     * Note files can alter due to background processes so file not found is Ok
     * 
     * @param srcDir
     * @param destDir
     * @param preserveFileDate
     * @throws IOException
     */
    static void copyDirectory(File srcDir, File destDir, boolean preserveFileDate) throws IOException
    {
        if (destDir.exists())
        {
            throw new IOException("Destination should be created from clean");
        }
        else
        {
            if (!destDir.mkdirs()) { throw new IOException("Destination '" + destDir + "' directory cannot be created"); }
            if (preserveFileDate)
            {
                // OL if file not found so does not need to check
                destDir.setLastModified(srcDir.lastModified());
            }
        }
        if (!destDir.canWrite()) { throw new IOException("No access to destination directory" + destDir); }

        File[] files = srcDir.listFiles();
        if (files != null)
        {
            for (int i = 0; i < files.length; i++)
            {
                File currentCopyTarget = new File(destDir, files[i].getName());
                if (files[i].isDirectory())
                {
                    copyDirectory(files[i], currentCopyTarget, preserveFileDate);
                }
                else
                {
                    copyFile(files[i], currentCopyTarget, preserveFileDate);
                }
            }
        }
    }

    private static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException
    {
        try
        {
            if (destFile.exists()) { throw new IOException("File shoud not exist " + destFile); }

            FileInputStream input = new FileInputStream(srcFile);
            try
            {
                FileOutputStream output = new FileOutputStream(destFile);
                try
                {
                    copy(input, output);
                }
                finally
                {
                    try
                    {
                        output.close();
                    }
                    catch (IOException io)
                    {

                    }
                }
            }
            finally
            {
                try
                {
                    input.close();
                }
                catch (IOException io)
                {

                }
            }

            // check copy
            if (srcFile.length() != destFile.length()) { throw new IOException("Failed to copy full from '" + srcFile
                        + "' to '" + destFile + "'"); }
            if (preserveFileDate)
            {
                destFile.setLastModified(srcFile.lastModified());
            }
        }
        catch (FileNotFoundException fnfe)
        {
            fnfe.printStackTrace();
        }
    }

    private static int copy(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[2048 * 4];
        int count = 0;
        int n = 0;
        while ((n = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    static void deleteDirectory(File directory) throws IOException
    {
        if (!directory.exists()) { return; }
        if (!directory.isDirectory()) { throw new IllegalArgumentException("Not a directory " + directory); }

        File[] files = directory.listFiles();
        if (files == null) { throw new IOException("Failed to delete director - no access" + directory); }

        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];

            if (file.isDirectory())
            {
                deleteDirectory(file);
            }
            else
            {
                if (!file.delete()) { throw new IOException("Unable to delete file: " + file); }
            }
        }

        if (!directory.delete()) { throw new IOException("Unable to delete directory " + directory); }
    }
    
    public ConcurrentHashMap<String, InformationServer> getInformationServers()
    {
        return this.informationServers;
    }

    public TrackerRegistry getTrackerRegistry()
    {
        return trackerRegistry;
    }

    public SolrTrackerScheduler getScheduler()
    {
        return scheduler;
    }
}
