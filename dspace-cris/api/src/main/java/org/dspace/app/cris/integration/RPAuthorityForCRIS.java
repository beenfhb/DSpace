/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;

public class RPAuthorityForCRIS extends CRISAuthorityForCRIS<ResearcherPage> 
{
	 @Override
	    public int getCRISTargetTypeID()
	    {
	        return CrisConstants.RP_TYPE_ID;
	    }

	    @Override
	    public Class<ResearcherPage> getCRISTargetClass()
	    {
	        return ResearcherPage.class;
	    }

	    
	    @Override
	    public String getPublicPath() {
	    	return "rp";
	    }

		@Override
		public ResearcherPage getNewCrisObject() {
			return new ResearcherPage();
		}

}
