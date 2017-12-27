/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.common.dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.core.Context;
import org.hibernate.Query;

import it.cilea.osd.common.core.SingleTimeStampInfo;

public class MetricsApplicationDao
        extends it.cilea.osd.common.dao.impl.ApplicationDao
{

    private final String query = "select cm1.uuid as uuid, subq.resourceid as resourceid, subq.resourcetypeid as resourcetypeid, subq.limitsx as startdate, subq.limitdx as enddate, case when (cm2.metriccount - cm1.metriccount) < 0 then 0 else (cm2.metriccount - cm1.metriccount) end as metriccount"
            + " from"
            + " (select cm1.resourceid, cm1.resourcetypeid, max(cm1.enddate) as limitsx, min(cm2.enddate) as limitdx, cm1.metrictype"
            + " from cris_metrics cm1 join cris_metrics cm2"
            + " on cm1.resourceid = cm2.resourceid and cm1.metrictype = cm2.metrictype and cm1.resourcetypeid = cm2.resourcetypeid"
            + " where cm1.metricType = :par0 " + " and cm1.enddate < :par1"
            + " and cm2.enddate > :par2"
            + " group by cm1.resourceid, cm1.resourcetypeid, cm1.metrictype) subq"
            + " join cris_metrics cm1"
            + " on cm1.resourceid = subq.resourceid and subq.metrictype = cm1.metrictype and cm1.resourcetypeid = subq.resourcetypeid and subq.limitsx = cm1.enddate"
            + " join cris_metrics cm2"
            + " on cm2.resourceid = subq.resourceid and subq.metrictype = cm2.metrictype and cm2.resourcetypeid = subq.resourcetypeid and subq.limitdx = cm2.enddate";

    private final String queryUpdateLast = "update cris_metrics set last = false where metrictype = :par0 and last = true and resourcetypeid = :par1 and resourceid = :par2 and timestampcreated < :par3";
    
	public void buildPeriodMetrics(Context context, String suffixNewType, String type, long rangeLimitSx,
			long rangeLimitDx) throws SQLException {

		List<Object[]> tri = getSessionFactory().getCurrentSession().createSQLQuery(query).setParameter(0, type)
				.setParameter(1, new Timestamp(rangeLimitSx)).setParameter(2, new Timestamp(rangeLimitDx)).list();
		for (Object[] rowSelect : tri) {

			String metrictype = type + suffixNewType;
			Date currentTimestamp = new Date();
			UUID resourceId = UUID.fromString((String) rowSelect[1]);// .getIntColumn("resourceid");
			int resourceTypeId = (Integer) rowSelect[2];// .getIntColumn("resourcetypeid");
			// just set false the column last
			getSessionFactory().getCurrentSession().createSQLQuery(queryUpdateLast).setParameter(0, metrictype)
					.setParameter(1, resourceTypeId).setParameter(3, resourceId)
					.setParameter(4, new Timestamp(currentTimestamp.getTime())).executeUpdate();

			CrisMetrics metric = new CrisMetrics();
			metric.getTimeStampInfo().setInfoCreated(new SingleTimeStampInfo(currentTimestamp));
			metric.getTimeStampInfo().setInfoLastModified(new SingleTimeStampInfo(currentTimestamp));
			metric.setStartDate((Date) rowSelect[4]);
			metric.setEndDate((Date) rowSelect[3]);
			metric.setMetricCount((double) rowSelect[5]);
			metric.setLast(true);
			getSessionFactory().getCurrentSession().persist(metric);

		}
	}

    /**
     * 
     * @param resourceTypeId
     * @param resourceId
     * @param metricType
     * @return the number of updated metrics
     */
	public int unsetLastMetric(Integer resourceTypeId, UUID resourceId, String metricType) {
        Query query = getSessionFactory().getCurrentSession().createQuery(
                "update org.dspace.app.cris.metrics.common.model.CrisMetrics set last = false where resourceTypeId = ? and resourceId = ? and metricType = ?");
        
        query.setParameter(0, resourceTypeId);
        query.setParameter(1, resourceId);
        query.setParameter(2, metricType);

        return query.executeUpdate();
	}

}
