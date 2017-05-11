/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.webui.cris.components.statistics.CrisStatBitstreamTopObjectComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatTopObjectComponent;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;

public class BrowseItemConfigurerComponent extends
        AFacetedQueryConfigurerComponent<BrowsableDSpaceObject>
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(BrowseItemConfigurerComponent.class);

    @Override
    public List<BrowsableDSpaceObject> getObjectFromSolrResult(DiscoverResult docs,
            Context context) throws Exception
    {
    	return docs.getDspaceObjects();
    }
    
    @Override
    public IStatsComponent getStatsDownloadComponent()
    {
        CrisStatBitstreamTopObjectComponent component = new CrisStatBitstreamTopObjectComponent();

        BeanComponent bean = new BeanComponent();
        bean.setQuery(getRelationConfiguration().getQuery());
        for(String key : getTypes().keySet()) {
            bean.getSubQueries().put(key, getTypes().get(key).getFacetQuery());
        }
        
        component.setFromField("search.uniqueid");
        component.setBean(bean);
        component.setTargetObjectClass(getTarget());
        component.setRelationObjectClass(getRelationObjectClass());
        
        try
        {
            component.setRelationObjectType(CrisConstants.getEntityType(getRelationObjectClass()));
        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
        }
        component.setCrisSearchService((CrisSearchService) getSearchService());
        return component;

    }

    @Override
    public IStatsComponent getStatsViewComponent()
    {
        CrisStatTopObjectComponent component = new CrisStatTopObjectComponent();

        BeanComponent bean = new BeanComponent();
        bean.setQuery(getRelationConfiguration().getQuery());
        for(String key : getTypes().keySet()) {
            bean.getSubQueries().put(key, getTypes().get(key).getFacetQuery());
        }
        
        component.setBean(bean);
        component.setTargetObjectClass(getTarget());
        component.setRelationObjectClass(getRelationObjectClass());
        try
        {
            component.setRelationObjectType(CrisConstants.getEntityType(getRelationObjectClass()));
        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
        }
        return component;
    }

    @Override
    public Class<? extends BrowsableDSpaceObject> getRelationObjectClass()
    {
        return getRelationConfiguration().getRelationClass();
    }

    @Override
    public Integer getRelationObjectType()
    {
        try
        {
            return CrisConstants.getEntityType(getRelationObjectClass());
        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
