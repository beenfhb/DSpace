/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class SubmitterValueGenerator implements TemplateValueGenerator {

	@Override
	public List<MetadataValue> generator(Context context, Item targetItem, Item templateItem,
			MetadataValue MetadataValue, String extraParams) {
		List<MetadataValue> m = new ArrayList<>();
		m.add(MetadataValue);
		EPerson eperson = targetItem.getSubmitter();
		if (StringUtils.equalsIgnoreCase(extraParams, "email")) {
			MetadataValue.setValue(eperson.getEmail());
		} else if (StringUtils.equalsIgnoreCase(extraParams, "phone")) {
			MetadataValue.setValue(eperson.getDSpaceObjectService().getMetadata(eperson, "phone"));
		} else if (StringUtils.equalsIgnoreCase(extraParams, "fullname")) {
			MetadataValue.setValue(eperson.getFullName());
		} else {
			MetadataValue.setValue(eperson.getDSpaceObjectService().getMetadata(eperson, extraParams));
		}
		if (StringUtils.isNotBlank(m.get(0).getValue())) {
			return m;
		} else {
			return new ArrayList<>();
		}
	}

}
