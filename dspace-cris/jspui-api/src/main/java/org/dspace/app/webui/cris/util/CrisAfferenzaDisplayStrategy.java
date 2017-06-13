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

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class CrisAfferenzaDisplayStrategy implements
        IDisplayMetadataValueStrategy
{

    private DSpace dspace = new DSpace();

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph)
    {
    	ACrisObject crisObject = (ACrisObject)item;
        return internalDisplay(field, crisObject);
    }

    private String internalDisplay(String field, ACrisObject crisObject)
    {
        String[] splitted = field.split("\\.");
        //FIXME apply aspectjproxy???
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);
        List<RPNestedObject> anos = applicationService
                .getNestedObjectsByParentIDAndShortname(crisObject.getId(),
                        splitted[1], crisObject.getClassNested());
        for (RPNestedObject ano : anos)
        {
            List<RPNestedProperty> props = ano.getAnagrafica4view().get(splitted[2]);
            for(RPNestedProperty prop : props) {
                return prop.toString();
            }
        }
        return "";
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
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, String field, List<IMetadataValue> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException {
        ACrisObject crisObject = (ACrisObject)item;
        return internalDisplay(field, crisObject);
	}

	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks, boolean emph)
			throws JspException {
		return null;
	}
}

