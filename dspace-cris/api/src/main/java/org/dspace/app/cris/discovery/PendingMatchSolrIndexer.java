/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.content.DSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.IMetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

public class PendingMatchSolrIndexer implements SolrServiceIndexPlugin
{

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {

        if (dso instanceof Item)
        {
            Item item = (Item) dso;

            List<String> listMetadata = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService()
                    .getAuthorityMetadataForAuthority(
                            RPAuthority.RP_AUTHORITY_NAME);

            for (String metadata : listMetadata)
            {
                List<IMetadataValue> values = item.getItemService().getMetadataByMetadataString(item, metadata);
                for (IMetadataValue val : values)
                {
                    if (val != null)
                    {
                        if (val.getAuthority() != null && !(val.getAuthority().isEmpty()))
                        {
                            if (val.getConfidence() != Choices.CF_ACCEPTED)
                            {
                                document.addField("authority." + RPAuthority.RP_AUTHORITY_NAME + "." + metadata +".pending", val.getAuthority());
                            }
                        }
                    }
                }
            }

        }

    }

}

