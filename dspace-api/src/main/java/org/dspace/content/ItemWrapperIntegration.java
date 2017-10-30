/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;

public interface ItemWrapperIntegration
{
	public List<IMetadataValue> getMetadata(Item item, String schema, String element, String qualifier, String lang);

	public String getMetadata(Item item, String field);

	public String getTypeText(Item item);

}
