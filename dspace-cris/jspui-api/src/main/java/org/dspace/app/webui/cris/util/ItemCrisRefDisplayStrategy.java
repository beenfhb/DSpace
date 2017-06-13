/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.IMetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.utils.DSpace;

public class ItemCrisRefDisplayStrategy extends ASimpleDisplayStrategy
{
	
    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(ItemCrisRefDisplayStrategy.class);

    private ApplicationService applicationService = new DSpace().getServiceManager()
            .getServiceByName("applicationService",
                    ApplicationService.class);

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit,
			boolean viewFull, String browseType, UUID colIdx, UUID itemId,
			String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks,
			boolean emph) throws JspException {
    	String publicPath = null;
    	int minConfidence = -1;
		if (metadataArray.size() > 0) {
			ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
			ChoiceAuthority ca = cam.getChoiceAuthority(metadataArray.get(0).getSchema(), metadataArray.get(0).getElement(), metadataArray.get(0).getQualifier());
			try {
				minConfidence = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService().getMinConfidence(UIUtil.obtainContext(hrq), metadataArray.get(0).getSchema(), metadataArray.get(0).getElement(), metadataArray.get(0).getQualifier());
			} catch (SQLException e) {
				throw new JspException(e);
			}
			if (ca != null && ca instanceof CRISAuthority) {
				CRISAuthority crisAuthority = (CRISAuthority) ca;
				publicPath = crisAuthority.getPublicPath();
				if (publicPath == null) {
					publicPath = ConfigurationManager.getProperty("ItemCrisRefDisplayStrategy.publicpath."+field);
					if (publicPath == null) {
						publicPath = metadataArray.get(0).getQualifier();
					}
				}
			}
		}
		
		if (publicPath == null) {
			return "";
		}
		
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
        }

        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < loopLimit; j++)
        {
            buildBrowseLink(hrq, viewFull, browseType, metadataArray, minConfidence,
                    disableCrossLinks, sb, j);
            if (StringUtils.isNotBlank(metadataArray.get(j).getAuthority()) && metadataArray.get(j).getConfidence() >= minConfidence) {
            	buildAuthority(hrq, metadataArray, publicPath, sb, j);
            }
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

    private void buildBrowseLink(HttpServletRequest hrq, boolean viewFull,
            String browseType, List<IMetadataValue> metadataArray, int minConfidence,
            boolean disableCrossLinks, StringBuffer sb, int j)
    {
        String startLink = "";
        String endLink = "";
        if (!StringUtils.isEmpty(browseType) && !disableCrossLinks)
        {
            String argument;
            String value;
            String authority = metadataArray.get(j).getAuthority();
			if (authority != null &&
                    metadataArray.get(j).getConfidence() >= minConfidence && !(authority.startsWith(AuthorityValueService.GENERATE)))
            {
                argument = "authority";
                value = authority;
            }
            else
            {
                argument = "value";
                value = metadataArray.get(j).getValue();
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
    }

    private void buildAuthority(HttpServletRequest hrq,
            List<IMetadataValue> metadataArray, String publicPath, StringBuffer sb, int j)
    {
        String startLink = "";
        String endLink = "";

        String authority = metadataArray.get(j).getAuthority();
		if (StringUtils.isNotBlank(authority)) {
			if (authority.startsWith(AuthorityValueService.GENERATE)) {
				String[] split = StringUtils.split(authority, AuthorityValueService.SPLIT);
				String type = null, info = null;
				if (split.length == 3) {
					type = split[1];
					info = split[2];
					String externalContextPath = ConfigurationManager.getProperty("cris","external.domainname.getAuthority().service."+type);
					startLink = "<a target=\"_blank\" href=\"" + externalContextPath + info;
					startLink += "\" class=\"authority\">&nbsp;<img style=\"width: 16px; height: 16px;\" src=\""+ hrq.getContextPath() +"/images/mini-icon-orcid.png\" alt=\"\">";
					endLink = "</a>";
					sb.append(startLink);
					sb.append(endLink);
				}
			}
			else {
		        startLink = "&nbsp;<a href=\"" + hrq.getContextPath() + "/cris/"+publicPath+ "/"
		                + authority;
		        startLink += "\" class=\"authority\">";
		        endLink = "</a>";		        
		        String icon = "";
				try {
					ACrisObject rp = applicationService.getEntityByCrisId(authority);
					String type = rp.getMetadata(ConfigurationManager.getProperty("cris", "researcher.cris."+publicPath+".ref.display.strategy.metadata.icon"));					
					String status = "";
					if(rp == null || !rp.getStatus()) {
			             startLink = "&nbsp;";
			             endLink = "";
			             status = "private.";
					}

					String title;
					try {
						title = I18nUtil
								.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + status + type + ".title", true);
					}
					catch (MissingResourceException e2)
                    {
						title = I18nUtil
								.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + type + ".title");
                    }
					
					try {
						icon = MessageFormat.format(
								I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + status + type + ".icon", true),
								title);
					}
					catch (MissingResourceException e2)
                    {
						icon = MessageFormat.format(
								I18nUtil.getMessage("ItemCrisRefDisplayStrategy." + publicPath + "." + type + ".icon"),
								title);
                    }
				} catch (Exception e) {
					log.debug("Error when build icon (perhaps missing this configuration: on cris module key:researcher.cris.rp.ref.display.strategy.metadata.icon)", e);
					try {
						icon = I18nUtil.getMessage("ItemCrisRefDisplayStrategy."+publicPath+".icon",true);
					} catch (MissingResourceException e2) {
						log.debug("Error when build icon (perhaps missing this configuration: on cris module key:researcher.cris.rp.ref.display.strategy.metadata.icon)", e2);
						icon = I18nUtil.getMessage("ItemCrisRefDisplayStrategy.default.icon");
					}
				}
				sb.append(startLink);
				sb.append(icon);
		        sb.append(endLink);
			}
		}
		

    }
}
