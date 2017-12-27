/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class CrisCommunityServiceDisplayStrategy implements IDisplayMetadataValueStrategy {

	/**
	 * log4j category
	 */
	public static final Log log = LogFactory.getLog(CrisCommunityServiceDisplayStrategy.class);

	private ApplicationService applicationService = new DSpace().getServiceManager()
			.getServiceByName("applicationService", ApplicationService.class);

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, BrowsableDSpaceObject item, boolean disableCrossLinks, boolean emph) {
		ACrisObject crisObject = (ACrisObject)item;
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks, boolean emph) throws JspException {

		ACrisObject crisObject = (ACrisObject) item;
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
	
    private String internalDisplay(HttpServletRequest hrq,
    		List<IMetadataValue> metadataArray, ACrisObject crisObject)
    {
        String metadata = "N/A";
        if (metadataArray!=null && metadataArray.size() > 0) {
            String publicPath = crisObject.getAuthorityPrefix();
            String authority = crisObject.getCrisID();
            
            metadata = "";
            metadata = prepareTitle(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += prepareName(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += preparePlaceOfConference(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += prepareDateOfConference(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += prepareContributors(hrq, metadataArray, crisObject, publicPath,
                    authority);
        }
        return metadata;
    }

    private String prepareContributors(HttpServletRequest hrq,
    		List<IMetadataValue> metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        String metadata = "</br>";
        List<IMetadataValue> contributors = crisObject.getMetadataValueInDCFormat("communityservicecontributors");
        for(IMetadataValue IMetadataValue : contributors) {
            String auth = IMetadataValue.getAuthority();
            if(StringUtils.isNotBlank(auth)) {
                ACrisObject rp = applicationService.getEntityByCrisId(auth, ResearcherPage.class);                
                metadata += internalContributorsDisplay(hrq, IMetadataValue, rp);
                metadata += "</br>";
            }
            
        }        
        return metadata;
    }
    
    private String internalContributorsDisplay(HttpServletRequest hrq,
    		IMetadataValue IMetadataValue, ACrisObject crisObject)
    {
        String metadata = "N/A";
        if (IMetadataValue!=null) {
            String publicPath = crisObject.getAuthorityPrefix();
            String authority = crisObject.getCrisID();

            String startLink = "&nbsp;<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/cris/" + publicPath + "/"
                    + authority;
            startLink += "\" class=\"authority\">";
            String endLink = "</a>";
            metadata = startLink;
            String icon = "";
            try {
                //perhaps this is to avoid a lazyloader exception?                
                String type = crisObject.getMetadata(ConfigurationManager.getProperty("cris",
                        "researcher.cris."+publicPath+".ref.display.strategy.metadata.icon"));

                String title = I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + type + ".title");
                icon = MessageFormat.format(
                        I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + type + ".icon"), title);
            } catch (Exception e) {
                log.error(
                        "Error when build icon (perhaps missing this configuration: on cris module key:researcher.cris."+publicPath+".ref.display.strategy.metadata.icon)",
                        e);
                try {
                    icon = I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + ".icon");
                } catch (MissingResourceException e2) {
                    icon = I18nUtil.getMessage("ItemCrisRefDisplayStrategy.default.icon");
                }
            }
            metadata += Utils.addEntities(IMetadataValue.getValue());
            metadata += "&nbsp;";
            metadata += icon;
            metadata += endLink;
        }
        return metadata;
    }


    private String prepareDateOfConference(HttpServletRequest hrq,
    		List<IMetadataValue> metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        String metadata = "";
        List<String> title = crisObject.getMetadataValue("communityserviceconferencedate");
        for(String value : title) {metadata = ",&nbsp;";
            metadata += Utils.addEntities(value);
        }
        return metadata;
    }

    private String preparePlaceOfConference(HttpServletRequest hrq,
    		List<IMetadataValue> metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        String metadata = "";
        List<String> title = crisObject.getMetadataValue("communityserviceconferenceplace");
        for(String value : title) {
            metadata = ",&nbsp;";
            metadata += Utils.addEntities(value);
        }
        return metadata;
    }

    private String prepareTitle(HttpServletRequest hrq,
    		List<IMetadataValue> metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        
        String metadata = "";
        List<String> title = crisObject.getMetadataValue("communityservicetitle");
        for(String value : title) {
            metadata += Utils.addEntities(value);
            metadata += "</br>";
        }        
        return metadata;
    }

    private String prepareName(HttpServletRequest hrq,
    		List<IMetadataValue> metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        String metadata;
        String startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/cris/" + publicPath + "/"
                + authority;
        startLink += "\" class=\"authority\">";
        String endLink = "</a>";
        metadata = startLink;
        metadata += Utils.addEntities(metadataArray.get(0).getValue());
        metadata += endLink;
        return metadata;
    }

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks, boolean emph)
			throws JspException {
		// noop
		return null;
	}

	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem, boolean disableCrossLinks,
			boolean emph) throws JspException {
		// noop
		return null;
	}

	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks, boolean emph)
			throws JspException {
		// noop
		return null;
	}

}
