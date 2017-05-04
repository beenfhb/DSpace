package org.dspace.authorize;

import java.util.UUID;

public interface AuthorizableEntity {

	int getType();
	boolean haveHierarchy();
	UUID getID();
	Integer getLegacyId();
}
