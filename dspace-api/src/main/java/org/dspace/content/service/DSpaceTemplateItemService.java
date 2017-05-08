/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.IMetadataValue;
import org.dspace.content.generator.TemplateValueGenerator;
import org.dspace.core.Context;

public class DSpaceTemplateItemService implements TemplateItemService {
	private Map<String, TemplateValueGenerator> generators;
	
	public void setGenerators(Map<String, TemplateValueGenerator> generators) {
		this.generators = generators;
	}

	@Override
	public void applyTemplate(Context context, Item targetItem, Item templateItem) throws SQLException {
        List<IMetadataValue> mds = templateItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        for (IMetadataValue md : mds)
        {
        	// replace ###SPECIAL-PLACEHOLDER### with the actual value, where the SPECIAL-PLACEHOLDER can be one of
        	// NOW.YYYY-MM-DD SUBMITTER RESEARCHER CURRENTUSER.fullname / email / phone
            if (StringUtils.startsWith(md.getValue(), "###") 
            		&& StringUtils.endsWith(md.getValue(), "###")) {
            	String[] splitted = md.getValue().substring(3, md.getValue().length()-3).split("\\.", 2);
            	TemplateValueGenerator gen = generators.get(splitted[0]);
            	if (gen != null) {
	            	String extraParams = null;
            		if (splitted.length == 2) {
	            		extraParams = splitted[1];
	            	}
            		List<IMetadataValue> genMetadata = gen.generator(context, targetItem, templateItem, md, extraParams);
            		for (IMetadataValue gm : genMetadata) {
            			targetItem.getItemService().addMetadata(context, targetItem, gm.getSchema(), gm.getElement(), gm.getQualifier(), gm.getLanguage(),
                                gm.getValue(), gm.getAuthority(), gm.getConfidence());
            		}
	            	continue;
            	}
            }
            targetItem.getItemService().addMetadata(context, targetItem, md.getSchema(), md.getElement(), md.getQualifier(), md.getLanguage(),
                    md.getValue(), md.getAuthority(), md.getConfidence());
        }
	}
	
    public void clearTemplate(Context context, Item targetItem, Item templateItem) throws SQLException
    {
    	List<IMetadataValue> mds = templateItem.getMetadata("*", "*", "*", "*");
        for(IMetadataValue md : mds) {
            targetItem.clearMetadata(context, md.getSchema(), md.getElement(), md.getQualifier(), "*");
        }

    }
}
