/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics.components;

import java.sql.SQLException;

import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.core.Context;

import it.cilea.osd.jdyna.model.PropertiesDefinition;

public abstract class CrisStatDownloadTopObjectComponent extends
        CrisStatBitstreamTopObjectComponent
{

    private ApplicationService applicationService;

    @Override
    public TwoKeyMap getLabels(Context context, String type)
            throws SQLException
    {

        TwoKeyMap labels = new TwoKeyMap();

        PieStatisticBean myvalue = (PieStatisticBean) statisticDatasBeans
                .get("top").get(type).get("sectionid");
        if (myvalue != null)
        {
            if (myvalue.getLimitedDataTable() != null)
            {
                for (StatisticDatasBeanRow row : myvalue.getLimitedDataTable())
                {
                    String pkey = (String) row.getLabel();

                    PropertiesDefinition def = innerCall(Integer.parseInt(pkey));

                    labels.addValue(row.getLabel(), "label", def.getLabel());

                    if (def != null)
                    {
                        labels.addValue(type, row.getLabel(), def);
                    }
                }
            }
        }
        return labels;
    }

    protected abstract PropertiesDefinition innerCall(Integer pkey);

 
    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

}
