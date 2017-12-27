/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;
import java.util.UUID;

public interface UsageEventEntity {

	UUID getID();

	String getTypeText();
	
	int getType();

	String getHandle();

	String getName();

	boolean haveHierarchy();

	List<String> getMetadataValue(String mdString);

}
