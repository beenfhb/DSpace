/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.dspace.content.IMetadataValue;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;

public interface BrowsableDSpaceObject extends IGlobalSearchResult
{
	public Map<String, Object> getExtraInfo();

    public boolean isArchived();

    public List<IMetadataValue> getMetadata(String schema, String element,
            String qualifier, String lang);

    public String getMetadata(String field);
    
	public boolean isDiscoverable();
	
	public String getName();

	public String findHandle(Context context) throws SQLException;

	public boolean haveHierarchy();

	public BrowsableDSpaceObject getParentObject();
	
	public String getMetadataFirstValue(String schema, String element, String qualifier, String language);
	
	public Date getLastModified();
}
