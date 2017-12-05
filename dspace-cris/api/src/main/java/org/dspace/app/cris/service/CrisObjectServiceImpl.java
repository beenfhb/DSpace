package org.dspace.app.cris.service;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.util.Researcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.RootEntityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class CrisObjectServiceImpl<T extends ACrisObject> implements RootEntityService<T> {
	
	private ApplicationService applicationService;
	
	@Override
	public void updateLastModified(Context context, T dSpaceObject) throws SQLException, AuthorizeException {
		// nothing		
	}

	@Override
	public abstract boolean isSupportsTypeConstant(int type);

	public ApplicationService getApplicationService() {
		if(applicationService==null) {
			Researcher researcher = new Researcher();
			applicationService = researcher.getApplicationService();
		}
		return applicationService;
	}

	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

}
