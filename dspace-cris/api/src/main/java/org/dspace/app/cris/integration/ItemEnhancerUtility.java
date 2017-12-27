/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.ItemEnhancer;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValueVolatile;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.integration.defaultvalues.DefaultValuesBean;
import org.dspace.content.integration.defaultvalues.EnhancedValuesGenerator;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class ItemEnhancerUtility
{
    private static final Logger log = Logger
            .getLogger(ItemEnhancerUtility.class);

    public static List<IMetadataValue> getMetadata(Item item, String metadata)
    {
        StringTokenizer dcf = new StringTokenizer(metadata, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while (dcf.hasMoreTokens())
        {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        if (!"item".equals(schema) && !Item.ANY.equals(schema))
        {
            return null;
        }
        if (element == null)
        {
            log.error("Wrong configuration asked for schema item metadata with null element");
        }

        List<ItemEnhancer> enhancers = getEnhancers(element);
        List<IMetadataValue> result = new ArrayList<>();

        for (ItemEnhancer enh : enhancers)
        {
            List<DefaultValuesBean> vals = getMetadata(item, enh, qualifier);
            for (DefaultValuesBean e : vals)
            {
				if (e.getValues() != null) {
					for (int idx = 0; idx < e.getValues().length; idx++) {
						MetadataValueVolatile dc = new MetadataValueVolatile();
						dc.schema = "item";
						dc.element = enh.getAlias();
						dc.qualifier = Item.ANY.equalsIgnoreCase(qualifier) || StringUtils.isBlank(qualifier) ? null
								: qualifier;
						dc.setValue(e.getValues()[idx]);
						if (StringUtils.isNotBlank(dc.getValue())) {
							if (e.getAuthorities() != null && e.getAuthorities().length > 0) {
								dc.setAuthority(e.getAuthorities()[idx]);
								dc.setConfidence(StringUtils.isNotEmpty(e.getAuthorities()[idx]) ? Choices.CF_ACCEPTED
										: Choices.CF_UNSET);
							} else {
								dc.setAuthority(null);
								dc.setConfidence(Choices.CF_UNSET);
							}							
							result.add(dc);
						}
					}
				}
            }

        }
        return result;
    }

	private static List<DefaultValuesBean> getMetadata(Item item, ItemEnhancer enh, String qualifier) {
		List<String> mdList = enh.getMetadata();
		List<DefaultValuesBean> result = new ArrayList<DefaultValuesBean>();

		for (String md : mdList) {
			item.turnOffItemWrapper();
			List<IMetadataValue> MetadataValues = ContentServiceFactory.getInstance().getItemService()
					.getMetadataByMetadataString(item, md);
			item.restoreItemWrapperState();
			if ("placeholder.placeholder.placeholder".equalsIgnoreCase(md)) {
				DefaultValuesBean valueGenerated = null;
				String schema = "placeholder";
				String element = "placeholder";
				String qual = "placeholder";
				String value = null;
				for (EnhancedValuesGenerator vg : enh.getGenerators()) {
					valueGenerated = vg.generateValues(item, schema, element, qual, value);
					if (valueGenerated.getValues() != null && valueGenerated.getValues().length > 0) {
						result.add(valueGenerated);
					}
				}
			} else {
				for (IMetadataValue dc : MetadataValues) {
					DefaultValuesBean valueGenerated = null;
					String schema = dc.getSchema();
					String element = dc.getElement();
					String qual = dc.getQualifier();
					String value = dc.getValue();
					for (EnhancedValuesGenerator vg : enh.getGenerators()) {
						valueGenerated = vg.generateValues(item, schema, element, qual, value);
						if (valueGenerated.getValues() != null && valueGenerated.getValues().length > 0) {
							result.add(valueGenerated);
						}
					}
				}
			}
		}

		return result;
	}

    private static List<ItemEnhancer> getEnhancers(String alias)
    {
        DSpace dspace = new DSpace();
        List<ItemEnhancer> enhancers = dspace.getServiceManager()
                .getServicesByType(ItemEnhancer.class);
        if (Item.ANY.equals(alias))
        {
            return enhancers;
        }
        List<ItemEnhancer> result = new ArrayList<ItemEnhancer>();
        for (ItemEnhancer enhancer : enhancers)
        {
            if (enhancer.getAlias().equals(alias))
            {
                result.add(enhancer);
            }
        }
        return result;
    }

}
