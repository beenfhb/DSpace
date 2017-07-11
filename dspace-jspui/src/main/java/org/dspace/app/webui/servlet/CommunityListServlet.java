/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.factory.EPersonServiceFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Servlet for listing communities (and collections within them)
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class CommunityListServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(CommunityListServlet.class);

    // services API
    private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // This will map community IDs to arrays of collections
    	Map<String, List<Collection>> colMap;

        // This will map communityIDs to arrays of sub-communities
        Map<String, List<Community>> commMap;

        colMap = new HashMap<>();
        commMap = new HashMap<>();

        log.info(LogManager.getHeader(context, "view_community_list", ""));
			List<Community> communities = communityService.findAllTop(context);
            String showCrisComm = ConfigurationManager.getProperty("community-list.topcommunity.show");

        	if (authorizeService.isAdmin(context) &&
        			StringUtils.equalsIgnoreCase(showCrisComm, "all") ||
        			( context.getCurrentUser() != null && StringUtils.equalsIgnoreCase(showCrisComm, "user") ) ){
        		for (Community c : communities) 
        		{
        			build(c, colMap, commMap);
        		}
            }else{
        		List<Community> topCom = new ArrayList<Community>();
        		for (Community c : communities) 
        		{
            		List<Group> groups = authorizeService.getAuthorizedGroups(context, c, Constants.READ);
            		for(Group group : groups){
            			if(group.getName().equals(Group.ANONYMOUS) || EPersonServiceFactory.getInstance().getGroupService().isMember(context, group)){
            				build(c, colMap, commMap);
            				topCom.add(c);
            				break;
            			}
            		}
        		}
                communities = topCom;
            }

        
        // can they admin communities?
        if (authorizeService.isAdmin(context))
        {
            // set a variable to create an edit button
            request.setAttribute("admin_button", Boolean.TRUE);
        }

        EPerson currUser = context.getCurrentUser();
        List<Subscription> commIDsubs = new ArrayList<Subscription>();
        if (currUser != null)
        {
            commIDsubs = EPersonServiceFactory.getInstance().getSubscribeService().getSubscriptionsCommunity(context, currUser);
            
        }
        request.setAttribute("subscription_communities", commIDsubs);
        
        request.setAttribute("communities", communities);
        request.setAttribute("collections.map", colMap);
        request.setAttribute("subcommunities.map", commMap);
        JSPManager.showJSP(request, response, "/community-list.jsp");
    }
    /*
     * Get all subcommunities and collections from a community
     */
	private void build(Community c, Map<String, List<Collection>> colMap, Map<String, List<Community>> commMap)
			throws SQLException {

        String comID = c.getID().toString();

        // Find collections in community
        List<Collection> colls = c.getCollections();
        colMap.put(comID, colls);

        // Find subcommunties in community
        List<Community> comms = c.getSubcommunities();
        
        // Get all subcommunities for each communities if they have some
        if (comms.size() > 0) 
        {
            commMap.put(comID, comms);
            
            for (Community sub : comms) {
                
                build(sub, colMap, commMap);
            }
        }
    }
}
