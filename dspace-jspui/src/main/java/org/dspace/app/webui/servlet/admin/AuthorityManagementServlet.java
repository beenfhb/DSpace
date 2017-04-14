/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.StringConfigurationComparator;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.AuthorityDAO;
import org.dspace.content.authority.AuthorityDAOFactory;
import org.dspace.content.authority.AuthorityInfo;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;

/**
 * 
 * @author bollini
 */
public class AuthorityManagementServlet extends DSpaceServlet
{
    public static final int AUTHORITY_KEYS_LIMIT = 20;

    /** log4j category */
    private static Logger log = Logger
            .getLogger(AuthorityManagementServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String issued = request.getParameter("issued");
        String authority = request.getParameter("authority");
        String authkey = request.getParameter("key");

        if (authority != null)
        {
            if (authkey != null)
            {
                doAuthorityKeyDetailsForAuthority(context, authority, authkey, request, response);
            }
            else
            {
                doAuthorityIssuedForAuthority(context, authority, request, response);
            }
        }
        else
        {
            if (authkey != null && issued != null)
            {
                doAuthorityKeyDetails(context, issued, authkey, request, response);
            }
            else if (issued != null)
            {
                doAuthorityIssued(context, issued, request, response);
            }
            else
            {
                doMainPage(context, request, response);
            }
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String key = request.getParameter("key");
        String issuedParam = request.getParameter("issued");
        String authority = request.getParameter("authority");
        
        List<String> metadataList;
        ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        if (authority != null)
        {
            metadataList = cam.getAuthorityMetadataForAuthority(authority);
        }
        else
        {
            metadataList = new LinkedList<String>();
            metadataList.add(issuedParam);
        }
        
        int[] items_uncertain = UIUtil.getIntParameters(request,
                "items_uncertain");
        int[] items_ambiguos = UIUtil.getIntParameters(request,
                "items_ambiguos");
        int[] items_novalue = UIUtil.getIntParameters(request, "items_novalue");
        int[] items_failed = UIUtil.getIntParameters(request, "items_failed");
        int[] items_unset = UIUtil.getIntParameters(request, "items_unset");
        int[] items_reject = UIUtil.getIntParameters(request, "items_reject");
        int[] items_notfound = UIUtil.getIntParameters(request, "items_notfound");
        
        int[][] items = { items_uncertain, items_ambiguos, items_notfound,
                items_failed, items_unset, items_reject, items_novalue };
        
        final String submitButton = UIUtil.getSubmitButton(request,
                "submit_accept");

        Set<Integer> itemRejectedIDs = new HashSet<Integer>();
        for (int[] items_uanfur : items)
        {
            if (items_uanfur!=null)
            {
                for (int itemID : items_uanfur)
                {
                    Item item = ContentServiceFactory.getInstance().getItemService().findByLegacyId(context, itemID);
                    
                    for (String issued : metadataList)
                    {
                        String[] metadata = issued.split("\\.");
                        List<MetadataValue> original = item.getMetadataValueInDCFormat(issued);
                        item.getItemService().clearMetadata(context, item, metadata[0], metadata[1],
                                metadata.length > 2 ? metadata[2] : null, Item.ANY);
                        for (MetadataValue md : original)
                        {
                            if (key.equals(md.getAuthority()))
                            {
                                if ("submit_accept".equalsIgnoreCase(submitButton))
                                {
                                    log.debug(LogManager.getHeader(context,
                                            "confirm_authority_key", "item_id: "
                                                    + itemID + ", authority_key: "
                                                    + key));
                                    md.setConfidence(Choices.CF_ACCEPTED);
                                }
                                else
                                {
                                    log.debug(LogManager.getHeader(context,
                                            "reject_authority_key", "item_id: "
                                                    + itemID + ", authority_key: "
                                                    + key));
                                    md.setConfidence(Choices.CF_UNSET);
                                    md.setAuthority(null);
                                    itemRejectedIDs.add(itemID);
                                }
                            }
                            item.getItemService().addMetadata(context, item, md.schema, md.element, md.qualifier,
                                    md.getLanguage(), md.getValue(), md.getAuthority(),
                                    md.getConfidence());
                        }
                    }
                    item.getItemService().update(context, item);
                }
            }
        }

        context.commit();
        if (itemRejectedIDs.size() > 0)
        {
            // notify reject
            int[] ids = new int[itemRejectedIDs.size()];
            Iterator<Integer> iter = itemRejectedIDs.iterator();
            int i = 0;
            while (iter.hasNext())
            {
                ids[i] = (Integer) iter.next();
                i++;
            }
            
            String[] splitted = metadataList.get(0).split("\\.");
            String schema = splitted[0];
            String element = splitted[1];
            String qualifier = (splitted.length == 3)?splitted[2]:null;
            cam
                    .notifyReject(ids, schema, element, qualifier, key);
        }
        log.info(LogManager.getHeader(context, "validate_authority_key",
                "action: " + submitButton + " #items: " + items.length));
        String message = I18nUtil.getMessage(
                "org.dspace.app.webui.AuthorityManagementServlet."
                        + submitButton, UIUtil.getSessionLocale(request));
        request.getSession().setAttribute("authority.message", message);
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        
        if (authority != null)
        {
            long numIssuedItems = dao.countIssuedItemsByAuthorityValueInAuthority(authority, key);
            if (numIssuedItems > 0)
            {
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/authority?key="
                        + URLEncoder.encode(key, "UTF-8") + "&authority=" + authority);
            }
            else
            {
                long numIssuedKeys = dao.countIssuedAuthorityKeysByAuthority(authority);
                if (numIssuedKeys > 0)
                {
                    // search the next authority key to process...
                    String authkey = dao.findNextIssuedAuthorityKeyInAuthority(authority, key);
                    if (authkey == null)
                    { // there is no next... go back!
                        authkey = dao.findPreviousIssuedAuthorityKeyInAuthority(authority, key);
                    }
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority?" + "&authority=" + authority
                            + "&key=" + URLEncoder.encode(authkey, "UTF-8"));
                }
                else
                {
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority");
                }
            }
        }
        else
        {
            long numIssuedItems = dao.countIssuedItemsByAuthorityValue(metadataList.get(0), key);
            if (numIssuedItems > 0)
            {
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/authority?key="
                        + URLEncoder.encode(key, "UTF-8") + "&issued=" + issuedParam);
            }
            else
            {
                long numIssuedKeys = dao.countIssuedAuthorityKeys(issuedParam);
                if (numIssuedKeys > 0)
                {
                    // search the next authority key to process...
                    String authkey = dao.findNextIssuedAuthorityKey(issuedParam, key);
                    if (authkey == null)
                    { // there is no next... go back!
                        authkey = dao.findPreviousIssuedAuthorityKey(issuedParam, key);
                    }
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority?" + "&issued=" + issuedParam
                            + "&key=" + URLEncoder.encode(authkey, "UTF-8"));
                }
                else
                {
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority");
                }
            }
        }
    }

    private void doMainPage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SQLException,
            ServletException, IOException
    {
        boolean detail = UIUtil.getBoolParameter(request, "detail");
        
        
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        Map<String, AuthorityInfo> infos = new HashMap<String, AuthorityInfo>();
        Comparator<String> configurationKeyComparator = new StringConfigurationComparator("authority.management.order.");
        
        if (detail)
        {
        	MetadataAuthorityService mam = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
            List<String> authorityMetadata = new ArrayList<String>();
            List<String> tmpAuthorityMetadata = mam.getAuthorityMetadata();
            final String authManagementPrefix = "authority.management.";
            for(String tmp : tmpAuthorityMetadata) {
                boolean req = ConfigurationManager.getBooleanProperty(
                        authManagementPrefix + tmp, true);
                if(req) {
                    authorityMetadata.add(tmp);
                }
            }
            Collections.sort(authorityMetadata, configurationKeyComparator);
            
            request.setAttribute("authorities", authorityMetadata);
    
            for (String md : authorityMetadata)
            {
                AuthorityInfo info = dao.getAuthorityInfo(md);
                infos.put(md, info);
            }
            request.setAttribute("infos", infos);
    
            // add RP set # total item in HUB
            long numItems = (Integer)(dao.getHibernateSession().createSQLQuery("select count(*) as count from Item where in_archive = true").addScalar("count").uniqueResult());
            request.setAttribute("numItems", numItems);
    
            log.info(LogManager.getHeader(context, "show_main_page",
                    "#authorities: " + authorityMetadata.size()));
            JSPManager.showJSP(request, response, "/dspace-admin/authority.jsp");//XXX
        }
        else
        {
            ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
            Set<String> authorityNames = cam.getAuthorities();
            List<String> listnames = new LinkedList<String>();
            
            for (String authorityName : authorityNames)
            {
                AuthorityInfo info = dao.getAuthorityInfoByAuthority(authorityName);
                infos.put(authorityName, info);
                listnames.add(authorityName);
            }
            
            Collections.sort(listnames, configurationKeyComparator);
            request.setAttribute("authorities", listnames);
            request.setAttribute("infos", infos);
    
            // add RP set # total item in HUB
            long numItems = (Integer)(dao.getHibernateSession().createSQLQuery("select count(*) as count from Item where in_archive = true").addScalar("count").uniqueResult());
            request.setAttribute("numItems", numItems);
    
            log.info(LogManager.getHeader(context, "show_main_page",
                    "#authorities file: " + authorityNames.size()));
            JSPManager.showJSP(request, response, "/dspace-admin/authority.jsp");
        }
    }

    private void doAuthorityIssued(Context context, String issued,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException
    {
    	ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        int page = UIUtil.getIntParameter(request, "page");
        if (page < 0)
        {
            page = 0;
        }
        List<String> keys = dao.listAuthorityKeyIssued(issued,
                AUTHORITY_KEYS_LIMIT, page);
        List<String[]> authoritiesIssued = new ArrayList<String[]>();// "authoritiesIssued"
        for (String key : keys)
        {
            authoritiesIssued.add(new String[] {
                    key,
                    cam.getLabel(issued.replaceAll("\\.", "_"), key, UIUtil
                            .getSessionLocale(request).toString()) });
        }
        request.setAttribute("authoritiesIssued", authoritiesIssued);
        request.setAttribute("totAuthoritiesIssued", Long.valueOf(dao
                .countIssuedAuthorityKeys(issued)));
        request.setAttribute("currPage", Integer.valueOf(page));
        log.info(LogManager.getHeader(context, "show_authority_issues",
                "metadata: " + issued + ", #keys: " + keys.size()));

        JSPManager.showJSP(request, response,
                "/dspace-admin/authority-issued.jsp");
    }

    private void doAuthorityKeyDetails(Context context, String issued,
            String authkey, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	MetadataAuthorityService mam = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        
		List<Item> items_uncertain = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_UNCERTAIN);
		List<Item> items_ambiguos = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_AMBIGUOUS);
		List<Item> items_novalue = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_NOVALUE);
		List<Item> items_failed = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_FAILED);
		List<Item> items_notfound = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_NOTFOUND);
		List<Item> items_unset = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_UNSET);
		List<Item> items_reject = mam.findIssuedByAuthorityValueAndConfidence(issued, authkey, Choices.CF_REJECTED);

        String label = cam.getLabel(issued.replaceAll("\\.", "_"), authkey,
                UIUtil.getSessionLocale(request).toString());
        String[] md = issued.split("\\.");

        MetadataValue metadataValue = new MetadataValue(md[0], md[1],
                md.length > 2 ? md[2] : null, authkey, UIUtil.getSessionLocale(
                        request).toString());
        List<String> variants = cam.getVariants(metadataValue);

        String nextKey = mam.findNextIssuedAuthorityKey(issued, authkey);
        String prevKey = mam.findPreviousIssuedAuthorityKey(issued, authkey);

        request.setAttribute("items_uncertain", items_uncertain);
        request.setAttribute("items_ambiguos", items_ambiguos);
        request.setAttribute("items_novalue", items_novalue);
        request.setAttribute("items_failed", items_failed);
        request.setAttribute("items_notfound", items_notfound);
        request.setAttribute("items_unset", items_unset);
        request.setAttribute("items_reject", items_reject);
        request.setAttribute("authKey", authkey);
        request.setAttribute("label", label);
        request.setAttribute("variants", variants);
        request.setAttribute("next", nextKey);
        request.setAttribute("previous", prevKey);
        request.setAttribute("required", mam.isAuthorityRequired(issued));

        log.info(LogManager.getHeader(context, "show_key_issues", "metadata: "
                + issued + ", key: " + authkey + ", #items: "
                + items_uncertain.size() + items_ambiguos.size()
                + items_novalue.size() + items_failed.size()
                + items_unset.size()));

        JSPManager
                .showJSP(request, response, "/dspace-admin/authority-key.jsp");
    }
    
    private void doAuthorityKeyDetailsForAuthority(Context context,
            String authority, String authkey, HttpServletRequest request,
            HttpServletResponse response) throws SQLException, AuthorizeException, IOException, ServletException
    {
    	MetadataAuthorityService mam = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);

        List<Item> arrItems_uncertain = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_UNCERTAIN);
        List<Item> arrItems_ambiguos = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_AMBIGUOUS);
        List<Item> arrItems_novalue = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_NOVALUE);
        List<Item> arrItems_failed = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_FAILED);
        List<Item> arrItems_notfound = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority,
                        authkey, Choices.CF_NOTFOUND);
        List<Item> arrItems_unset = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_UNSET);
        List<Item> arrItems_reject = mam
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_REJECTED);

        String mdString = (String)(cam.getAuthorityMetadataForAuthority(authority).get(0));
		String label = cam.getLabel(mdString.replaceAll("\\.", "_"), authkey,
                UIUtil.getSessionLocale(request).toString());
        String[] md = mdString.split("\\.");

        MetadataValue metadataValue = new MetadataValue(md[0], md[1],
                md.length > 2 ? md[2] : null, authkey, UIUtil.getSessionLocale(
                        request).toString());
        List<String> variants = cam.getVariants(metadataValue);

        String nextKey = mam.findNextIssuedAuthorityKeyInAuthority(authority, authkey);
        String prevKey = mam.findPreviousIssuedAuthorityKeyInAuthority(authority, authkey);

        request.setAttribute("items_uncertain", arrItems_uncertain);
        request.setAttribute("items_ambiguos", arrItems_ambiguos);
        request.setAttribute("items_novalue", arrItems_novalue);
        request.setAttribute("items_failed", arrItems_failed);
        request.setAttribute("items_notfound", arrItems_notfound);
        request.setAttribute("items_unset", arrItems_unset);
        request.setAttribute("items_reject", arrItems_reject);
        request.setAttribute("authKey", authkey);
        request.setAttribute("label", label);
        request.setAttribute("variants", variants);
        request.setAttribute("next", nextKey);
        request.setAttribute("previous", prevKey);
        request.setAttribute("required", mam.isAuthorityRequired(mdString));

        log.info(LogManager.getHeader(context, "show_key_issues", "authority: "
                + authority + ", key: " + authkey + ", #items: "
                + arrItems_uncertain.size() + arrItems_ambiguos.size()
                + arrItems_novalue.size() + arrItems_failed.size()
                + arrItems_unset.size()));

        JSPManager
                .showJSP(request, response, "/dspace-admin/authority-key.jsp");
    }

    private void doAuthorityIssuedForAuthority(Context context,
            String authority, HttpServletRequest request,
            HttpServletResponse response) throws SQLException, ServletException, IOException
    {
    	ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        int page = UIUtil.getIntParameter(request, "page");
        if (page < 0)
        {
            page = 0;
        }
        List<String> keys = dao.listAuthorityKeyIssuedByAuthority(authority,
                AUTHORITY_KEYS_LIMIT, page);
        List<String[]> authoritiesIssued = new ArrayList<String[]>();// "authoritiesIssued"
        for (String key : keys)
        {
            authoritiesIssued.add(new String[] {
                    key,
                    cam.getLabel(cam.getAuthorityMetadataForAuthority(authority).get(0).replaceAll("\\.", "_"), key, UIUtil
                            .getSessionLocale(request).toString()) });
        }
        request.setAttribute("authoritiesIssued", authoritiesIssued);
        request.setAttribute("totAuthoritiesIssued", Long.valueOf(dao
                .countIssuedAuthorityKeysByAuthority(authority)));
        request.setAttribute("currPage", Integer.valueOf(page));
        log.info(LogManager.getHeader(context, "show_authority_issues",
                "authority: " + authority + ", #keys: " + keys.size()));

        JSPManager.showJSP(request, response,
                "/dspace-admin/authority-issued.jsp");
        
    }
}
