/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.common.dao.PaginableObjectDao;

import java.util.List;
import java.util.UUID;

import org.dspace.app.cris.model.CrisDeduplication;
import org.dspace.app.cris.model.CrisSubscription;

/**
 * This interface define the methods available to retrieve CrisDeduplication
 * 
 * @author Luigi Andrea Pascarelli
 * 
 */
public interface CrisDeduplicationDao extends PaginableObjectDao<CrisDeduplication, Integer> {
	
	public List<CrisDeduplication> findByFirstAndSecond(String firstID, String secondID);

	public CrisDeduplication uniqueByFirstAndSecond(String firstID, String secondID);
}
