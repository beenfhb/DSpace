package org.dspace.app.cris.service;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.core.Context;

public class ResearcherPageServiceImpl extends CrisObjectServiceImpl<ResearcherPage> {

	@Override
	public ResearcherPage find(Context context, UUID id) throws SQLException {
		return getApplicationService().getEntityByUUID(id.toString(), ResearcherPage.class);
	}

	@Override
	public boolean isSupportsTypeConstant(int type) {
		if(CrisConstants.RP_TYPE_ID == type) {
			return true;
		}
		return false;
	}
	
}
