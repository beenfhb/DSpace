/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.IMetadataValue;
import org.dspace.core.I18nUtil;

public class AbstractMetadataDisplayStrategy extends ASimpleDisplayStrategy {
	
	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, UUID colIdx,
			UUID itemid, String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks, boolean emph)
			throws JspException {
		String metadataDisplay = "-";
		boolean found = false;

		for (IMetadataValue descrMetadata : metadataArray) {
			if (StringUtils.startsWith(descrMetadata.getQualifier(), "abstract")) {
				found = true;
				break;
			}
		}

		// Assente di default
		try {
			metadataDisplay = MessageFormat.format(
					I18nUtil.getMessage("jsp.hasabstract.display-strategy.none", UIUtil.obtainContext(hrq)),
					hrq.getContextPath(), "");
		} catch (SQLException e) {
			// converto a runtime
			throw new RuntimeException(e.getMessage(), e);
		}

		if (found) {

			try {
				metadataDisplay = MessageFormat.format(
						I18nUtil.getMessage("jsp.hasabstract.display-strategy.default", UIUtil.obtainContext(hrq)),
						hrq.getContextPath());

			} catch (SQLException e) {
				// converto a runtime
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return metadataDisplay;
	}

	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String string, UUID colIdx,
			String field, List<IMetadataValue> metadataArray, boolean disableCrossLinks, boolean emph,
			PageContext pageContext) {
		return "nowrap=\"nowrap\" align=\"center\"";
	}


}
