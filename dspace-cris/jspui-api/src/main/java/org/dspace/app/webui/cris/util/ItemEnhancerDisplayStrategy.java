/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.I18nUtil;

public class ItemEnhancerDisplayStrategy extends ASimpleDisplayStrategy
{

    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(ItemEnhancerDisplayStrategy.class);

    private String commonDisplay(HttpServletRequest hrq, UUID itemId,
            String field)
    {
        Item item;
        String metadata = "";
        try
        {
            item =  ContentServiceFactory.getInstance().getItemService().find(UIUtil.obtainContext(hrq), itemId);
            String result = item.getMetadata(field);
            
            if(result != null) {
                try {      
                    metadata += I18nUtil.getMessage(
                                "ItemEnhancerDisplayStrategy." + result, true);
                }
                catch(Exception ex) {
                    metadata += result;
                }
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }

        return metadata;
    }

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, UUID itemid, String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks,
			boolean emph) throws JspException {
		   return commonDisplay(hrq, itemid, field);
	}

}
