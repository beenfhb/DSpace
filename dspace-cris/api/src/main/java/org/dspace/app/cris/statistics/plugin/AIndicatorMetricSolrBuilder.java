package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.List;

import org.dspace.browse.BrowsableDSpaceObject;

public abstract class AIndicatorMetricSolrBuilder<ACO extends BrowsableDSpaceObject>
        extends AIndicatorBuilder<ACO>
{

    @Override
    public List<String> getFields()
    {
        List<String> results = new ArrayList<String>();
        for (String input : getInputs())
        {
            results.add("crismetrics_" + input);
        }
        return results;
    }
    
    @Override
    public String getAdditionalField()
    {
        if(super.getAdditionalField()!=null) {
            return "crismetrics_" + super.getAdditionalField();
        }
        return super.getAdditionalField();
    }
    
}
