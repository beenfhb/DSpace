/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.integration.batch.ScriptCrossrefSender;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.hibernate.Session;

/**
 * Implements virtual field processing to build doi suffix
 * 
 * @author pascarelli
 */
public class VirtualFieldCrossrefPhdDOI implements VirtualFieldDisseminator,
        VirtualFieldIngester
{

    private static String PREFIX = "phd";
    
    public String[] getMetadata(Item item, Map<String, String> fieldCache,
            String fieldName)
    {

        Context context = null;
        try
        {
            context = new Context();
            //phd<year>_autore tutto minuscolo con replace di [^a-z] in -
            
            // Check to see if the virtual field is already in the cache
            // - processing is quite intensive, so we generate all the values on
            // first request
            if (fieldCache.containsKey(fieldName))
            {
                return new String[] { fieldCache.get(fieldName) };
            }
            
            String result = ConfigurationManager.getProperty("doi.prefix");
			List<MetadataValue> authors = item.getMetadata("dc", "contributor", "author", Item.ANY);
			if (authors.size() > 0) {
				MetadataValue md = authors.get(0);
				String mdValue = md.getValue().toLowerCase();
				mdValue = mdValue.replaceAll("[^a-z]+", "-");
				result += mdValue;
			}
           
            result += "_"+PREFIX;
            
            MetadataValue mddate = item.getItemService().getMetadata(item, "dc", "date", "issued", Item.ANY).get(0);
            result += mddate.getValue();
            
            Object count = getHibernateSession(context).createSQLQuery("select count(*) as cc from "
                    + ScriptCrossrefSender.TABLE_NAME_DOI2ITEM
                    + " where identifier_doi = :par0").setParameter(0, result).uniqueResult();
            if(count!=null) {
                if((Integer)count>0) {
                    result += "_" + item.getID();
                }
            }
            fieldCache.put("virtual.pgthesisdoi", result);
            // Return the value of the virtual field (if any)
            if (fieldCache.containsKey(fieldName))
            {
                return new String[] { fieldCache.get(fieldName) };
            }

        }
        catch (SQLException e)
        {
            // nothing
        }
        finally
        {
            if (context!=null && context.isValid())
            {
                context.abort();
            }
        }
        return null;
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache,
            String fieldName, String value)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache)
    {
        return false;
    }
    
	public Session getHibernateSession(Context context) throws SQLException {
		return ((Session) context.getDBConnection().getSession());
	}
}
