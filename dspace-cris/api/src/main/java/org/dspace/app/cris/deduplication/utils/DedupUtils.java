/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.cris.configuration.ViewResolver;
import org.dspace.app.cris.deduplication.service.DedupService;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.CrisDeduplication;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.hibernate.Session;

public class DedupUtils
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(DedupUtils.class);

    private DedupService dedupService;

    private ApplicationService applicationService;
    
    private DSpace dspace = new DSpace();

    public DuplicateInfoList findSignatureWithDuplicate(Context context,
            String signatureType, int resourceType, int limit, int offset, int rule)
                    throws SearchServiceException, SQLException
    {
        return findPotentialMatch(context, signatureType, resourceType, limit,
                offset, rule);
    }

    public Map<String, Integer> countSignaturesWithDuplicates(String query, int resourceTypeId)
            throws SearchServiceException
    {
        Map<String, Integer> results = new HashMap<String, Integer>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD+":"+ SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD+":"+resourceTypeId);
        if (ConfigurationManager.getBooleanProperty("deduplication",
                "tool.duplicatechecker.ignorewithdrawn"))
        {
            solrQuery.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
        }
        QueryResponse response = dedupService.search(solrQuery);

        FacetField facetField = response.getFacetField(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD);
        if (facetField != null)
        {
            for (Count count : facetField.getValues())
            {
                solrQuery = new SolrQuery();
                solrQuery.setQuery(query);
                solrQuery.setRows(0);
                solrQuery.setFacet(true);
                solrQuery.setFacetMinCount(1);
                solrQuery.addFacetField(count.getName());
                solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD+":"+ SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
                if (ConfigurationManager.getBooleanProperty("deduplication",
                        "tool.duplicatechecker.ignorewithdrawn"))
                {
                    solrQuery.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
                }
                solrQuery.addFilterQuery(count.getAsFilterQuery());
                response = dedupService.search(solrQuery);
        
                FacetField facetField2 = response.getFacetField(count.getName());
                
                results.put(count.getName(), facetField2.getValueCount());
            }
        }

        return results;
    }

    public Map<String, Integer> countSuggestedDuplicate(String query, int resourceTypeId)
            throws SearchServiceException
    {
        Map<String, Integer> results = new HashMap<String, Integer>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        boolean ignoreSubmitterSuggestion = ConfigurationManager.getBooleanProperty("deduplication", "tool.duplicatechecker.ignore.submitter.suggestion", true);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD+":"+ (ignoreSubmitterSuggestion?SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription():"verify*"));
        if (ConfigurationManager.getBooleanProperty("deduplication",
                "tool.duplicatechecker.ignorewithdrawn"))
        {
            solrQuery.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
        }
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD+":"+resourceTypeId);
        QueryResponse response = dedupService.search(solrQuery);
        if(response!=null && response.getResults()!=null && !response.getResults().isEmpty()) {
            Long numbers = response.getResults().getNumFound();
            results.put("onlyreported", numbers.intValue());
        }

        return results;
    }
    
    /**
     * @param context
     * @param id
     * @param resourceType
     * @param signatureType
     * @param isInWorkflow set null to retrieve all (ADMIN) 
     * @return
     * @throws SQLException
     * @throws SearchServiceException
     */
    private List<DuplicateItemInfo> findDuplicate(Context context, UUID id,
            Integer resourceType, String signatureType, Boolean isInWorkflow)
                    throws SQLException, SearchServiceException
    {
            ViewResolver resolver = dspace.getServiceManager().getServiceByName(CrisConstants.getEntityTypeText(resourceType) + "ViewResolver", ViewResolver.class);
        
            List<UUID> result = new ArrayList<UUID>();
            Map<UUID, String> verify = new HashMap<UUID,String>();

            SolrQuery findDuplicateBySignature = new SolrQuery();
            findDuplicateBySignature.setQuery((isInWorkflow == null?SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED:(isInWorkflow?SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF:SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFY)));
            findDuplicateBySignature
                    .addFilterQuery(SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":"
                            + id);
            findDuplicateBySignature.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":"
                    + resourceType);
            String filter = "";
            if(isInWorkflow==null) {            
                filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription();            }
            else if(isInWorkflow) {
                filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":("
                    + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription() +" OR "+ SolrDedupServiceImpl.DeduplicationFlag.VERIFYWS.getDescription() + ")";
            }
            else {
                filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription();
            }

            findDuplicateBySignature.addFilterQuery(filter);

            findDuplicateBySignature
                    .setFields("dedup.ids", "dedup.note", "dedup.flag");

            if (ConfigurationManager.getBooleanProperty("deduplication",
                    "tool.duplicatechecker.ignorewithdrawn"))
            {
                findDuplicateBySignature.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
            }

            QueryResponse response2 = dedupService
                    .search(findDuplicateBySignature);
            SolrDocumentList solrDocumentList2 = response2.getResults();
            for (SolrDocument solrDocument : solrDocumentList2)
            {
                Collection<Object> tmp = (Collection<Object>) solrDocument.getFieldValues("dedup.ids");
                if(tmp!=null && !tmp.isEmpty()) {
                    for(Object tttmp : tmp) {
                        String idtmp = (String)tttmp;
                        UUID parseInt = UUID.fromString(idtmp);
                        if(!parseInt.equals(id)) {
                            String flag = (String)solrDocument.getFieldValue("dedup.flag");
                            if(SolrDedupServiceImpl.DeduplicationFlag.VERIFYWS.getDescription().equals(flag)) {
                                verify.put(parseInt, (String)solrDocument.getFieldValue("dedup.note"));
                            }
                            else {
                                result.add(parseInt);
                            }
                            break;
                        }
                    }
                }
            }
        
        List<DuplicateItemInfo> dupsInfo = new ArrayList<DuplicateItemInfo>();        
        for (UUID idResult : result) {
            DuplicateItemInfo info = new DuplicateItemInfo();            
            info.setRejected(false);
            info.setDuplicateItem(resolver.fillDTO(context, idResult, resourceType));
            if(verify.containsKey(idResult)) {
                info.setNote(verify.get(idResult));
                info.setCustomActions(context);
            }
            else {
                info.setDefaultActions(context, isInWorkflow);
            }                     
            dupsInfo.add(info);
        }
        
        return dupsInfo;
    }

    public boolean rejectAdminDups(Context context, UUID firstId,
            UUID secondId, Integer type) throws SQLException, AuthorizeException
    {
        if (firstId == secondId)
        {
            return false;
        }
        if (!AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only the administrator can reject the duplicate in the administrative section");
        }
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        
        CrisDeduplication row = null;
        try
        {

            row = applicationService.uniqueCrisDeduplicationByFirstAndSecond(sortedIds[0].toString(), sortedIds[1].toString());
            if (row != null)
            {
                row.setAdmin_id(context.getCurrentUser().getID());
                row.setAdmin_time(new Date());
                row.setResource_type_id(type);
                row.setAdmin_decision(DeduplicationFlag.REJECTADMIN.getDescription());
            }
            else
            {
                row = new CrisDeduplication();
                row.setAdmin_id(context.getCurrentUser().getID());
                row.setFirstItemId(sortedIds[0].toString());
                row.setSecondItemId(sortedIds[1].toString());
                row.setAdmin_time(new Date());
                row.setResource_type_id(type);
                row.setAdmin_decision(DeduplicationFlag.REJECTADMIN.getDescription());
            }
            applicationService.saveOrUpdate(CrisDeduplication.class, row);
            dedupService.buildReject(context, firstId.toString(), secondId.toString(), type, DeduplicationFlag.REJECTADMIN, null);
            return true;
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Mark all the potential duplicates for the specified signature and item as
     * fake.
     * 
     * @param context
     * @param itemID
     * @param signatureID
     * @return false if no potential duplicates are found
     * @throws SQLException
     * @throws AuthorizeException
     * @throws SearchServiceException
     */
    public boolean rejectAdminDups(Context context, UUID itemID,
            String signatureType, int resourceType) throws SQLException,
                    AuthorizeException, SearchServiceException
    {

        DuplicateSignatureInfo dsi = findPotentialMatchByID(context,
                signatureType, resourceType, itemID);

        boolean found = false;
        for (BrowsableDSpaceObject item : dsi.getItems())
        {
            if(item!=null) {
                if (item.getID() == itemID)
                {
                    found = true;
                    break;
                }
            }
        }
        if (found && dsi.getNumItems() > 1)
        {
            for (BrowsableDSpaceObject item : dsi.getItems())
            {
                if (item != null)
                {
                    if (item.getID() != itemID)
                    {
                        rejectAdminDups(context, itemID, item.getID(),
                                resourceType);
                    }
                }
            }
        }
        return true;

    }

    public void rejectAdminDups(Context context, List<DSpaceObject> items,
            String signatureID) throws SQLException, AuthorizeException,
                    SearchServiceException
    {
        for (DSpaceObject item : items)
        {
            rejectAdminDups(context, item.getID(), signatureID, item.getType());
        }
    }

    public void verify(Context context, int dedupId, UUID firstId,
            UUID secondId, int type, boolean toFix, String note, boolean check)
                    throws SQLException, AuthorizeException
    {
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        firstId = sortedIds[0];
        secondId = sortedIds[1];
        Item firstItem = ContentServiceFactory.getInstance().getItemService().find(context, firstId);
        Item secondItem = ContentServiceFactory.getInstance().getItemService().find(context, secondId);
        if (AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context, firstItem,
                Constants.WRITE)
                || AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context, secondItem,
                        Constants.WRITE))
        {
            CrisDeduplication row = applicationService.uniqueCrisDeduplicationByFirstAndSecond(firstId.toString(), secondId.toString()); 
            
            if(row!=null) {               
                String submitterDecision = row.getSubmitter_decision();                
                if(check && StringUtils.isNotBlank(submitterDecision)) {
                    row.setSubmitter_decision(submitterDecision);
                }                
            }
            else {
                row = new CrisDeduplication();
            }
            
            row.setFirstItemId(firstId.toString());
            row.setSecondItemId(secondId.toString());
            row.setResource_type_id(type);
            row.setTofix(toFix);
            row.setFake(false);
            row.setReader_note(note);
            row.setReader_id(context.getCurrentUser().getID());
            row.setReader_time(new Date());
            row.setResource_type_id(type);
            if(check) {
            	row.setWorkflow_decision(DeduplicationFlag.VERIFYWF.getDescription());
            }
            else {
            	row.setSubmitter_decision(DeduplicationFlag.VERIFYWS.getDescription());
            }

            applicationService.saveOrUpdate(CrisDeduplication.class, row);
            dedupService.buildReject(context, firstId.toString(), secondId.toString(), type, check?DeduplicationFlag.VERIFYWF:DeduplicationFlag.VERIFYWS, note);
        }
        else
        {
            throw new AuthorizeException(
                    "Only authorize users can access to the deduplication");
        }
    }

    public boolean rejectDups(Context context, UUID firstId,
            UUID secondId, Integer type, boolean notDupl, String note,
            boolean check) throws SQLException
    {
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        CrisDeduplication row = null;
        try
        {
        	
        	row = applicationService.uniqueCrisDeduplicationByFirstAndSecond(sortedIds[0].toString(), sortedIds[1].toString());        	

            Item firstItem = ContentServiceFactory.getInstance().getItemService().find(context, firstId);
            Item secondItem = ContentServiceFactory.getInstance().getItemService().find(context, secondId);
            if (AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context, firstItem,
                    Constants.WRITE)
                    || AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context,
                            secondItem, Constants.WRITE))
            {

                if(row!=null) {                
                    String submitterDecision = row.getSubmitter_decision();                
                    if(check && StringUtils.isNotBlank(submitterDecision)) {
                        row.setSubmitter_decision(submitterDecision);
                    }    
                }
                else {
                	 row = new CrisDeduplication();
                }
                
                row.setEperson_id(context.getCurrentUser().getID());
                row.setFirstItemId(sortedIds[0].toString());
                row.setSecondItemId(sortedIds[1].toString());
                row.setReject_time(new Date());
                row.setNote(note);
                row.setFake(notDupl);
                row.setResource_type_id(type);
                if (check)
                {
                    row.setWorkflow_decision(
                            DeduplicationFlag.REJECTWF.getDescription());
                }
                else
                {
                    row.setSubmitter_decision(
                            DeduplicationFlag.REJECTWS.getDescription());
                }
                applicationService.saveOrUpdate(CrisDeduplication.class, row);
                dedupService.buildReject(context, firstId.toString(), secondId.toString(), type,
                        check ? DeduplicationFlag.REJECTWF
                                : DeduplicationFlag.REJECTWS,
                        note);
                return true;
            }
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    private DuplicateInfoList findPotentialMatch(Context context,
            String signatureType, int resourceType, int start, int rows, int rule)
                    throws SearchServiceException, SQLException
    {

        DuplicateInfoList dil = new DuplicateInfoList();

        if (StringUtils.isNotEmpty(signatureType))
        {
            if (!StringUtils.contains(signatureType, "_signature"))
            {
                signatureType += "_signature";
            }
        }
        SolrQuery solrQueryExternal = new SolrQuery();

        solrQueryExternal.setRows(0);
        
        String subqueryNotInRejected = null;
        
        switch (rule)
        {
        case 1:
            subqueryNotInRejected = SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFY;
            break;
        case 2:
            subqueryNotInRejected = SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF;
            break;
        default:
            subqueryNotInRejected = SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED;
            break;
        }

        solrQueryExternal.setQuery(subqueryNotInRejected);

        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":"
                + signatureType);
        
        solrQueryExternal
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD
                        + ":" + resourceType);
        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
        if (ConfigurationManager.getBooleanProperty("deduplication",
                "tool.duplicatechecker.ignorewithdrawn"))
        {
            solrQueryExternal.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
        }
        solrQueryExternal.setFacet(true);
        solrQueryExternal.setFacetMinCount(1);
        solrQueryExternal.addFacetField(signatureType);
        solrQueryExternal.setFacetSort(FacetParams.FACET_SORT_COUNT);

        QueryResponse responseFacet = getDedupService().search(solrQueryExternal);

        FacetField facetField = responseFacet.getFacetField(signatureType);
        
        List<DuplicateInfo> result = new ArrayList<DuplicateInfo>();
        
        int index = 0;
        for (Count facetHit : facetField.getValues())
        {
            if (index >= start + rows)
            {
                break;
            }
            if (index >= start)
            {
                String name = facetHit.getName();

                SolrQuery solrQueryInternal = new SolrQuery();

                solrQueryInternal.setQuery(subqueryNotInRejected);
                
                solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":"
                                + signatureType);
                solrQueryInternal.addFilterQuery(facetHit.getAsFilterQuery());                
                solrQueryInternal.addFilterQuery(
                        SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":"
                                + resourceType);
                solrQueryInternal.setRows(Integer.MAX_VALUE);
                solrQueryInternal.addFilterQuery(
                        SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
                if (ConfigurationManager.getBooleanProperty("deduplication",
                        "tool.duplicatechecker.ignorewithdrawn"))
                {
                    solrQueryInternal.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
                }
                QueryResponse response = getDedupService().search(solrQueryInternal);
                
                SolrDocumentList solrDocumentList = response.getResults();

                DuplicateSignatureInfo dsi = new DuplicateSignatureInfo(signatureType, name);
                
                for (SolrDocument solrDocument : solrDocumentList)
                {

                    List<String> signatureTypeList = (List<String>) (solrDocument
                            .getFieldValue(signatureType));
                    // Collection<Object> tmp = (Collection<Object>)
                    for (String signatureTypeString : signatureTypeList)
                    {
                        if(name.equals(signatureTypeString)) {
                            
                            dsi.setSignature(signatureTypeString);    
                            Integer resourceTypeString = (Integer) (solrDocument
                                    .getFieldValue(
                                            SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD));
                            List<String> ids = (List<String>) solrDocument
                                    .getFieldValue(
                                            SolrDedupServiceImpl.RESOURCE_IDS_FIELD);
                                                        
                            if (resourceTypeString < CrisConstants.CRIS_TYPE_ID_START)
                            {
    
                                for (String obj : ids)
                                {
                                	Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(obj));
                                    if (item != null)
                                    {
                                        if (!(dsi.getItems().contains(item)))
                                        {
                                            dsi.getItems().add(item);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                for (String obj : ids)
                                {
                                	BrowsableDSpaceObject dspaceObject = getApplicationService()
                                            .getEntityByCrisId(obj);
                                    if (!(dsi.getItems().contains(dspaceObject)))
                                    {
                                        dsi.getItems().add(dspaceObject);
                                    }
                                }
                            }
    
                            result.add(dsi);
                        }
                        else {
                            dsi.getOtherSignature().add(signatureTypeString);
                        }
                    }
                }
            }
            index++;
        }

        dil.setDsi(result);
        dil.setSize(facetField.getValues().size());
        return dil;
    }

    private DuplicateSignatureInfo findPotentialMatchByID(Context context,
            String signatureType, int resourceType, UUID itemID)
                    throws SearchServiceException, SQLException
    {
        if (StringUtils.isNotEmpty(signatureType))
        {
            if (!StringUtils.contains(signatureType, "_signature"))
            {
                signatureType += "_signature";
            }
        }
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery(
                SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":" + itemID);

        solrQuery.addFilterQuery(
                SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":"
                        + signatureType);
        solrQuery
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD
                        + ":" + resourceType);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH
                        .getDescription());

        QueryResponse response = getDedupService().search(solrQuery);

        SolrDocumentList solrDocumentList = response.getResults();

        DuplicateSignatureInfo dsi = new DuplicateSignatureInfo(signatureType);
        for (SolrDocument solrDocument : solrDocumentList)
        {

            String signatureTypeString = (String) ((List) (solrDocument
                    .getFieldValue(signatureType))).get(0);

            dsi.setSignature(signatureTypeString);

            Integer resourceTypeString = (Integer) (solrDocument.getFieldValue(
                    SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD));
            List<String> ids = (List<String>) solrDocument
                    .getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

            if (resourceTypeString < CrisConstants.CRIS_TYPE_ID_START)
            {

                for (String obj : ids)
                {
                    Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(obj));
                    if (!(dsi.getItems().contains(item)))
                    {
                        dsi.getItems().add(item);
                    }
                }
            }
            else
            {
                for (String obj : ids)
                {
                	BrowsableDSpaceObject dspaceObject = getApplicationService()
                            .getEntityByCrisId(obj);
                    if (!(dsi.getItems().contains(dspaceObject)))
                    {
                        dsi.getItems().add(dspaceObject);
                    }
                }
            }

        }

        return dsi;
    }

    public DedupService getDedupService()
    {
        return dedupService;
    }

    public void setDedupService(DedupService dedupService)
    {
        this.dedupService = dedupService;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void commit()
    {
        dedupService.commit();        
    }

    public List<DuplicateItemInfo> getDuplicateByIDandType(Context context,
            UUID itemID, int typeID, boolean isInWorkflow) throws SQLException, SearchServiceException
    {      
        return getDuplicateByIdAndTypeAndSignatureType(context, itemID, typeID, null, isInWorkflow);
    }
    
    public List<DuplicateItemInfo> getDuplicateByIdAndTypeAndSignatureType(Context context,
            UUID itemID, int typeID, String signatureType, boolean isInWorkflow) throws SQLException, SearchServiceException
    {        
        return findDuplicate(context, itemID, typeID, signatureType, isInWorkflow);
    }

    public List<DuplicateItemInfo> getAdminDuplicateByIdAndType(Context context,
            UUID itemID, int typeID) throws SQLException, SearchServiceException
    {        
        return findDuplicate(context, itemID, typeID, null, null);
    }
    
    public DuplicateInfoList findSuggestedDuplicate(Context context,
            int resourceType, int start, int rows)
                    throws SearchServiceException, SQLException
    {

        DuplicateInfoList dil = new DuplicateInfoList();

        SolrQuery solrQueryInternal = new SolrQuery();

        solrQueryInternal
                .setQuery(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED);

        solrQueryInternal
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD
                        + ":" + resourceType);
        boolean ignoreSubmitterSuggestion = ConfigurationManager.getBooleanProperty("deduplication", "tool.duplicatechecker.ignore.submitter.suggestion", true);
        if(ignoreSubmitterSuggestion) {
            solrQueryInternal
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription());
        }
        else {
            solrQueryInternal.addFilterQuery(
                    SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":verify*");            
        }

        QueryResponse response = getDedupService().search(solrQueryInternal);

        SolrDocumentList solrDocumentList = response.getResults();

        List<DuplicateInfo> result = new ArrayList<DuplicateInfo>();

        int index = 0;

        for (SolrDocument solrDocument : solrDocumentList)
        {
            if (index >= start + rows)
            {
                break;
            }
            DuplicateSignatureInfo dsi = new DuplicateSignatureInfo("suggested",
                    (String) solrDocument.getFirstValue("_version_"));

            Integer resourceTypeString = (Integer) (solrDocument.getFieldValue(
                    SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD));
            List<String> ids = (List<String>) solrDocument
                    .getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

            if (resourceTypeString < CrisConstants.CRIS_TYPE_ID_START)
            {

                for (String obj : ids)
                {
                	Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(obj));
                    if (item != null)
                    {
                        if (!(dsi.getItems().contains(item)))
                        {
                            dsi.getItems().add(item);
                        }
                    }
                }
            }
            else
            {
                for (String obj : ids)
                {
                	BrowsableDSpaceObject dspaceObject = getApplicationService()
                            .getEntityByCrisId(obj);
                    if (!(dsi.getItems().contains(dspaceObject)))
                    {
                        dsi.getItems().add(dspaceObject);
                    }
                }
            }

            result.add(dsi);
            index++;
        }

        dil.setDsi(result);
        dil.setSize(solrDocumentList.getNumFound());
        return dil;
    }
    
    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}
