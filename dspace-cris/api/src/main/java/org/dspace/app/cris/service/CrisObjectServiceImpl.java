package org.dspace.app.cris.service;

import java.sql.SQLException;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.RootEntityService;
import org.dspace.core.Context;

public class CrisObjectServiceImpl<T extends ACrisObject> implements RootEntityService<T> {

	@Override
	public void updateLastModified(Context context, T dSpaceObject) throws SQLException, AuthorizeException {
		// nothing		
	}

	@Override
	public boolean isSupportsTypeConstant(int type) {
		if(CrisConstants.CRIS_DYNAMIC_TYPE_ID_START >= type) {
			return true;
		}
		return false;
	}

}
