/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.RootObject;
import org.dspace.core.Context;

public interface RootEntityService<T extends RootObject> {

	void updateLastModified(Context context, T dSpaceObject) throws SQLException, AuthorizeException;
	boolean isSupportsTypeConstant(int type);

}
