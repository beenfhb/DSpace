package org.dspace.app.cris.service;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.cris.model.ResearchObject;
import org.dspace.core.Context;

public class ResearchObjectServiceImpl extends CrisObjectServiceImpl<ResearchObject> {

	@Override
	public ResearchObject find(Context context, UUID id) throws SQLException {
		return getApplicationService().getEntityByUUID(id.toString(), ResearchObject.class);
	}

}
