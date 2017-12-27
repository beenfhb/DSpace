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
import org.dspace.content.IMetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class SubmitterValueGenerator implements TemplateValueGenerator {

	@Override
	public List<IMetadataValue> generator(Context context, Item targetItem, Item templateItem,
			IMetadataValue IMetadataValue, String extraParams) {
		List<IMetadataValue> m = new ArrayList<>();
		m.add(IMetadataValue);
		EPerson eperson = targetItem.getSubmitter();
		if (StringUtils.equalsIgnoreCase(extraParams, "email")) {
			IMetadataValue.setValue(eperson.getEmail());
		} else if (StringUtils.equalsIgnoreCase(extraParams, "phone")) {
			IMetadataValue.setValue(eperson.getDSpaceObjectService().getMetadata(eperson, "phone"));
		} else if (StringUtils.equalsIgnoreCase(extraParams, "fullname")) {
			IMetadataValue.setValue(eperson.getFullName());
		} else {
			IMetadataValue.setValue(eperson.getDSpaceObjectService().getMetadata(eperson, extraParams));
		}
		if (StringUtils.isNotBlank(m.get(0).getValue())) {
			return m;
		} else {
			return new ArrayList<>();
		}
	}

}
