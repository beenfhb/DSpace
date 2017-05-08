/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.submit;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataAuthorityISSNExtractor implements ISSNItemExtractor
{
    @Autowired(required = true)
    public ItemService itemService;

    private List<String> metadataList;

    public void setMetadataList(List<String> metadataList)
    {
        this.metadataList = metadataList;
    }

    @Override
    public List<String> getISSNs(Context context, Item item)
    {
        List<String> values = new ArrayList<String>();
        for (String metadata : metadataList)
        {
            List<IMetadataValue> dcvalues = itemService.getMetadataByMetadataString(item, metadata);
            for (IMetadataValue dcvalue : dcvalues)
            {
                String authority = dcvalue.getAuthority();
                if(authority !=null){
                    values.add(authority);
                }
            }
        }
        return values;
    }
}
