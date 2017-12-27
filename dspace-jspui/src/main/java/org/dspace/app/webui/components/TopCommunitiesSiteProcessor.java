/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

/**
 * This class add top communities object to the request attributes to use in
 * the site home page implementing the SiteHomeProcessor.
 * 
 * @author Andrea Bollini
 * 
 */
public class TopCommunitiesSiteProcessor implements SiteHomeProcessor
{

	private CommunityService communityService;
	
    /**
     * blank constructor - does nothing.
     * 
     */
    public TopCommunitiesSiteProcessor()
    {
    	 communityService = ContentServiceFactory.getInstance().getCommunityService();
    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response) throws PluginException,
            AuthorizeException
    {
        // Get the top communities to shows in the community list
        List<Community> communities;
        List<Community> topCom;
        try
        {
            communities = communityService.findAllTop(context);
            topCom = new ArrayList<Community>();
            String showCrisComm = ConfigurationManager.getProperty("community-list.topcommunity.show");

            if(AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context) || 
                    StringUtils.equalsIgnoreCase(showCrisComm, "all") ||
                    ( context.getCurrentUser() != null && StringUtils.equalsIgnoreCase(showCrisComm, "user") ) ){
                for (Community com : communities)
                {
                    topCom.add(com);
                }
            }else{                
            	for (Community com : communities)
                {
                    List<Group> groups = AuthorizeServiceFactory.getInstance().getAuthorizeService().getAuthorizedGroups(context, com, Constants.READ);
                    for(Group group : groups){
                        if(group.getName().equals(Group.ADMIN) || EPersonServiceFactory.getInstance().getGroupService().isMember(context, group)){
                            topCom.add(com);
                            break;
                        }
                    }
                }
            }
            
        }
        catch (SQLException e)
        {
            throw new PluginException(e.getMessage(), e);
        }
        request.setAttribute("communities", communities);
    }

}
