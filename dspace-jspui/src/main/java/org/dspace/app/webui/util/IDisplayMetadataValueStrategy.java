/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.discovery.IGlobalSearchResult;

public interface IDisplayMetadataValueStrategy
{
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph)
            throws JspException;

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException;

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem,
            boolean disableCrossLinks, boolean emph)
            throws JspException;

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException;

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph)
            throws JspException;
    
}
