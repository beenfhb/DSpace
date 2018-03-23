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
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.IMetadataValue;
import org.dspace.core.I18nUtil;

public class DateDisplayStrategy extends ASimpleDisplayStrategy
{
	/** log4j category */
    private static Logger log = Logger.getLogger(DateDisplayStrategy.class);
    
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, UUID itemid, String field,
            List<IMetadataValue> metadataArray, boolean disableCrossLinks, boolean emph)
    {
        String metadata = "-";
        if (metadataArray.size() > 0)
        {
            String value = metadataArray.get(0).getValue();
            if(!StringUtils.isBlank(value)) {
                if(value.equals("9999")) {
                    metadata = I18nUtil.getMessage("jsp.mydspace.render.inpress", hrq.getLocale());
                }
                else {
                    DCDate dd = new DCDate(value);
                    try {
                    	metadata = UIUtil.displayDate(dd, false, false, hrq);
                    }
                    catch (RuntimeException rte) {
						log.error("Malformed value for the DateDiplayStrategy " + rte.getMessage() + " -" + value);
                    	metadata = value;
                    }
                }
            }         	
        }
        metadata = (emph ? "<strong>" : "") + metadata
                + (emph ? "</strong>" : "");
        return metadata;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String string, UUID colIdx, UUID itemid, String field,
            List<IMetadataValue> metadataArray, boolean disableCrossLinks, boolean emph,
            PageContext pageContext)
    {
        return "nowrap=\"nowrap\" align=\"right\"";
    }

}
