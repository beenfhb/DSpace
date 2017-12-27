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

import org.dspace.app.cris.model.StatSubscription;

import it.cilea.osd.common.dao.PaginableObjectDao;

/**
 * This interface define the methods available to retrieve StatSubscription
 * 
 * @author cilea
 * 
 */
public interface StatSubscriptionDao extends PaginableObjectDao<StatSubscription, Integer> {
    public List<StatSubscription> findByFreq(int freq);
    public List<StatSubscription> findByFreqAndType(int freq, int type);
	public List<StatSubscription> findByType(Integer type);
	public List<StatSubscription> findByUID(String uid);
	public List<StatSubscription> findByEPersonID(UUID epersonID);    
    public List<StatSubscription> findByEPersonIDandUID(UUID id, String uid);
    public List<StatSubscription> findByEPersonIDandType(UUID id, Integer type);
    public void deleteByEPersonID(UUID id);    
}
