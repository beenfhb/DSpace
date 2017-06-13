/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class CrisCommunityServiceKeynoteTitleDisplayStrategy implements IDisplayMetadataValueStrategy {

	/**
	 * log4j category
	 */
	public static final Log log = LogFactory.getLog(CrisCommunityServiceKeynoteTitleDisplayStrategy.class);

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
			String field, List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks, boolean emph) {
		// not used
		return null;
	}
	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks, boolean emph) throws JspException {
		return null;
	}

	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem, boolean disableCrossLinks, boolean emph) throws JspException {
		return null;
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
            metadata += prepareName(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += preparePlaceOfConference(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += prepareDateOfConference(hrq, metadataArray, crisObject, publicPath,
                    authority);
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

}
