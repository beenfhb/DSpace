package org.dspace.content;

import java.util.UUID;

public interface UsageEventEntity {

	UUID getID();

	String getTypeText();
	
	int getType();

	String getHandle();

	String getName();

	boolean haveHierarchy();

}
