package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.IAtomicDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.IMetadataValue;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

public class SimpleGenericDisplayStrategy extends ASimpleDisplayStrategy
        implements IAtomicDisplayStrategy
{
    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(SimpleGenericDisplayStrategy.class);

    @Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, UUID itemid, String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks,
			boolean emph) throws JspException {
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
            if (metadataArray != null && metadataArray.size() > 0)
            {
                try {
					sb.append(getDisplayForValue(UIUtil.obtainContext(hrq), hrq, field, metadataArray.get(j).getValue(),
					        metadataArray.get(j).getAuthority(), metadataArray.get(j).getLanguage(),
					        metadataArray.get(j).getConfidence(), itemid, viewFull,
					        browseType, disableCrossLinks, emph));
				} catch (SQLException e) {
					log.warn(e.getMessage());
				}
                if (j < (loopLimit - 1))
                {
                    if (colIdx != null)
                    {
                        sb.append(";&nbsp;");
                    }
                    else
                    {
                        // we are in the item tag
                        sb.append("<br />");
                    }
                }
            }
            else
            {
                break;
            }
        }
        if (truncated)
        {
            Locale locale = UIUtil.getSessionLocale(hrq);
            String etal = I18nUtil.getMessage("itemlist.et-al", locale);
            sb.append(", " + etal);
        }

        if (colIdx != null) // we are showing metadata in a table row (browse or
                          // item list)
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

	@Override
	public String getDisplayForValue(Context context, HttpServletRequest hrq, String field, String value,
			String authority, String language, int confidence, UUID itemid, boolean viewFull, String browseType,
			boolean disableCrossLinks, boolean emph) {
        try
        {
            String tmpValue = value;
            if (StringUtils.isNotBlank(authority))
            {
                tmpValue = authority;
            }
            String metadataDisplay = MessageFormat.format(
                    I18nUtil.getMessage("jsp.display-strategy." + field + "." + this.getPluginInstanceName(),
                            UIUtil.obtainContext(hrq)),
                    hrq.getContextPath(), tmpValue, value, authority, field);
            return metadataDisplay;
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return null;
		
	}

}
