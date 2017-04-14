/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class IdentifierValueGenerator implements TemplateValueGenerator
{

    private static Logger log = Logger.getLogger(IdentifierValueGenerator.class);

    @Override
    public List<MetadataValue> generator(Context context, Item targetItem, Item templateItem,
            MetadataValue MetadataValue, String extraParams)
    {
        List<MetadataValue> m = new ArrayList<>();
        m.add(MetadataValue);
        String value = ""+targetItem.getID();
        MetadataValue.setValue(value);
        return m;
    }
   
}
