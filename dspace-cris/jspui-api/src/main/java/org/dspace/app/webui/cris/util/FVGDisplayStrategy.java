/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.harvest.HarvestedItem;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.ADiscoveryDisplayStrategy;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.content.Item;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class FVGDisplayStrategy extends ADiscoveryDisplayStrategy implements IDisplayMetadataValueStrategy {
	private static Logger log = Logger.getLogger(FVGDisplayStrategy.class);   

	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, BrowseItem item, boolean disableCrossLinks, boolean emph,
			PageContext pageContext) {
		String dispalyedValue = "";
		if (metadataArray.length > 0) {
			dispalyedValue = metadataArray[0].value;
			String message = null;
			try {
				Context ctx = UIUtil.obtainContext(hrq);
				message = I18nUtil.getMessageIfExists("jsp.display.fvgdisplaystrategy." + dispalyedValue.toLowerCase(), null,
		                ctx.getCurrentLocale());
			} catch (Exception e){
				log.error(e.getMessage(), e);
			}
			if (("9999 - 9999".equals(dispalyedValue)) || ("9999".equals(dispalyedValue))) {
				return "In print";
			} 
			if(StringUtils.isNotBlank(message)){		
				return message;
			}
		}
		return dispalyedValue;
	}

	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, Item item, boolean disableCrossLinks, boolean emph,
			PageContext pageContext) {
		String dispalyedValue = "";
		if (metadataArray.length > 0) {
			dispalyedValue = metadataArray[0].value;
			String message = null;
			try {
				Context ctx = UIUtil.obtainContext(hrq);
				message = I18nUtil.getMessageIfExists("jsp.display.fvgdisplaystrategy." + dispalyedValue.toLowerCase(), null,
		                ctx.getCurrentLocale());
			} catch (Exception e){
				log.error(e.getMessage(), e);
			}
			if (("9999 - 9999".equals(dispalyedValue)) || ("9999".equals(dispalyedValue))) {
				return "In print";
			} 
			if(StringUtils.isNotBlank(message)){		
				return message;
			}
		}
		return dispalyedValue;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, int colIdx,
			String field, List<String> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks, boolean emph,
			PageContext pageContext) throws JspException {
		String dispalyedValue = "";
		if (metadataArray.size() > 0) {
			dispalyedValue = metadataArray.get(0);
			String message = null;
			try {
				Context ctx = UIUtil.obtainContext(hrq);
				message = I18nUtil.getMessageIfExists("jsp.display.fvgdisplaystrategy." + dispalyedValue.toLowerCase(), null,
		                ctx.getCurrentLocale());
			} catch (Exception e){
				log.error(e.getMessage(), e);
			}
			if (("9999 - 9999".equals(dispalyedValue)) || ("9999".equals(dispalyedValue))) {
				return "In print";
			} 
			if(StringUtils.isNotBlank(message)){		
				return message;
			}
		}
		return dispalyedValue;
	}

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        String dispalyedValue = "";
        if (metadataArray.length > 0) {
            dispalyedValue = metadataArray[0].value;
            String message = null;
            try {
                Context ctx = UIUtil.obtainContext(hrq);
                message = I18nUtil.getMessageIfExists("jsp.display.fvgdisplaystrategy." + dispalyedValue.toLowerCase(), null,
                        ctx.getCurrentLocale());
            } catch (Exception e){
                log.error(e.getMessage(), e);
            }
            if (("9999 - 9999".equals(dispalyedValue)) || ("9999".equals(dispalyedValue))) {
                return "In print";
            } 
            if(StringUtils.isNotBlank(message)){        
                return message;
            }
        }
        return dispalyedValue;
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        String dispalyedValue = "";
        if (metadataArray.length > 0) {
            dispalyedValue = metadataArray[0].value;
            String message = null;
            try {
                Context ctx = UIUtil.obtainContext(hrq);
                message = I18nUtil.getMessageIfExists("jsp.display.fvgdisplaystrategy." + dispalyedValue.toLowerCase(), null,
                        ctx.getCurrentLocale());
            } catch (Exception e){
                log.error(e.getMessage(), e);
            }
            if (("9999 - 9999".equals(dispalyedValue)) || ("9999".equals(dispalyedValue))) {
                return "In print";
            } 
            if(StringUtils.isNotBlank(message)){        
                return message;
            }
        }
        return dispalyedValue;
    }

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        String dispalyedValue = "";
        if (metadataArray.length > 0) {
            dispalyedValue = metadataArray[0].value;
            String message = null;
            try {
                Context ctx = UIUtil.obtainContext(hrq);
                message = I18nUtil.getMessageIfExists("jsp.display.fvgdisplaystrategy." + dispalyedValue.toLowerCase(), null,
                        ctx.getCurrentLocale());
            } catch (Exception e){
                log.error(e.getMessage(), e);
            }
            if (("9999 - 9999".equals(dispalyedValue)) || ("9999".equals(dispalyedValue))) {
                return "In print";
            } 
            if(StringUtils.isNotBlank(message)){        
                return message;
            }
        }
        return dispalyedValue;
    }
}
