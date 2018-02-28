/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactoryImpl;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the base class for any Rest Repository. It provides utility method to
 * access the DSpaceContext
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class AbstractDSpaceRestRepository {
	
	private static final Logger log = Logger.getLogger(AbstractDSpaceRestRepository.class);
	
	@Autowired
	protected Utils utils;

	protected RequestService requestService = new DSpace().getRequestService();

	protected Context obtainContext() {
		Request currentRequest = requestService.getCurrentRequest();
		Context context = ContextUtil.obtainContext(currentRequest.getServletRequest());
        return context;
	}

	public RequestService getRequestService() {
		return requestService;
	}
}
