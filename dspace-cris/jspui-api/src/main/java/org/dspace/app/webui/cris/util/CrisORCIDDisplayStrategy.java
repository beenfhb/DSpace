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

import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.IGlobalSearchResult;

public class CrisORCIDDisplayStrategy implements IDisplayMetadataValueStrategy
{
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph)
    {
        String metadata = "";
        metadata = internalDisplay(hrq, metadataArray, metadata);
        return metadata;
    }

    private String internalDisplay(HttpServletRequest hrq,
            List<IMetadataValue> metadataArray, String metadata)
    {
        if (metadataArray!=null && metadataArray.size() > 0)
        {
		    String externalContextPath = ConfigurationManager.getProperty("cris","external.domainname.authority.service.orcid");
			metadata += "<a target=\"_blank\" href=\"" + externalContextPath + metadataArray.get(0).getValue();
			metadata += "\" class=\"authority\">&nbsp;<img style=\"width: 16px; height: 16px;\" src=\""+ hrq.getContextPath() +"/images/mini-icon-orcid.png\" alt=\"\">";
			metadata += "</a>";

        }
        return metadata;
    }
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph)
    {
        // not used
        return null;
    }
    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem,
            boolean disableCrossLinks, boolean emph)
            throws JspException
    {
        return null;
    }
    
	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, String field, List<IMetadataValue> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException {
        String metadata = "";
        metadata = internalDisplay(hrq, metadataArray, metadata);
        return metadata;
	}
}
