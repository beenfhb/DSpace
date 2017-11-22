/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.util.SimpleMapConverter;
import org.dspace.utils.DSpace;

/**
 * Costruisce i singoli autori a partire dalla stringa allauthor
 * 
 * @author bollini
 */
public class VirtualFieldRefererType implements VirtualFieldDisseminator, VirtualFieldIngester
{
	
	private SimpleMapConverter converter;
	
    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName)
    {
        String[] virtualFieldName = fieldName.split("\\.");
        
        // virtualFieldName[0] == "virtual"
		String qualifier = virtualFieldName[2];

		String type = ConfigurationManager
				.getProperty("crosswalk.virtualname.referer.type." + qualifier
						+ "." + fieldCache.get("formAlias"));
		if (StringUtils.isNotBlank(type)) {
			return new String[] { type };
		}
        
		String metadata = item.getMetadata("dc.type");

		if (StringUtils.isNotBlank(type)) {
			type = getConverter(qualifier).getValue(metadata, true);
			if (StringUtils.isNotBlank(type)) {
				return new String[] { type };
			}
		}
		
		return new String[] { ConfigurationManager
				.getProperty("crosswalk.virtualname.referer.type." + qualifier) };
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache)
    {
        return false;
    }

	public SimpleMapConverter getConverter(String qualifier) {
		if(converter==null) {
			
			converter = new DSpace()
	                .getServiceManager().getServiceByName(
	                        "mapConverter"+qualifier, SimpleMapConverter.class);
		}
		return converter;
	}

}
