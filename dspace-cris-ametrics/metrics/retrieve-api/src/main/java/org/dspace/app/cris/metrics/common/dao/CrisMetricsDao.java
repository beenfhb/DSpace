/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.common.dao;

import java.util.List;
import java.util.UUID;

import org.dspace.app.cris.metrics.common.model.CrisMetrics;

import it.cilea.osd.common.dao.PaginableObjectDao;

/**
 * This interface define the methods available to retrieve CrisMetrics
 *
 * @author l.pascarelli
 */
public interface CrisMetricsDao extends PaginableObjectDao<CrisMetrics, Integer> {
    
    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(UUID resourceID, Integer resourceTypeId, String metricsType);

    public List<CrisMetrics> findLastMetricByResourceIdAndResourceTypeIdAndMetricsTypes(
            Integer resourceID, Integer resourceTypeId,
            List<String> metricsTypes);
}
