/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.cris.deduplication.utils.DedupUtils;
import org.dspace.app.cris.deduplication.utils.DuplicateInfo;
import org.dspace.app.cris.deduplication.utils.DuplicateInfoList;
import org.dspace.app.util.DCInput;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.IMetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.util.ItemUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.hibernate.Session;

/**
 * 
 * Servlet to choose and merge duplicates.
 * 
 * @author pascarelli
 * 
 */
public class DuplicateCheckerServlet extends DSpaceServlet
{

    /** Logger */
    private static Logger log = Logger.getLogger(DuplicateCheckerServlet.class);

    private List<String> blockedTypes = new ArrayList<String>();

    private Map<String, Integer> submitOptionMap = new LinkedHashMap<String, Integer>();

    private final static String optionsString[] = { "submit", "submitcheck",
            "submittargetchoice", "submitpreview", "submitunrelatedall",
            "submitmerge" };

    private final static int ALL_DUPLICATES = 0;

    private final static int UNCHECKED_OR_TOFIX_DUPLICATES = 1;

    private final static int TOFIX_DUPLICATES = 2;

    private final static int COUNT_DUPLICATES = 0;

    private final static int SHOW_DUPLICATES = 1;

    private final static int SELECT_TARGET = 2;

    private final static int MANAGE_PREVIEW = 3;

    private final static int REJECT = 4;

    private final static int MERGE = 5;

    private final static int optionsInt[] = { COUNT_DUPLICATES, SHOW_DUPLICATES,
            SELECT_TARGET, MANAGE_PREVIEW, REJECT, MERGE };

    private DedupUtils dedupUtils = new DSpace().getServiceManager()
            .getServiceByName("dedupUtils", DedupUtils.class);

	private final transient ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	private final ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
	
    /**
     * Load blocked metadata configuration.
     */
    public DuplicateCheckerServlet()
    {
        String property = ConfigurationManager.getProperty("misc",
                "tool.duplicatechecker.blocked");
        if (property != null)
        {
            String[] typesConf = property.split(",");
            for (String type : typesConf)
            {
                blockedTypes.add(type.trim());
            }
        }

        int count = 0;
        for (String opt : optionsString)
        {
            submitOptionMap.put(opt, optionsInt[count]);
            count++;
        }
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
                    SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "cleaner", "cleaner servlet"));
        doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
                    SQLException, AuthorizeException
    {

        String option = UIUtil.getSubmitButton(request, "submit");
        String signatureType = request.getParameter("signatureType");
        int scope = UIUtil.getIntParameter(request, "scope");
        boolean mergeByUser = UIUtil.getBoolParameter(request, "mergeByUser");
        if (scope == -1)
            scope = ALL_DUPLICATES;

        int resourceType = UIUtil.getIntParameter(request, "resourceType");
        if (resourceType == -1)
            resourceType = Constants.ITEM;

        UUID targetDefault = null;
        int optionInt = submitOptionMap.get(option);
        
        // if suggested duplicates go to SHOW_DUPLICATES
        if (scope == TOFIX_DUPLICATES)
        {
            optionInt = SHOW_DUPLICATES;
        }
        
        // try to get JSP for item choices or preview merge
        switch (optionInt)
        {
        case COUNT_DUPLICATES:
        {
                // count for each group how many duplicates are there
                Map<String, Integer> duplicatesAll = new LinkedHashMap<String, Integer>();
                Map<String, Integer> duplicatesOnlyWorspace = new LinkedHashMap<String, Integer>();
                Map<String, Integer> duplicatesOnlyWorkflow = new LinkedHashMap<String, Integer>();
                Map<String, Integer> duplicatesOnlyReported = new LinkedHashMap<String, Integer>();

                try
                {
                    duplicatesAll = dedupUtils.countSignaturesWithDuplicates(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED, resourceType);
                    duplicatesOnlyWorspace = dedupUtils.countSignaturesWithDuplicates(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFY, resourceType);
                    duplicatesOnlyWorkflow = dedupUtils.countSignaturesWithDuplicates(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF, resourceType);
                    duplicatesOnlyReported = dedupUtils.countSuggestedDuplicate(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED, resourceType);
                }
                catch (SearchServiceException e)
                {
                    throw new ServletException(e);
                }

                request.setAttribute("scope", scope);
                request.setAttribute("duplicatessignatureall", duplicatesAll);
                request.setAttribute("duplicatessignatureonlyws", duplicatesOnlyWorspace);
                request.setAttribute("duplicatessignatureonlywf", duplicatesOnlyWorkflow);
                request.setAttribute("duplicatessignatureonlyreported", duplicatesOnlyReported);
                JSPManager.showJSP(request, response,
                        "/deduplication/cleanerview-choose.jsp");
                return;
        }
        case SHOW_DUPLICATES:
        {
            // After loaded grid to display logic here tries discover if target
            // must be choice or not.

            int rule = UIUtil.getIntParameter(request, "submitcheck");
            if (rule == -1)
            {
                rule = UIUtil.getIntParameter(request, "rule");
            }
            if (rule == -1)
            {
                rule = 0;
            }
            int rows = UIUtil.getIntParameter(request, "rows");
            int start = UIUtil.getIntParameter(request, "start");
            if (rows == -1)
            {
                rows = 50;
            }
            if (start == -1)
            {
                start = 0;
            }
            // parent grid is a container where key is the signature description
            // which we want to grouped item duplicates
            Map<String, Map<UUID, String[]>> gridParent = new LinkedHashMap<String, Map<UUID, String[]>>();
            Map<String, List<String>> gridTwiceGroups = new LinkedHashMap<String, List<String>>();

            long count = 0;
            // List<DuplicateInfo> duplicateCouple = null;
            Map<UUID, BrowsableDSpaceObject> extraInfo = new LinkedHashMap<UUID, BrowsableDSpaceObject>();
            Map<UUID, String> itemTypeInfo = new LinkedHashMap<UUID, String>();
            String idsListString = request.getParameter("itemid_list");
            List<BrowsableDSpaceObject> itemList = new ArrayList<BrowsableDSpaceObject>();

            if (StringUtils.isNotBlank(idsListString))
            {
                String ids[] = idsListString.split(",");
                count = ids.length;
                for (int j = 0; j < ids.length; j++)
                {
                    UUID currentId = null;
                    try
                    {
                        currentId = UUID.fromString(ids[j].trim());
                    }
                    catch (Exception e)
                    {
                        log.error(e.getMessage(), e);
                        count--;
                        continue;
                    }
                    BrowsableDSpaceObject current = (BrowsableDSpaceObject)ContentServiceFactory.getInstance().getDSpaceObjectService(resourceType).find(context, currentId);
                    if ((current == null) || (current.isWithdrawn()))
                    {
                        count--;
                        continue;
                    }
                    itemList.add(current);
                }

                Map<UUID, String[]> grid = new LinkedHashMap<UUID, String[]>();
                putItemsOnGrid(context, grid, extraInfo, itemList, request);
                gridParent.put("items", grid);
            }
            else
            {
                try
                {
                    DuplicateInfoList dil = null;
                    if (scope == TOFIX_DUPLICATES) {
                        dil = dedupUtils
                            .findSuggestedDuplicate(context, resourceType, start, rows);
                    }
                    else {
                        dil = dedupUtils
                                .findSignatureWithDuplicate(context, signatureType,
                                        resourceType, start, rows, rule);
                    }
                    for (DuplicateInfo info : dil.getDsi())
                    {
                        boolean found = false;
                        String keyChecked = "";
                        for(String check : info.getOtherSignature()) {
                            if(gridParent.containsKey(check)) {                                
                                found = true;
                                keyChecked = check;
                                break;
                            }
                        }
                        if(!found) {
                            Map<UUID, String[]> grid = new LinkedHashMap<UUID, String[]>();
                            putItemsOnGrid(context, grid, extraInfo,
                                    info.getItems(), request);
                            gridParent.put(info.getSignature(), grid);
                            gridTwiceGroups.put(info.getSignature(), new ArrayList<String>());
                        }
                        else {
                            gridTwiceGroups.get(keyChecked).add(info.getSignature());
                        }
                    }
                    count = dil.getSize();
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), e);
                }
            }

            Iterator<BrowsableDSpaceObject> it = extraInfo.values().iterator();
            while (it.hasNext())
            {
            	BrowsableDSpaceObject current = it.next();
                if (resourceType == Constants.ITEM)
                {
                    String aliasForm = ItemUtils.getDCInputSet((Item) current)
                            .getFormName();
                    if (StringUtils.isNotBlank(aliasForm))
                    {
                        itemTypeInfo.put(current.getID(), aliasForm);
                    }
                }
            }

            request.setAttribute("extraInfo", extraInfo);
            request.setAttribute("itemTypeInfo", itemTypeInfo);
            request.setAttribute("signatureType", signatureType);
            request.setAttribute("grid", gridParent);
            request.setAttribute("gridTwiceGroups", gridTwiceGroups);            
            request.setAttribute("start", start);
            request.setAttribute("scope", scope);
            request.setAttribute("rows", rows);
            request.setAttribute("count", count);
            request.setAttribute("rule", rule);
            request.setAttribute("mergeByUser", mergeByUser);
            JSPManager.showJSP(request, response,
                    "/deduplication/cleanerview-check.jsp");
            return;
        }
        case SELECT_TARGET:
        {
            // choose target or go to merge
            List<UUID> items = UIUtil.getUUIDParameters(request, "itemstomerge");
            int rule = UIUtil.getIntParameter(request, "rule");
            if (items == null || items.size() < 2)
            {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate?submitcheck=" + rule + "&scope="
                        + scope);
                return;
            }

            Map<UUID, String[]> grid = new LinkedHashMap<UUID, String[]>();
            Map<UUID, BrowsableDSpaceObject> extraInfo = new LinkedHashMap<UUID, BrowsableDSpaceObject>();

            putItemsOnGrid(context, grid, extraInfo,
                    getDSpaceObjects(context, items, resourceType), request);
            UUID oldestId = null;
            for (BrowsableDSpaceObject item : getDSpaceObjects(context, items,
                    resourceType))
            {
                if (((Item) item).isArchived())
                {
                    if (targetDefault == null)
                    {
                        targetDefault = item.getID();
                    }
                    else
                    {
                        if (item.getID().compareTo(targetDefault)<0)
                        {
                            targetDefault = item.getID();
                        }
                    }
                }
                if (targetDefault == null)
                {
                    if (oldestId == null)
                    {
                        oldestId = item.getID();
                    }
                    if (item.getID().compareTo(oldestId)<0)
                    {
                        oldestId = item.getID();
                    }
                }
            }
            if (targetDefault == null)
            {
                targetDefault = oldestId;
            }
            request.setAttribute("rule", rule);
            request.setAttribute("scope", scope);
            optionInt = MANAGE_PREVIEW;
        }
        case MANAGE_PREVIEW:
        {
            // manage preview
            List<UUID> items = UIUtil.getUUIDParameters(request, "itemstomerge");
            int rule = UIUtil.getIntParameter(request, "rule");
            if (items != null && items.size() == 1)
            {
                String itemstomerge = "";
                for (UUID i : items)
                {
                    itemstomerge += "&itemstomerge=" + i;
                }
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate?submittargetchoice&scope=" + scope
                        + "&rule=" + rule + itemstomerge);
                return;
            }

            UUID target = UIUtil.getUUIDParameter(request, "target");

            if (target == null)
            {
                target = targetDefault;
            }

            // Retrieve styles
            String propertyStyles = ConfigurationManager
                    .getProperty("deduplication", "plugin.bootstrap.styles");
            String propertyTargetStyle = ConfigurationManager
                    .getProperty("deduplication", "plugin.bootstrap.targetStyle");
            String propertyDefaultStyle = ConfigurationManager
                    .getProperty("deduplication", "plugin.bootstrap.defaultStyle");
            String[] styles = { "warning,danger" };
            
            String[] configurationStyles = null;
            if(StringUtils.isNotEmpty(propertyStyles)) {
                configurationStyles = propertyStyles.split(",");
            }

            // Checking configuration
            if (ArrayUtils.isNotEmpty(configurationStyles))
            {
                log.info("INFO: Applying default style " + styles
                        + "  You can overwrite \"plugin.bootstrap.styles\" in dspace.cfg");
                styles = configurationStyles;
            }
            if (StringUtils.isEmpty(propertyTargetStyle))
            {
                log.info(
                        "INFO: Applying taget style \" label label-success\"  You can overwrite \"plugin.bootstrap.targetStyle\" in dspace.cfg");
                propertyTargetStyle = "success";
            }
            if (StringUtils.isEmpty(propertyDefaultStyle))
            {
                log.info(
                        "INFO: Applying taget style \" label label-info\"  You can overwrite \"plugin.bootstrap.defaultStyle\" in dspace.cfg");
                propertyDefaultStyle = "info";
            }

            HashMap<UUID, String> legenda = new HashMap<UUID, String>();
            int k = 0;
            for (UUID i : items)
            {
                if (i .equals(( target)))
                    legenda.put(i, propertyTargetStyle);
                else if (k < styles.length)
                    legenda.put(i, styles[k]);
                else
                    legenda.put(i, propertyDefaultStyle);
                k++;
            }

            Map<UUID, String> citations = new HashMap<UUID, String>();
            for (UUID item : items)
            {
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
                        .getNamedPlugin(StreamDisseminationCrosswalk.class,
                                ConfigurationManager.getProperty("deduplication",
                                        "tool.duplicatechecker.citation"));
                try
                {
                    streamCrosswalkDefault.disseminate(context,
                            itemService.find(context, item), output);
                    citations.put(item, output.toString("UTF-8"));
                }
                catch (CrosswalkException e)
                {
                    log.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            }

            // All DC types in the registry
            List<MetadataField> types = ContentServiceFactory.getInstance().getMetadataFieldService().findAll(context);

            // Get a HashMap of metadata field ids and a field name to display
            HashMap<Integer, String> metadataFields = new HashMap<Integer, String>();

            // Get all existing Schemas
            List<MetadataSchema> schemas = ContentServiceFactory.getInstance().getMetadataSchemaService().findAll(context);
            for (MetadataSchema schema : schemas)
            {
                String schemaName = schema.getName();
                // Get all fields for the given schema
                List<MetadataField> fields = ContentServiceFactory.getInstance().getMetadataFieldService().findAllInSchema(context,
                        schema);
                for (MetadataField field : fields)
                {
                    String displayName = "";
                    displayName = schemaName + "." + field.getElement()
                            + (field.getQualifier() == null ? ""
                                    : "." + field.getQualifier());
                    metadataFields.put(field.getID(), displayName);
                }
            }

            // load target and fill object with other metadata
            Item targetItem = loadTarget(context, request, target, items, types,
                    metadataFields);
            List<Item> otherItems = getItems(context, items);

            Map<Collection, Boolean[]> collections = getCollections(context,
                    targetItem, otherItems, request);

            request.setAttribute("collections", collections);
            request.setAttribute("items", otherItems);
            request.setAttribute("target", targetItem);
            request.setAttribute("blockedMetadata", getBlockedTypes());
            request.setAttribute("dcTypes", types);
            request.setAttribute("metadataFields", metadataFields);
            request.setAttribute("legenda", legenda);
            request.setAttribute("citations", citations);
            request.setAttribute("rule", rule);
            request.setAttribute("scope", scope);
            request.setAttribute("signatureType", signatureType);
            request.setAttribute("mergeByUser", mergeByUser);
            
            JSPManager.showJSP(request, response,
                    "/deduplication/cleanerview-preview.jsp");
            break;
        }
        case REJECT:
        {
            // unrelate item
            List<UUID> items = UIUtil.getUUIDParameters(request, "itemstomerge");
            int rule = UIUtil.getIntParameter(request, "rule");
            if (items == null)
            {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate?submitcheck&scope=" + scope
                        + "&rule=" + rule);
                break;
            }

            for (UUID itemId : items)
            {
                for (UUID itemId2 : items)
                {
                    if (itemId2 .equals(( itemId)))
                    {
                        continue;
                    }
                    dedupUtils.rejectAdminDups(context, itemId, itemId2, resourceType);
                }
            }
            // Complete transaction
            context.complete();
            dedupUtils.commit();

            if(mergeByUser) {
                response.sendRedirect(request.getContextPath()
                        + "/mydspace");
            }
            else {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate");                
            }
            
            break;
        }
        case MERGE:
        {
            // commit works
            Item item = itemService.find(context,
                    UIUtil.getUUIDParameter(request, "item_id"));

            UUID collectionOwner = UIUtil.getUUIDParameter(request,
                    "collectionOwner");
            List<UUID> otherCollections = UIUtil.getUUIDParameters(request,
                    "collectionOthers");

            List<UUID> toRemove = UIUtil.getUUIDParameters(request, "itemremove_id");

            int rule = UIUtil.getIntParameter(request, "rule");

            Collection collection = null;
            if (collectionOwner != null)
            {
            	List<UUID> ccc = new ArrayList<UUID>();
            	ccc.add(collectionOwner);
                collection = getCollections(context,
                		ccc, null).get(0);
            }
            List<Collection> collections = new LinkedList<Collection>();
            if (otherCollections != null && otherCollections.size() > 0)
            {
                collections = getCollections(context, otherCollections,
                        collectionOwner);
            }
            processUpdateItem(context, request, response, item, collection,
                    collections);

            for (UUID remove : toRemove)
            {
				if (!remove.equals((item.getID())))
                {
                    Item itemRemove = itemService.find(context, remove);
                    if (itemRemove.isArchived() || itemRemove.isWithdrawn())
                    {
                        // add metadata replaced and go to withdrawn
                        itemService.addMetadata(context, itemRemove, MetadataSchema.DC_SCHEMA,
                                "relation", "isreplacedby", null,
                                "hdl:" + item.getHandle());

                        remove(context, request, itemRemove);

                        dedupUtils.rejectAdminDups(context, item.getID(),
                                remove, resourceType);
                        for (UUID other : toRemove)
                        {
							if (!other.equals((itemRemove.getID())))
                            {
                                dedupUtils.rejectAdminDups(context,
                                        itemRemove.getID(), other, resourceType);
                            }
                        }
                        // reject all other duplicates as fake
                        if ((scope != TOFIX_DUPLICATES) && (rule != -1))
                        {
                            try
                            {
                                dedupUtils.rejectAdminDups(context,
                                        itemRemove.getID(), signatureType,
                                        itemRemove.getType());
                            }
                            catch (SearchServiceException e)
                            {
                                throw new ServletException(e.getMessage(), e);
                            }
                        }
                        itemService.update(context, itemRemove);
                    }
                    else
                    {

                        remove(context, request, itemRemove);
                        
                        
                        getHibernateSession(context).createSQLQuery(
                                "DELETE FROM cris_deduplication WHERE first_item_id = :remove OR second_item_id = :remove").setParameter("remove", remove.toString()).executeUpdate();
                    }
                }
            }
            if ((scope != TOFIX_DUPLICATES) && (rule != -1))
            {
                try
                {
                    dedupUtils.rejectAdminDups(context, item.getID(),
                            signatureType, item.getType());
                }
                catch (SearchServiceException e)
                {
                    throw new ServletException(e.getMessage(), e);
                }
            }
            dedupUtils.commit();
            
            if(mergeByUser) {
                response.sendRedirect(request.getContextPath()
                        + "/mydspace");
            }
            else {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate");                
            }
        }
        default:
            // none operations
            break;
        }

    }

    private void remove(Context context, HttpServletRequest request,
            Item itemRemove)
                    throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(context, "merge_remove_item",
                "item_id=" + itemRemove.getID()));
        Integer status = ItemUtils.getItemStatus(context, itemRemove);
        switch (status)
        {
        case ItemUtils.ARCHIVE:
            itemService.withdraw(context, itemRemove);
            break;
        case ItemUtils.WORKFLOW:
            WorkflowItem wfi = WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context, itemRemove);
            Collection collectionParent = wfi.getCollection();
            if(collectionParent!=null) { 
                collectionParent.getCollectionService().removeItem(context, collectionParent, itemRemove);
            }
            WorkflowServiceFactory.getInstance().getWorkflowItemService().deleteWrapper(context, wfi);
            itemService.delete(context, itemRemove);
            break;
        case ItemUtils.WORKSPACE:
            WorkspaceItem wsi = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context, itemRemove);
            ContentServiceFactory.getInstance().getWorkspaceItemService().deleteAll(context, wsi);
            break;
        default:
            break;
        }
    }

    private Map<Collection, Boolean[]> getCollections(Context context,
            Item targetItem, List<Item> otherItems, HttpServletRequest request)
                    throws SQLException
    {
        Map<Collection, Boolean[]> result = new LinkedHashMap<Collection, Boolean[]>();
        for (Collection coll : targetItem.getCollections())
        {
            result.put(coll, new Boolean[] { false, true });
        }
        if (targetItem.isArchived())
        {
            result.put(targetItem.getOwningCollection(),
                    new Boolean[] { true, false });
        }
        else
        {
            request.setAttribute("noowningcollection", true); // show only
                                                              // radiobutton
                                                              // to choose
                                                              // inprogresssubmission
                                                              // collection

            
			WorkspaceItem wsi = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context,
					targetItem);

			if (wsi == null) { // if item not in workspace then
								// check if item
								// is
								// in workflow state
				WorkflowItem wfi = WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context,
						targetItem);
				if (wfi != null) {
					if (!result.containsKey(wfi.getCollection())) {
						result.put(wfi.getCollection(), new Boolean[] { false, false });
					}

				}
			} else {
				if (!result.containsKey(wsi.getCollection())) {
					result.put(wsi.getCollection(), new Boolean[] { false, false });
				}

			}		
        }

        for (Item other : otherItems)
        {
            for (Collection coll : other.getCollections())
            {
                if (!result.containsKey(coll))
                {
                    result.put(coll, new Boolean[] { false, false });
                }
            }
            if (other.isArchived())
            {
                if (!result.containsKey(other.getOwningCollection()))
                {
                    result.put(other.getOwningCollection(),
                            new Boolean[] { false, false });
                }
            }
            else
            {
				WorkspaceItem wsi = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context,
						other);

				if (wsi == null) { // if item not in workspace then
									// check if item
									// is
									// in workflow state
					WorkflowItem wfi = WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context,
							other);
					if (wfi != null) {
						if (!result.containsKey(wfi.getCollection())) {
							result.put(wfi.getCollection(), new Boolean[] { false, false });
						}

					}
				} else {
					if (!result.containsKey(wsi.getCollection())) {
						result.put(wsi.getCollection(), new Boolean[] { false, false });
					}

				}
            }

        }
        return result;
    }

    /**
     * Read from configuration to load blocked metadata
     * 
     * @return
     */
    private List<String> getBlockedTypes()
    {
        return blockedTypes;
    }

    /**
     * Fill target item with new metadata, manages bundles "ORIGINAL" and
     * organize attribute to render on view.
     * 
     * @param context
     * @param request
     * @param target
     * @param items
     * @param fields
     * @param metadataFields
     * @return
     * @throws SQLException
     */
    private Item loadTarget(Context context, HttpServletRequest request,
            UUID target, List<UUID> items, List<MetadataField> fields,
            HashMap<Integer, String> metadataFields) throws SQLException
    {
        // get items
        Item item = itemService.find(context, target);

        List<Item> others = getItems(context, items);
        // This map contains items which are owners of metadata (metadata
        // formkey -> owner item ID)
        Map<String, UUID> metadataSourceInfo = new HashMap<String, UUID>();
        // This map contains a field which correspond to the various types of
        // metadata (MetatadaField ID -> List of DTO's DCValues)
        Map<Integer, List<DTODCValue>> metadataExtraSourceInfo = new HashMap<Integer, List<DTODCValue>>();
        // List of all metadata
        List<DTODCValue> dtodcvalues = new LinkedList<DTODCValue>();

        Map<Integer, DCInput> dcinputs = new HashMap<Integer, DCInput>();

        // fill other metadata on target object
        outer: for (MetadataField field : fields)
        {
            Integer fieldID = new Integer(field.getID());
            List<DTODCValue> dtodcvalue = new LinkedList<DTODCValue>();

            String mdString = metadataFields.get(fieldID);
            List<IMetadataValue> value = item.getMetadataValueInDCFormat(mdString);
            if (value != null && value.size() > 0)
            {
                if (dcinputs.get(fieldID) == null)
                {
                    try
                    {
                        dcinputs.put(fieldID,
                                ItemUtils.getDCInput(MetadataSchema.DC_SCHEMA,
                                        field.getElement(),
                                        field.getQualifier(),
                                        ItemUtils.getDCInputSet(item)));
                    }
                    catch (Exception e)
                    {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            if (value == null || value.size() == 0)
            {
                // get from the first match
                inner: for (Item other : others)
                {

                    List<IMetadataValue> valueOther = other
                            .getMetadataValueInDCFormat(mdString);
                    if (valueOther == null || valueOther.size() == 0)
                    {
                        continue inner;
                    }
                    else
                    {
                        for (IMetadataValue v : valueOther)
                        {
                            itemService.addMetadata(context, item, v.getSchema(), v.getElement(), v.getQualifier(),
                                    v.getLanguage(), v.getValue());
                            createDTODCValue(fieldID, dtodcvalue, other, v,
                                    false, mdString);
                        }
                        metadataSourceInfo.put(mdString.replaceAll("\\.", "_"),
                                other.getID());
                        if (dcinputs.get(fieldID) == null)
                        {
                            try
                            {
                                dcinputs.put(fieldID, ItemUtils.getDCInput(
                                        MetadataSchema.DC_SCHEMA,
                                        field.getElement(),
                                        field.getQualifier(),
                                        ItemUtils.getDCInputSet(other)));
                            }
                            catch (Exception e)
                            {
                                log.error(e.getMessage(), e);
                            }
                        }
                        break inner;
                    }

                }
            }
            else
            {
                metadataSourceInfo.put(mdString.replaceAll("\\.", "_"),
                        item.getID());
                for (IMetadataValue v : value)
                {
                    createDTODCValue(fieldID, dtodcvalue, item, v, false,
                            mdString);
                }
                inner: for (Item other : others)
                {
					if (!other.getID().equals((item.getID())))
                    {
                        List<IMetadataValue> valueOther = other.getItemService()
                                .getMetadataByMetadataString(other, mdString);
                        if (valueOther == null || valueOther.size() == 0)
                        {
                            continue inner;
                        }
                        else
                        {
                            for (IMetadataValue v : valueOther)
                            {

                                boolean removed = checkContentEquality(value,
                                        v);

                                itemService.addMetadata(context, item, v.getSchema(), v.getElement(),
                                        v.getQualifier(), v.getLanguage(), v.getValue(),
                                        v.getAuthority(), v.getConfidence());
                                createDTODCValue(fieldID, dtodcvalue, other, v,
                                        true, removed, mdString);

                            }
                            if (dcinputs.get(fieldID) == null)
                            {
                                try
                                {
                                    dcinputs.put(fieldID,
                                            ItemUtils.getDCInput(
                                                    MetadataSchema.DC_SCHEMA,
                                                    field.getElement(),
                                                    field
                                                            .getQualifier(),
                                            ItemUtils.getDCInputSet(other)));
                                }
                                catch (Exception e)
                                {
                                    log.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
            }

            metadataExtraSourceInfo.put(fieldID, dtodcvalue);
            dtodcvalues.addAll(dtodcvalue);
        }

        List<Bitstream> bitstreams = new LinkedList<Bitstream>();

        for (Item other : others)
        {
            for (Bundle bnd : itemService.getBundles(other, Constants.CONTENT_BUNDLE_NAME))
            {
                for (Bitstream b : bnd.getBitstreams())
                {
                    bitstreams.add(b);
                }
            }
        }

        request.setAttribute("bitstreams", bitstreams);
        request.setAttribute("dtodcvalues", dtodcvalues);
        request.setAttribute("metadataSourceInfo", metadataSourceInfo);
        request.setAttribute("metadataExtraSourceInfo",
                metadataExtraSourceInfo);
        request.setAttribute("dcinputs", dcinputs);
        return item;
    }

    /**
     * Utility method to create a DTODCValue and fill it on list to render on
     * view
     * 
     * @param fieldID
     * @param dtodcvalue
     * @param other
     * @param v
     * @param hidden
     * @param mdString
     * @return
     * @throws SQLException
     */
    private DTODCValue createDTODCValue(Integer fieldID,
            List<DTODCValue> dtodcvalue, Item other, IMetadataValue v,
            boolean hidden, boolean removed, String mdString)
                    throws SQLException
    {

        DTODCValue dto = new DTODCValue();
        dto.setDcValue(v);
        dto.setHidden(hidden);
        dto.setOwner(other.getID());
        dto.setRemoved(removed);
        dto.setMetadataFieldId(fieldID);
        // Collection[] collections = other.getCollections();
        // // owning Collection ID for choice authority calls
        // int collectionID = -1;
        // if (collections.length > 0)
        // collectionID = collections[0].getID();
        // dto.setOwnerCollectionID(collectionID);

        if (getBlockedTypes().contains(mdString))
        {
            dto.setBlocked(true);
        }
        dtodcvalue.add(dto);
        return dto;

    }

    /**
     * Wrapped method to utility method to create a DTODCValue and fill it on
     * list to render on view
     * 
     * @param fieldID
     * @param dtodcvalue
     * @param other
     * @param v
     * @param hidden
     * @param mdString
     * @return
     * @throws SQLException
     */
    private DTODCValue createDTODCValue(Integer fieldID,
            List<DTODCValue> dtodcvalue, Item other, IMetadataValue v,
            boolean hidden, String mdString) throws SQLException
    {
        return createDTODCValue(fieldID, dtodcvalue, other, v, hidden, false,
                mdString);
    }

    private boolean checkContentEquality(List<IMetadataValue> value, IMetadataValue v)
    {
        DTODCValue dtoValue = new DTODCValue();
        dtoValue.setDcValue(v);
        boolean result = false;
        for (IMetadataValue adcvalue : value)
        {
            IMetadataValue vv = adcvalue;
            DTODCValue dtoValueToCompare = new DTODCValue();
            dtoValueToCompare.setDcValue(vv);
            if ((dtoValueToCompare.getValue() == null)
                    || (dtoValue.getValue() == null))
            {
                continue;
            }
            if (!dtoValueToCompare.getValue().equals(dtoValue.getValue())
                    || (!dtoValueToCompare.getAuthority()
                            .equals(dtoValue.getAuthority())))
            {
                continue;
            }
            result = true;
            break;
        }

        return result;
    }

    private void putItemsOnGrid(Context context, Map<UUID, String[]> grid,
            Map<UUID, BrowsableDSpaceObject> extraInfo, List<BrowsableDSpaceObject> items,
            HttpServletRequest hrq) throws SQLException, IOException
    {
        final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
                .getNamedPlugin(StreamDisseminationCrosswalk.class,
                        ConfigurationManager.getProperty("deduplication",
                                "tool.duplicatechecker.citation"));

        for (BrowsableDSpaceObject item : items)
        {

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try
            {
                streamCrosswalkDefault.disseminate(context, item, output);
            }
            catch (CrosswalkException e)
            {
                log.error(e.getMessage(), e);
                throw new IOException(e);
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
                throw new IOException(e);
            }
            catch (AuthorizeException e)
            {
                log.error(e.getMessage(), e);
                throw new IOException(e);
            }

            grid.put(item.getID(), new String[] { output.toString("UTF-8") });
            extraInfo.put(item.getID(), item);
        }
    }

    /**
     * Get item from int id.
     * 
     * @param context
     * @param items
     * @return
     * @throws SQLException
     */
    private List<BrowsableDSpaceObject> getDSpaceObjects(Context context, List<UUID> items,
            int resourceTypeId) throws SQLException
    {
        List<BrowsableDSpaceObject> result = new ArrayList<BrowsableDSpaceObject>();
        for (UUID i : items)
        {
        	BrowsableDSpaceObject item = (BrowsableDSpaceObject)ContentServiceFactory.getInstance().getDSpaceObjectService(resourceTypeId).find(context, i);
            result.add(item);
        }
        return result;
    }

    private List<Item> getItems(Context context, List<UUID> items)
            throws SQLException
    {
        List<Item> result = new ArrayList<Item>();
        for (UUID i : items)
        {
            Item item = itemService.find(context, i);
            result.add(item);
        }
        return result;
    }

    /**
     * Get collections from int id.
     * 
     * @param context
     * @param items
     * @return
     * @throws SQLException
     */
    private List<Collection> getCollections(Context context, List<UUID> items,
            UUID target) throws SQLException
    {
        List<Collection> result = new ArrayList<Collection>();
        for (UUID item : items)
        {
			if (!item.equals((target)))
            {
                Collection collection = ContentServiceFactory.getInstance().getCollectionService().find(context, item);
                result.add(collection);
            }
        }
        return result;
    }

    /**
     * Process input from the cleaner duplicate form
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     */
    private void processUpdateItem(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item, Collection ownerCollection,
            List<Collection> otherCollections) throws ServletException,
                    IOException, SQLException, AuthorizeException
    {

        /* First, we remove it all, then build it back up again. */
        itemService.clearMetadata(context, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        // process collections
        if (ownerCollection != null)
        { // if exist collection owner check where
          // to insert item (on a
          // inprogresssubmission object direct
          // insert on db)
            WorkspaceItem trWsi = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context,item);
            if (trWsi == null)
            { // if item not in workspace then
              // check if item
              // is
              // in workflow state
            	BasicWorkflowItem trWfi = (BasicWorkflowItem)WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context, item);
                if (trWfi == null)
                {
					if (item.getOwningCollection() != null
							&& !item.getOwningCollection().getID().equals((ownerCollection.getID())))
                    {
                        itemService.move(context, item, item.getOwningCollection(), ownerCollection); // on
                                                                                // archived
                                                                                // item
                                                                                // move
                                                                                // collection
                                                                                // from
                                                                                // old
                                                                                // to
                                                                                // new
                    }
                }
                else
                {                	
                	trWfi.setCollection(ownerCollection);
                }
            }
            else
            {
                trWsi.setCollection(ownerCollection);
            }
        }

        // if item is archived then insert other collections
        if (item.isArchived())
        {
            Map<Collection, Boolean> toRemoveItem = new LinkedHashMap<Collection, Boolean>(); // this
                                                                                              // map
                                                                                              // contains
                                                                                              // collection
                                                                                              // (with
                                                                                              // value
                                                                                              // a
                                                                                              // true)
                                                                                              // where
                                                                                              // we
                                                                                              // goes
                                                                                              // to
                                                                                              // remove
                                                                                              // item
            if (otherCollections != null && !otherCollections.isEmpty())
            {
                for (Collection c : otherCollections)
                {
                    boolean founded = false;
                    for (Collection cc : item.getCollections())
                    {
                        if (c.getID() .equals(( cc.getID())))
                        {
                            founded = true;
                            toRemoveItem.put(cc, false);
                            break;
                        }
                        else
                        {
                            if (!toRemoveItem.containsKey(cc))
                            {
                                toRemoveItem.put(cc, true);
                            }
                        }
                    }
                    if (!founded)
                    {
                        c.getCollectionService().addItem(context, c, item);
                        c.getCollectionService().update(context, c);
                    }
                }
            }

            for (Collection cc : toRemoveItem.keySet())
            {
                if (!cc.getID() .equals(( ownerCollection.getID())))
                {
                    if (toRemoveItem.get(cc))
                    { // if collection is old remove
                      // item
                    	cc.getCollectionService().removeItem(context, cc, item);
                    	cc.getCollectionService().update(context, cc);
                    }
                }
            }
        }
        // read sorted row contains on table_values parameter
        String[] tablerows = request.getParameterValues("table_values");
        String result = "";
        String[] splitresult = null;
        for (String row : tablerows)
        {
            // warning 'table-selected' is table id on the view
            result = row.replaceAll("table\\-selected\\[\\]=", "");
            splitresult = result.split("&");
        }

        // for each row searching for "value_" parameters (with "hidden_value_"
        // prefix there are discarded metadata)
        for (String row : splitresult)
        {
            if (!row.isEmpty())
            {
                int index = row.indexOf("_");
                String p = "value" + row.substring(index);
                String parameter = request.getParameter(p);
                if (parameter != null && !parameter.isEmpty())
                {

                    /*
                     * It's a metadata value - it will be of the form
                     * value_element_1 OR value_element_qualifier_2 (the number
                     * being the sequence number) We use a StringTokenizer to
                     * extract these values
                     */
                    StringTokenizer st = new StringTokenizer(p, "_");

                    st.nextToken(); // Skip "value"

                    String schema = st.nextToken();

                    String element = st.nextToken();

                    String qualifier = null;

                    if (st.countTokens() == 2)
                    {
                        qualifier = st.nextToken();
                    }

                    String sequenceNumber = st.nextToken();

                    // Get a string with "element" for unqualified or
                    // "element_qualifier"
                    String key = MetadataField.formKey(schema, element,
                            qualifier);

                    // Get the language
                    String language = request.getParameter(
                            "language_" + key + "_" + sequenceNumber);

                    // Empty string language = null
                    if ((language != null) && language.equals(""))
                    {
                        language = null;
                    }

                    // Get the authority key if any
                    String authority = request.getParameter(
                            "choice_" + key + "_authority_" + sequenceNumber);

                    // Empty string authority = null
                    if ((authority != null) && authority.equals(""))
                    {
                        authority = null;
                    }

                    // Get the authority confidence value, passed as symbolic
                    // name
                    String sconfidence = request.getParameter(
                            "choice_" + key + "_confidence_" + sequenceNumber);
                    int confidence = (sconfidence == null
                            || sconfidence.equals("")) ? Choices.CF_NOVALUE
                                    : Choices.getConfidenceValue(sconfidence);

                    // Get the value
                    String value = parameter.trim();

                    // If remove button pressed for this value, we don't add it
                    // back to the item. We also don't add empty values
                    // (if no authority is specified).
                    if (!((value.equals("") && authority == null)))
                    {
                        // Value is empty, or remove button for this wasn't
                        // pressed
                        itemService.addMetadata(context, item, schema, element, qualifier, language,
                                value, authority, confidence);
                    }
                }
            }
        }

        // logic to discovery bitstream
        List<UUID> bitIDs = UIUtil.getUUIDParameters(request, "bitstream_id");        
		if (bitIDs != null) {
			
			BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

			List<DTOBitstream> toAdd = new ArrayList<>();
			if (!bitIDs.isEmpty()) {
				for (UUID bid : bitIDs) {
					Bitstream bb = bitstreamService.find(context, bid);
	                if (!bb.getBundles().get(0).getItems().get(0).getID().equals(item.getID()))
	                {
						DTOBitstream dtoBit = new DTOBitstream();
						dtoBit.create(bb.getID(), bb.getName(), bb.getSource(), bb.getDescription(), bb.getFormat(),
								bb.getUserFormatDescription(),
								AuthorizeServiceFactory.getInstance().getAuthorizeService().getPolicies(context, bb));
						dtoBit.setInputStream(bitstreamService.retrieve(context, bb));
						dtoBit.setBundleID(bb.getBundles().get(0).getID());
						dtoBit.setItemID(item.getID());
						toAdd.add(dtoBit);
	                }
				}
			}
			List<Bundle> originals = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
			List<Bitstream> toRemove = new ArrayList<>();
			//PREPARE
			for (Bundle orig : originals) {
				List<Bitstream> bits = orig.getBitstreams();
				for (Bitstream b : bits) {
					// bitstream in the target item has been unselect
					if (!bitIDs.contains(b.getID())) {
						toRemove.add(b);
					}
				}
			}
			//REMOVE			
			for(Bitstream bb : toRemove) {
				Bundle bundle = bb.getBundles().get(0);
				bundle.getBundleService().removeBitstream(context, bundle, bb);
				bundle.getBundleService().update(context, bundle);
			}

			Bundle orig = null;
			if (!bitIDs.isEmpty()) {
				if (originals.isEmpty()) {
					orig = ContentServiceFactory.getInstance().getBundleService().create(context, item,
							Constants.CONTENT_BUNDLE_NAME);
					ContentServiceFactory.getInstance().getBundleService().update(context, orig);
					// add read policy to the anonymous group
					AuthorizeServiceFactory.getInstance().getAuthorizeService().addPolicy(context, orig, Constants.READ,
							EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS));
				} else {
					orig = originals.get(0);
				}
				for (DTOBitstream b : toAdd) {

					// we need to add only bitstream that are not yet attached
					// to
					// the target item
					Bitstream newBits = bitstreamService.create(context, orig, b.getIs());

					// Now set the format and name of the bitstream
					newBits.setName(context, b.getName());
					newBits.setSource(context, b.getSource());
					newBits.setDescription(context, b.getDescription());
					bitstreamService.setFormat(context, newBits, b.getFormat());
					bitstreamService.setUserFormatDescription(context, newBits, b.getUserFormatDescription());
					bitstreamService.update(context, newBits);

					b.getIs().close();
					List<DTOResourcePolicy> rps = b.getRps();
					for (DTOResourcePolicy rp : rps) {
						ResourcePolicy newrp = AuthorizeServiceFactory.getInstance().getAuthorizeService()
								.createResourcePolicy(context, newBits, rp.getGroup(), rp.getEperson(), rp.getAction(),
										rp.getResourcePolicyType());
						newrp.setEndDate(rp.getEndDate());
						newrp.setStartDate(rp.getStartDate());
						resourcePolicyService.update(context, newrp);
					}
				}
			}
	        if (orig != null && orig.getBitstreams().isEmpty())
	        {
	            itemService.removeBundle(context, item, orig);
	        }
		}

        itemService.update(context, item);
    }

    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
    
    class DTOBitstream {
    	
    	DTOBitstreamData data = new DTOBitstreamData();

		public String getName() {
			return data.getName();
		}

		public void setInputStream(InputStream retrieve) {
			this.data.setIs(retrieve);
		}

		public void create(UUID id, String name2, String source2, String description2, BitstreamFormat format2,
				String userFormatDescription2, List<ResourcePolicy> policies) {
			this.data.setId(id);
			this.data.setName(name2);
			this.data.setSource(source2);
			this.data.setDescription(description2);
			this.data.setFormat(format2);
			this.data.setUserFormatDescription(userFormatDescription2);
			if (policies != null) {
				for (ResourcePolicy rp : policies) {
					DTOResourcePolicy dtorp = new DTOResourcePolicy();
					dtorp.create(rp.getGroup(), rp.getEPerson(), rp.getAction(), rp.getRpType(), rp.getStartDate(),
							rp.getEndDate());
					getRps().add(dtorp);
				}
			}
			
		}

		public void setName(String name) {
			this.data.setName(name);
		}

		public String getSource() {
			return data.getSource();
		}

		public void setSource(String source) {
			this.data.setSource(source);
		}

		public String getDescription() {
			return data.getDescription();
		}

		public void setDescription(String description) {
			this.data.setDescription(description);
		}

		public BitstreamFormat getFormat() {
			return data.getFormat();
		}

		public void setFormat(BitstreamFormat format) {
			this.data.setFormat(format);
		}

		public String getUserFormatDescription() {
			return data.getUserFormatDescription();
		}

		public void setUserFormatDescription(String userFormatDescription) {
			this.data.setUserFormatDescription(userFormatDescription);
		}

		public List<DTOResourcePolicy> getRps() {			
			return data.getRps();
		}

		public void setRps(List<DTOResourcePolicy> rps) {
			this.data.setRps(rps);
		}

		public UUID getItemID() {
			return data.getItemID();
		}

		public void setItemID(UUID itemID) {
			this.data.setItemID(itemID);
		}

		public UUID getBundleID() {
			return data.getBundleID();
		}

		public void setBundleID(UUID bundleID) {
			this.data.setBundleID(bundleID);
		}

		public UUID getId() {
			return data.getId();
		}

		public void setId(UUID id) {
			this.data.setId(id);
		}

		public InputStream getIs() {
			return data.getIs();
		}

		public void setIs(InputStream is) {
			this.data.setIs(is);
		}
    }
    
    class DTOResourcePolicy {
    	
		DTOResourcePolicyData data = new DTOResourcePolicyData();

		public Group getGroup() {
			return data.getGroup();
		}
		public void create(Group group2, EPerson ePerson2, int action2, String rpType, Date startDate2, Date endDate2) {
			this.data.setGroup(group2);
			this.data.setEperson(ePerson2);
			this.data.setAction(action2);
			this.data.setResourcePolicyType(rpType);
			this.data.setStartDate(startDate2);
			this.data.setEndDate(endDate2);			
		}
		
		public void setGroup(Group group) {
			this.data.setGroup(group);
		}
		public EPerson getEperson() {
			return data.getEperson();
		}
		public void setEperson(EPerson eperson) {
			this.data.setEperson(eperson);
		}
		public int getAction() {
			return data.getAction();
		}
		public void setAction(int action) {
			this.data.setAction(action);
		}
		public String getResourcePolicyType() {
			return data.getResourcePolicyType();
		}
		public void setResourcePolicyType(String resourcePolicyType) {
			this.data.setResourcePolicyType(resourcePolicyType);
		}
		public Date getStartDate() {
			return data.getStartDate();
		}
		public void setStartDate(Date startDate) {
			this.data.setStartDate(startDate);
		}
		public Date getEndDate() {
			return data.getEndDate();
		}
		public void setEndDate(Date endDate) {
			this.data.setEndDate(endDate);
		}
    }
}
