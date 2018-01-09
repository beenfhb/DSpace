/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics.components;

import org.dspace.app.cris.integration.components.BeanFacetComponent;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;

public class CrisDOStatsDualSelectedComponent extends CrisStatsDualComponent<ResearchObject>
{
  
    @Override
    public IStatsComponent getStatsDownloadComponent()
    {
        StatCrisDownloadSelectedObjectComponent comp = new StatCrisDODownloadSelectedObjectComponent();        
        comp.setApplicationService(getApplicationService());
        comp.setRelationObjectClass(getRelationObjectClass());
        comp.setRelationObjectType(getRelationObjectType());
        
        
        BeanFacetComponent bean = new BeanFacetComponent();
        bean.setFacetQuery(FILE+":*");
        bean.setFacetField(FILE);
        comp.setBean(bean);

        return comp;
    }

    @Override
    public IStatsComponent getStatsViewComponent()
    {
        StatCrisViewSelectedObjectComponent comp = new StatCrisViewSelectedObjectComponent();
        comp.setRelationObjectClass(getRelationObjectClass());
        comp.setRelationObjectType(getRelationObjectType());
        
        return comp;
    }

    @Override
    public Integer getRelationObjectType()
    {

        return CrisConstants.CRIS_DYNAMIC_TYPE_ID_START;
    }

    @Override
    public Class<ResearchObject> getRelationObjectClass()
    {
        return ResearchObject.class;
    }
 
   
}
