package org.dspace.app.cris.service;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;
import org.dspace.core.Context;

public class ProjectServiceImpl extends CrisObjectServiceImpl<Project> {

	@Override
	public Project find(Context context, UUID id) throws SQLException {
		return getApplicationService().getEntityByUUID(id.toString(), Project.class);
	}

	@Override
	public boolean isSupportsTypeConstant(int type) {
		if(CrisConstants.PROJECT_TYPE_ID == type) {
			return true;
		}
		return false;
	}
	
}
