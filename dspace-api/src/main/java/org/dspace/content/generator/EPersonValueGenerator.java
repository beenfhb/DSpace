/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.IMetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

public class EPersonValueGenerator implements TemplateValueGenerator {

	private static Logger log = Logger.getLogger(EPersonValueGenerator.class);

	@Override
	public List<IMetadataValue> generator(Context context, Item targetItem, Item templateItem,
			IMetadataValue IMetadataValue, String extraParams) {
		String[] params = StringUtils.split(extraParams, "\\.");
		String prefix = params[0];
		String suffix = "";
		if (params.length > 1) {
			suffix = params[1];
		}
		String value = prefix;
		if (StringUtils.startsWith(prefix, "submitter")) {
			String metadata = prefix.substring("submitter[".length(), prefix.length() - 1);

			value = EPersonServiceFactory.getInstance().getEPersonService().getMetadata(targetItem.getSubmitter(),
					metadata);

		} else if (StringUtils.startsWith(prefix, "item")) {
			value = targetItem.getMetadata(prefix.replace("_", "."));
		}

		if (StringUtils.isNotBlank(suffix)) {
			value = value + "-" + suffix;
		}

		List<IMetadataValue> m = new ArrayList<>();
		m.add(IMetadataValue);
		EPerson ePerson = null;
		try {
			ePerson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, value);
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		String result = "";
		if (ePerson != null) {
			result = "" + ePerson.getID();
		}
		IMetadataValue.setValue(result);
		return m;
	}

}
