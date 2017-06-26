/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.IMetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;

public class DefaultDisplayStrategy extends ASimpleDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DefaultDisplayStrategy.class);
    
    private String displayStrategyName;
    
    public DefaultDisplayStrategy()
    {
    
    }
    
    public DefaultDisplayStrategy(String displayStrategyName)
    {
        this.displayStrategyName = displayStrategyName;
    }
    
    
    private final transient MetadataAuthorityService metadataAuthorityService
            = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
    
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, UUID itemid, String field,
            List<IMetadataValue> metadataArray, boolean disableCrossLinks, boolean emph) throws JspException
    {
        boolean isNoBreakLine = "nobreakline".equals(getDisplayStrategyName());
        String metadata;
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
            String startLink = "";
            String endLink = "";
            if (!StringUtils.isEmpty(browseType) && !disableCrossLinks)
            {
                String argument;
                String value;
                try {
					if (metadataArray.get(j).getAuthority() != null &&
					        metadataArray.get(j).getConfidence() >= metadataAuthorityService
					            .getMinConfidence(UIUtil.obtainContext(hrq), metadataArray.get(j).getSchema(), metadataArray.get(j).getElement(), metadataArray.get(j).getQualifier()))
					{
					    argument = "authority";
					    value = metadataArray.get(j).getAuthority();
					}
					else
					{
					    argument = "value";
					    value = metadataArray.get(j).getValue();
					}
				} catch (SQLException e1) {
					throw new JspException(e1);
				}
                if (viewFull)
                {
                    argument = "vfocus";
                }
                try
                {
                    startLink = "<a href=\"" + hrq.getContextPath()
                            + "/browse?type=" + browseType + "&amp;" + argument
                            + "=" + URLEncoder.encode(value, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException(e.getMessage(), e);
                }

                if (metadataArray.get(j).getLanguage() != null)
                {
                    try
                    {
                        startLink = startLink + "&amp;" + argument + "_lang="
                                + URLEncoder.encode(metadataArray.get(j).getLanguage(), "UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }

                if ("authority".equals(argument))
                {
                    startLink += "\" class=\"authority " + browseType + "\">";
                }
                else
                {
                    startLink = startLink + "\">";
                }
                endLink = "</a>";
            }
            sb.append(startLink);
            sb.append(Utils.addEntities(metadataArray.get(j).getValue()));
            sb.append(endLink);
            if (j < (loopLimit - 1))
            {
                if (colIdx != null || isNoBreakLine) // we are showing metadata in a table row (browse or item list)
                {
                    if (isNoBreakLine)
                    {
                        String separator = ConfigurationManager
                                .getProperty("webui.itemdisplay.nobreakline."+ field +".separator");
                        if (separator == null)
                        {
                            separator = ConfigurationManager
                                    .getProperty("webui.itemdisplay.nobreakline.separator");
                            if(separator == null) {
                                separator = ";&nbsp;";
                            }
                        }
                        sb.append(separator);
                    }
                    else {
                        sb.append(";&nbsp;");
                    }
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
        	Locale locale = UIUtil.getSessionLocale(hrq); 
            String etal = I18nUtil.getMessage("itemlist.et-al", locale);
            sb.append(", " + etal);
        }
        
        if (colIdx != null) // we are showing metadata in a table row (browse or item list)
        {
            metadata = (emph ? "<strong><em>" : "<em>") + sb.toString()
            + (emph ? "</em></strong>" : "</em>");
        }
        else
        {
            // we are in the item tag
            metadata = (emph ? "<strong>" : "") + sb.toString()
            + (emph ? "</strong>" : "");
        }
        
        return metadata;
    }

    public String getDisplayStrategyName()
    {
        return displayStrategyName;
    }

    public void setDisplayStrategyName(String displayStrategyName)
    {
        this.displayStrategyName = displayStrategyName;
    }
}