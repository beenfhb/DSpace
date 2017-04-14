/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.List;
import java.util.UUID;

import org.dspace.app.cris.model.orcid.OrcidHistory;

import it.cilea.osd.common.dao.PaginableObjectDao;

/**
 * This interface define the methods available to retrieve OrcidQueue
 * 
 * @author l.pascarelli
 * 
 */
public interface OrcidHistoryDao extends PaginableObjectDao<OrcidHistory, Integer> {
	
	public List<OrcidHistory> findOrcidHistoryByResearcherId(UUID entityId);
	public List<OrcidHistory> findOrcidHistoryByProjectId(UUID entityId);
	public List<OrcidHistory> findOrcidHistoryByPublicationId(UUID entityId);
	public List<OrcidHistory> findOrcidHistoryByEntityIdAndTypeId(UUID entityId, Integer typeId);
	public List<OrcidHistory> findOrcidHistoryInSuccess();
	public List<OrcidHistory> findOrcidHistoryInError();
	public List<OrcidHistory> findOrcidHistoryInSuccessByOwner(String owner);
	public List<OrcidHistory> findOrcidHistoryInSuccessByOwnerAndTypeId(String owner, Integer typeId);
	public OrcidHistory uniqueOrcidHistoryInSuccessByOwnerAndEntityIdAndTypeId(String owner, UUID entityId, Integer typeId);
	public OrcidHistory uniqueOrcidHistoryByOwnerAndEntityIdAndTypeId(String owner, UUID entityId, Integer typeId);	
	
	
}
