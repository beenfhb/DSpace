/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.List;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public interface ImportAuthorityFiller {
	void fillRecord(Context context, Item item, List<MetadataValue> metadata, String authorityID, ACrisObject crisObject);

	boolean allowsUpdate(Context ctx, Item item, List<MetadataValue> MetadataValueList, String authorityKey, ACrisObject rp);
	
}
