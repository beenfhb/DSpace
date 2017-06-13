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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.tag.JDynATagLibraryFunctions;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;

public class CrisDropdownDisplayStrategy implements
        IDisplayMetadataValueStrategy
{

    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(CrisDropdownDisplayStrategy.class);
    
    private ApplicationService applicationService = new DSpace()
            .getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph)
    {
    	ACrisObject crisObject = (ACrisObject)item;
        String metadata = internalDisplay(hrq, metadataArray, crisObject, field);
        return metadata;
    }
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
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
                return internalDisplay(hrq, metadataArray, entityByCrisId, field);
            } else {
                return metadataArray.get(0).getValue();
            }
        }
        return "N/D";
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
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph)
                    throws JspException
    {

        ACrisObject crisObject = (ACrisObject) item;
        String metadata = internalDisplay(hrq, metadataArray, crisObject, field);
        return metadata;
    }
    
    private String internalDisplay(HttpServletRequest hrq,
            List<IMetadataValue> metadataArray, ACrisObject crisObject, String field)
    {
        String metadata = "";
        if (metadataArray != null && metadataArray.size() > 0)
        {
            try 
            {
                PropertiesDefinition pd = applicationService.findPropertiesDefinitionByShortName(crisObject.getClassPropertiesDefinition(), field.split("\\.")[1]);
                for(IMetadataValue mm : metadataArray) {
                    metadata += JDynATagLibraryFunctions.getCheckRadioDisplayValue((((WidgetCheckRadio)pd.getRendering()).getStaticValues()), mm.getValue());
                }
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
                //failover insert the stored value
                for(IMetadataValue mm : metadataArray) {
                    metadata += mm.getValue();
                }                
            }
        }
        return metadata;
    }
}

