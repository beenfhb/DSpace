package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.Context;

public class IndicatorMetricPercentageBuilder<ACO extends BrowsableDSpaceObject>
        extends IndicatorMetricSumBuilder<ACO>
{

    @Override
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService,
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, Double> mapAdditionalValueComputed, Map<String, List<Double>> mapElementsValueComputed, 
            ACO aco, SolrDocument doc, Integer resourceType,
            UUID resourceId, String uuid)
    {
        Double valueComputed = mapValueComputed
                .containsKey(this.getName())
                        ? mapValueComputed.get(this.getName())
                        : 0;
        Double additionalValueComputed = mapAdditionalValueComputed
                .containsKey(this.getName())
                        ? mapAdditionalValueComputed
                                .get(this.getName())
                        : 0;
                                
        if (valueComputed > 0)
        {
            Double count = (Double) doc.getFirstValue(getAdditionalField());

            if(count!=null && count>0) {
                additionalValueComputed = (valueComputed
                        / count) * 100;;
            }
            
            mapValueComputed.put(this.getName(), valueComputed);
            mapAdditionalValueComputed.put(this.getName(),
                    additionalValueComputed);
        }
    }
}
