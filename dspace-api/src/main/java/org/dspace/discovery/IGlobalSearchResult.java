/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.UUID;

import org.dspace.content.IMetadataValue;

public interface IGlobalSearchResult {

	public String getHandle();
	
	public List<String> getMetadataValue(String mdString);
	
	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString);
	
	public String getTypeText();
	
	public int getType();
	
	public UUID getID();
	
	public boolean isWithdrawn();

}
