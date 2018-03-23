/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.IAtomicDisplayStrategy;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.IMetadataValue;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.utils.DSpace;

public class MultiformDisplayStrategy extends ASimpleDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(MultiformDisplayStrategy.class);

    private MultiformRegexConfigurator multiformRegexConfigurator;

	@Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, UUID itemid,
            String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks,
            boolean emph)
    {
        // limit the number of records if this is the author field (if
        // -1, then the limit is the full list)
        boolean truncated = false;
        int loopLimit = metadataArray.size();
        if (limit != -1)
        {
            loopLimit = (limit > metadataArray.size() ? metadataArray.size()
                    : limit);
            truncated = (limit < metadataArray.size());
            log.debug("Limiting output of field " + field + " to "
                    + Integer.toString(loopLimit) + " from an original "
                    + Integer.toString(metadataArray.size()));
        }
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < loopLimit; j++)
        {
            String displayvalue = "";
            String authority = metadataArray.get(j).getAuthority();
            String value = metadataArray.get(j).getValue();
            String language = metadataArray.get(j).getLanguage();
            int confidence = metadataArray.get(j).getConfidence();
            // discover decorator by regex
            String decorator = getMultiformRegexConfigurator().checkRegex(value,
                    authority);

            IDisplayMetadataValueStrategy strategy = (IDisplayMetadataValueStrategy) CoreServiceFactory.getInstance().getPluginService()
                    .getNamedPlugin(IDisplayMetadataValueStrategy.class,
                            decorator);
            if (strategy instanceof IAtomicDisplayStrategy)
            {
                IAtomicDisplayStrategy ss = (IAtomicDisplayStrategy) strategy;
                ss.setPluginInstanceName(decorator);
                
                try {
					displayvalue = ss
					        .getDisplayForValue(UIUtil.obtainContext(hrq), hrq, field, value, authority, language, confidence, itemid, viewFull, browseType, disableCrossLinks, emph);
				} catch (SQLException e) {
					log.warn(e.getMessage());
				}
            }

            sb.append(displayvalue);

            if (j < (loopLimit - 1))
            {
                if (colIdx != null) // we are showing metadata in a table row
                                  // (browse or item list)
                {
                    sb.append("; ");
                }
                else
                {
                    // we are in the item tag
                    sb.append("<br />");
                }
            }
        }
        if (truncated)
        {
            if (colIdx != null)
            {
                sb.append("; ...");
            }
            else
            {
                sb.append("<br />...");
            }
        }

        return sb.toString();
    }

    public MultiformRegexConfigurator getMultiformRegexConfigurator()
    {
        if (multiformRegexConfigurator == null)
        {
            multiformRegexConfigurator = new DSpace()
                    .getSingletonService(MultiformRegexConfigurator.class);
        }
        return multiformRegexConfigurator;
    }

}
