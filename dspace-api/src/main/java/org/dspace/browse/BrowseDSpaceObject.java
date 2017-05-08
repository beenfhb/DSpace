/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Transient;

import org.dspace.content.IMetadataValue;
import org.dspace.core.Context;

public class BrowseDSpaceObject
{
    private BrowsableDSpaceObject browseObject;

    @Transient
	public transient Map<String, Object> extraInfo = new HashMap<String, Object>();
    
    public BrowseDSpaceObject(Context context, BrowsableDSpaceObject browseObject)
    {
        super();
        this.browseObject = browseObject;
        this.extraInfo = browseObject.getExtraInfo();
    }
    
    public List<IMetadataValue> getMetadata(String schema, String element,
            String qualifier, String lang)
    {
        return browseObject.getMetadata(schema, element, qualifier, lang);
    }

    public List<String> getMetadataValue(String mdString)
    {
        return browseObject.getMetadataValue(mdString);
    }
    
    public BrowsableDSpaceObject getBrowsableDSpaceObject()
    {
        return browseObject;
    }

	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
		return browseObject.getMetadataValueInDCFormat(mdString);
	}

	public String getTypeText() {
		return browseObject.getTypeText();
	}

	public int getType() {
		return browseObject.getType();
	}

	public String getName() {
		return browseObject.getName();
	}

	public Map<String, Object> getExtraInfo() {
		return extraInfo;
	}
	
	public UUID getID() {
		return browseObject.getID();
	}
	
	public String getHandle() {
		return browseObject.getHandle();
	}
	
	public boolean isWithdrawn() {
		return browseObject.isWithdrawn();
	}
}
