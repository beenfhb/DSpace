/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery.recentSubmissions;

import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryRecentSubmissionsConfiguration;

/**
 * Class containing utility methods used to render recent submissions
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class RecentSubmissionUtils {

    private static final Logger log = Logger.getLogger(RecentSubmissionUtils.class);

    /**
     * Retrieves the recent submitted items of the given scope
     *
     * @param context session context.
     * @param dso the DSpace object can either be null (indicating home page), a collection or a community
     * @param offset start here in the list.
     * @return result.
     */
    public static DiscoverResult getRecentlySubmittedItems(Context context, DSpaceObject dso, int offset) {
        try {
            DiscoverQuery queryArgs = new DiscoverQuery();

            //Add the default filter queries
            DiscoveryConfiguration discoveryConfiguration = getDiscoveryConfiguration(dso);
            List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
            queryArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));
            queryArgs.setDSpaceObjectFilter(Constants.ITEM);

            DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = getRecentSubmissionConfiguration(discoveryConfiguration);
            if(recentSubmissionConfiguration != null){
                queryArgs.setMaxResults(recentSubmissionConfiguration.getMax());
                queryArgs.setStart(offset);
                String sortField = SearchUtils.getSearchService().toSortFieldIndex(recentSubmissionConfiguration.getMetadataSortField(), recentSubmissionConfiguration.getType());
                if(sortField != null){
                    queryArgs.setSortField(
                            sortField,
                            DiscoverQuery.SORT_ORDER.desc
                    );
                }
                SearchService service = SearchUtils.getSearchService();
                return service.search(context, (BrowsableDSpaceObject)dso, queryArgs);
            }else{
                //No configuration, no results
                return null;
            }
        }catch (SearchServiceException se){
            log.error("Caught SearchServiceException while retrieving recent submission for: " + (dso == null ? "home page" : dso.getHandle()), se);
            return null;
        }
    }

    public static DiscoveryRecentSubmissionsConfiguration getRecentSubmissionConfiguration(DSpaceObject dso) {
        return getRecentSubmissionConfiguration(getDiscoveryConfiguration(dso));

    }
    public static DiscoveryRecentSubmissionsConfiguration getRecentSubmissionConfiguration(DiscoveryConfiguration discoveryConfiguration) {
        return discoveryConfiguration.getRecentSubmissionConfiguration();
    }

    public static DiscoveryConfiguration getDiscoveryConfiguration(DSpaceObject dso) {
        return SearchUtils.getDiscoveryConfiguration((BrowsableDSpaceObject)dso);
    }

}
