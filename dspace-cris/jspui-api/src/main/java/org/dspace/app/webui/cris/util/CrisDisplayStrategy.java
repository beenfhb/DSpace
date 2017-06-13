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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;

public class CrisDisplayStrategy implements IDisplayMetadataValueStrategy {

    @Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, String field, List<IMetadataValue> metadataArray, BrowsableDSpaceObject item, boolean disableCrossLinks,
			boolean emph) {
    	ACrisObject crisObject = (ACrisObject)item;
		String metadata = "-";
		if (metadataArray.size() > 0) {
			metadata = "<a href=\"" + hrq.getContextPath() + "/cris/" + crisObject.getPublicPath() + "/"
					+ ResearcherPageUtils.getPersistentIdentifier(crisObject) + "\">"
					+ Utils.addEntities(metadataArray.get(0).getValue()) + "</a>";
		}
		metadata = (emph ? "<strong>" : "") + metadata + (emph ? "</strong>" : "");
		return metadata;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, String field, List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks, boolean emph) {
		String metadata;
		// limit the number of records if this is the author field (if
		// -1, then the limit is the full list)
		boolean truncated = false;
		int loopLimit = metadataArray.size();
		if (limit != -1) {
			loopLimit = (limit > metadataArray.size() ? metadataArray.size() : limit);
			truncated = (limit < metadataArray.size());
		}

		StringBuffer sb = new StringBuffer();
		for (int j = 0; j < loopLimit; j++) {
			if (metadataArray.get(j).getConfidence() != Choices.CF_ACCEPTED) {
				continue;
			}
			buildBrowseLink(hrq, viewFull, browseType, metadataArray, disableCrossLinks, sb, j);
			buildAuthority(hrq, metadataArray, sb, j);
			if (j < (loopLimit - 1)) {
				if (colIdx != null) // we are showing metadata in a table row
									// (browse or item list)
				{
					sb.append("; ");
				} else {
					// we are in the item tag
					sb.append("<br />");
				}
			}
		}
		if (truncated) {
            Locale locale = UIUtil.getSessionLocale(hrq); 
            String etal = I18nUtil.getMessage("itemlist.et-al", locale);

			sb.append(", " + etal);
		}

		if (colIdx != null) // we are showing metadata in a table row (browse or
							// item list)
		{
			metadata = (emph ? "<strong><em>" : "<em>") + sb.toString() + (emph ? "</em></strong>" : "</em>");
		} else {
			// we are in the item tag
			metadata = (emph ? "<strong>" : "") + sb.toString() + (emph ? "</strong>" : "");
		}

		return metadata;
	}

	private void buildBrowseLink(HttpServletRequest hrq, boolean viewFull, String browseType, List<IMetadataValue> metadataArray,
			boolean disableCrossLinks, StringBuffer sb, int j) {
		String startLink = "";
		String endLink = "";
		if (StringUtils.isEmpty(browseType)) {
			browseType = "author";
		}
		String argument;
		String value;
		argument = "authority";
		String authority = metadataArray.get(j).getAuthority();
		value = metadataArray.get(j).getValue();
		if (viewFull) {
			argument = "vfocus";
		}
		try {
			if(authority.startsWith(AuthorityValueService.GENERATE)) {
				startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/browse?type=" + browseType + "&amp;";
			}
			else {
				startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/browse?type=" + browseType + "&amp;"
					+ argument + "=" + URLEncoder.encode(authority, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (metadataArray.get(j).getLanguage() != null) {
			try {
				startLink = startLink + "&amp;" + argument + "_lang="
						+ URLEncoder.encode(metadataArray.get(j).getLanguage(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		if ("authority".equals(argument)) {
			startLink += "\" class=\"authority " + browseType + "\">";
		} else {
			startLink = startLink + "\">";
		}
		endLink = "</a>";
		sb.append(startLink);
		sb.append(Utils.addEntities(value));
		sb.append(endLink);

	}

	private void buildAuthority(HttpServletRequest hrq,
			List<IMetadataValue> metadataArray, StringBuffer sb, int j)
    {
        String startLink = "";
        String endLink = "";
		String authority = metadataArray.get(j).getAuthority();
		if (StringUtils.isNotBlank(authority)) {
			if (authority.startsWith(AuthorityValueService.GENERATE)) {
				String[] split = StringUtils.split(authority, AuthorityValueService.SPLIT);
				String type = null, info = null;
				if (split.length > 0) {
					type = split[1];
					if (split.length > 1) {
						info = split[2];
					}
				}
				String externalContextPath = ConfigurationManager.getProperty("cris","external.domainname.authority.service."+type);
				startLink = "<a target=\"_blank\" href=\"" + externalContextPath + info;
				startLink += "\" class=\"authority\">";
				startLink += "\" class=\"authority\">&nbsp;<img style=\"width: 16px; height: 16px;\" src=\"" + hrq.getContextPath() + "/images/mini-icon-orcid.png\" alt=\"\">";
				endLink = "</a>";
				sb.append(startLink);				
				sb.append(endLink);
			}
			else {
				startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/cris/rp/" + authority;
				startLink += "\" class=\"authority\">";
				endLink = "</a>";
				sb.append(startLink);
				sb.append(" <i class=\"fa fa-user\"></i>");
				sb.append(endLink);
			}
		}
		
    }


	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem, boolean disableCrossLinks, boolean emph) throws JspException {
		return null;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, String field, List<IMetadataValue> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException {
		ACrisObject crisObject = (ACrisObject) item;
		String metadata = "-";
		if (metadataArray.size() > 0) {
			metadata = "<a href=\"" + hrq.getContextPath() + "/cris/" + crisObject.getPublicPath() + "/"
					+ ResearcherPageUtils.getPersistentIdentifier(crisObject) + "\">"
					+ Utils.addEntities(metadataArray.get(0).getValue()) + "</a>";
		}
		metadata = (emph ? "<strong>" : "") + metadata + (emph ? "</strong>" : "");
		return metadata;
	}

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

}
