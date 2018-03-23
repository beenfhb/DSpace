/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

public class LinkDisplayStrategy extends AUniformDisplayStrategy implements IAtomicDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger.getLogger(LinkDisplayStrategy.class);

    protected String getDisplayForValue(HttpServletRequest hrq, String value, UUID itemid)
    {
        try {
			return getDisplayForValue(UIUtil.obtainContext(hrq), hrq, null, value, null, null, -1, itemid, false, null, false, false);
		} catch (SQLException e) {
			log.warn(e.getMessage());
		}
        return "";
    }

	@Override
	public String getDisplayForValue(Context context, HttpServletRequest hrq, String field, String value,
			String authority, String language, int confidence, UUID itemid, boolean viewFull, String browseType,
			boolean disableCrossLinks, boolean emph) {
        StringBuffer sb = new StringBuffer();
        String startLink = "<a href=\"" + value + "\">";
        String endLink = "</a>";
        sb.append(startLink);
        sb.append(value);
        sb.append(endLink);
        return sb.toString();
	}

}
