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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
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

public class CrisRefDisplayStrategy implements IDisplayMetadataValueStrategy
{

	/**
	 * log4j category
	 */
    public static final Log log = LogFactory
            .getLog(CrisRefDisplayStrategy.class);

    private ApplicationService applicationService = new DSpace()
            .getServiceManager()
			.getServiceByName("applicationService", ApplicationService.class);
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph)
    {
    	ACrisObject crisObject = (ACrisObject)item;
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph)
    {
        if (metadataArray != null && metadataArray.size() > 0)
        {
            String authority = metadataArray.get(0).getAuthority();
            if (StringUtils.isNotBlank(authority))
            {
                ACrisObject entityByCrisId = applicationService
                        .getEntityByCrisId(authority);
                return internalDisplay(hrq, metadataArray, entityByCrisId);
            } else {
                return metadataArray.get(0).getValue();
            }
	    }
		return "N/D";
	}
    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
		return null;
	}

	@Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem,
            boolean disableCrossLinks, boolean emph)
                    throws JspException
    {
		return null;
	}

	@Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            List<IMetadataValue> metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph)
                    throws JspException
    {

		ACrisObject crisObject = (ACrisObject) item;
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
	
    private String internalDisplay(HttpServletRequest hrq,
            List<IMetadataValue> metadataArray, ACrisObject crisObject)
    {
        String metadata = "N/A";
        if (metadataArray != null && metadataArray.size() > 0)
        {
			try 
			{
				String publicPath = crisObject.getAuthorityPrefix();
				String authority = crisObject.getCrisID();

				String target = ConfigurationManager.getBooleanProperty("cris",
						"researcher.cris." + publicPath + ".ref.display.strategy.target.blank", false)
								? "target=\"_blank\" " : "";
				String startLink = "<a " + target + "href=\"" + hrq.getContextPath() + "/cris/" + publicPath + "/"
						+ authority;
				startLink += "\" class=\"authority\">";
				String endLink = "</a>";
            
				String icon = "";
                try
                {
					// perhaps this is to avoid a lazyloader exception?
					ACrisObject rp = applicationService.getEntityByCrisId(authority, crisObject.getClass());
					String metadataIcon = ConfigurationManager.getProperty("cris",
                            "researcher.cris." + publicPath + ".ref.display.strategy.metadata.icon");
					if(StringUtils.isBlank(metadataIcon)) {
					    throw new RuntimeException("No metadata configuration to retrieve icon for object: " + publicPath);
					}
					String type = rp.getMetadata(metadataIcon);
					String status = ""; 
					if (rp == null || !rp.getStatus()) {
						startLink = "";
						endLink = "";
						status = "private.";
					}

					String title;
					try {
						title = I18nUtil
								.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + status + type + ".title", true);
					}
					catch (MissingResourceException e2)
                    {
						title = I18nUtil
								.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + type + ".title");
                    }
					
					try {
						icon = MessageFormat.format(
								I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + status + type + ".icon", true),
								title);
					}
					catch (MissingResourceException e2)
                    {
						icon = MessageFormat.format(
								I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + type + ".icon"),
								title);
                    }
                }
                catch (Exception e)
                {
                    log.warn(
                            "Error when build icon (perhaps missing this configuration: on cris module key:researcher.cris."
                                    + publicPath
                                    + ".ref.display.strategy.metadata.icon, " + e.getMessage());                            
                    try
                    {
                        icon = I18nUtil.getMessage("ItemCrisRefDisplayStrategy."
                                + publicPath + ".icon", true);
                        log.info(
                                "Retrieved for "
                                        + publicPath
                                        + "ItemCrisRefDisplayStrategy."
                                                + publicPath + ".icon");      
                    }
                    catch (MissingResourceException e2)
                    {
                        icon = I18nUtil.getMessage(
                                "ItemCrisRefDisplayStrategy.default.icon");
                        log.info(
                                "Retrieved for "
                                        + publicPath
                                        + "ItemCrisRefDisplayStrategy.default.icon");
                    }
                }
                metadata = startLink;
                metadata += Utils.addEntities(metadataArray.get(0).getValue());
                metadata += "&nbsp;";
                metadata += icon;
                metadata += endLink;
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        return metadata;
    }

}
