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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.IMetadataValue;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;

/**
 * @author Luigi Andrea Pascarelli
 *
 */
public class CrisRPAdvancedDisplayStrategy extends ItemCrisRefDisplayStrategy {

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, UUID itemId, String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks,
			boolean emph) throws JspException {

		String RPOtherMetadata = ConfigurationManager.getProperty("webui.item.displaystrategy.crisrp.tooltipmetadata");
		List<IMetadataValue> otherMetadata = null;
		try {
			otherMetadata = ContentServiceFactory.getInstance().getItemService().find(UIUtil.obtainContext(hrq), itemId)
					.getMetadataValueInDCFormat(RPOtherMetadata);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String publicPath = null;
		int minConfidence = -1;
		if (metadataArray.size() > 0) {
			ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
			ChoiceAuthority ca = cam.getChoiceAuthority(metadataArray.get(0).getSchema(),
					metadataArray.get(0).getElement(), metadataArray.get(0).getQualifier());
			try {
				minConfidence = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService().getMinConfidence(
						UIUtil.obtainContext(hrq), metadataArray.get(0).getSchema(), metadataArray.get(0).getElement(),
						metadataArray.get(0).getQualifier());
			} catch (SQLException e) {
				throw new JspException(e);
			}
			if (ca != null && ca instanceof CRISAuthority) {
				CRISAuthority crisAuthority = (CRISAuthority) ca;
				publicPath = crisAuthority.getPublicPath();
				if (publicPath == null) {
					publicPath = ConfigurationManager.getProperty("ItemCrisRefDisplayStrategy.publicpath." + field);
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
		if (limit != -1) {
			loopLimit = (limit > metadataArray.size() ? metadataArray.size() : limit);
			truncated = (limit < metadataArray.size());
		}

		StringBuffer sb = new StringBuffer();
		for (int j = 0; j < loopLimit; j++) {
			buildBrowseLink(hrq, viewFull, browseType, metadataArray, minConfidence, otherMetadata, disableCrossLinks, sb, j);
			if (StringUtils.isNotBlank(metadataArray.get(j).getAuthority())
					&& metadataArray.get(j).getConfidence() >= minConfidence) {
				buildAuthority(hrq, metadataArray.get(j).getValue(), metadataArray.get(j).getAuthority(), itemId, publicPath, sb);
			}
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

    private void buildBrowseLink(HttpServletRequest hrq, boolean viewFull,
            String browseType, List<IMetadataValue> metadataArray, int minConfidence,
            List<IMetadataValue> otherMetadata, boolean disableCrossLinks, StringBuffer sb, int j)
    {

		String startLink = "";
		String endLink = "";
		if (!StringUtils.isEmpty(browseType) && !disableCrossLinks) {
			String argument;
			String value;
			String authority = metadataArray.get(j).getAuthority();
			if (authority != null &&
                    metadataArray.get(j).getConfidence() >= minConfidence && !(authority.startsWith(AuthorityValueService.GENERATE)))
            {
				argument = "authority";
				value = authority;
			} else {
				argument = "value";
				value = metadataArray.get(j).getValue();
			}
			if (viewFull) {
				argument = "vfocus";
			}
			try {
				startLink = "<a href=\"" + hrq.getContextPath() + "/browse?type=" + browseType + "&amp;" + argument
						+ "=" + URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

			if (metadataArray.get(j).getLanguage() != null) {
				try {
					startLink = startLink + "&amp;" + argument + "_lang="
							+ URLEncoder.encode(metadataArray.get(j).getLanguage(), "UTF-8") + "\"";
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			} else {
				startLink += "\"";
			}

			if (otherMetadata != null && otherMetadata.size() > j) {
				String val = StringUtils.equals(otherMetadata.get(j).getValue(), MetadataValue.PARENT_PLACEHOLDER_VALUE) ? "N/D"
						: otherMetadata.get(j).getValue();
				startLink += " data-toggle=\"tooltip\" data-placement=\"right\" title=\"" + val + "\" ";
			}

			if ("authority".equals(argument)) {
				startLink += " class=\"authority " + browseType + "\">";
			} else {
				startLink = startLink + ">";
			}
			endLink = "</a>";
		}
		sb.append(startLink);
		sb.append(Utils.addEntities(metadataArray.get(j).getValue()));
		if (otherMetadata != null && otherMetadata.size() > 1) {
			sb.append("<strong>*</strong>");
		}
		sb.append(endLink);
	}

}
