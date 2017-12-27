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
import org.dspace.content.IMetadataValue;
import org.dspace.core.Context;

public class IdentifierValueGenerator implements TemplateValueGenerator
{

    private static Logger log = Logger.getLogger(IdentifierValueGenerator.class);

    @Override
    public List<IMetadataValue> generator(Context context, Item targetItem, Item templateItem,
            IMetadataValue IMetadataValue, String extraParams)
    {
        List<IMetadataValue> m = new ArrayList<>();
        m.add(IMetadataValue);
        String value = ""+targetItem.getID();
        IMetadataValue.setValue(value);
        return m;
    }
   
}
