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
import org.dspace.core.Context;
import org.hibernate.Session;

/**
 * Implements virtual field processing to build suffix doi
 * 
 * @author pascarelli
 */
public class VirtualFieldArticleDoi implements VirtualFieldDisseminator,
        VirtualFieldIngester
{

    private static String DEFAULT_PREFIX = "article";
    
    public String[] getMetadata(Item item, Map<String, String> fieldCache,
            String fieldName)
    {

        Context context = null;
        try
        {
            context = new Context();

           
           
            // Check to see if the virtual field is already in the cache
            // - processing is quite intensive, so we generate all the values on
            // first request
            if (fieldCache.containsKey(fieldName))
            {
                return new String[] { fieldCache.get(fieldName) };
            }
            
            String result = "";
           
            List<MetadataValue> mdpartof = item.getMetadata("dc", "relation", "ispartofjournal", Item.ANY);
            if(mdpartof.size()>0) {                         
                result += mdpartof.get(0).getValue().toLowerCase();
                result = result.replaceAll("[^a-z]+", "-");
            }
            else {
                result += DEFAULT_PREFIX;
            }            
            
            result += ".";
            
            List<MetadataValue> md = item.getMetadata("dc", "relation", "volume", Item.ANY);
            if(md.size()>0){
                result += md.get(0).getValue() + ".";    
            }                      
            
            List<MetadataValue> mddaterel = item.getMetadata("dc", "relation", "issue", Item.ANY);
            if(mddaterel.size()>0){
                result += mddaterel.get(0).getValue() + ".";    
            }                       

            List<MetadataValue> mdfirst = item.getMetadata("dc", "relation", "firstpage", Item.ANY);
            if(mdfirst.size()>0){
                result += mdfirst.get(0).getValue() + "-";    
            }                      
                       
            
            List<MetadataValue> mdlast = item.getMetadata("dc", "relation", "lastpage", Item.ANY);
            if(mdlast.size()>0){
                result += mdlast.get(0).getValue() + ".";    
            }                 

            List<MetadataValue> mddate = item.getMetadata("dc", "date", "issued", Item.ANY);
            
            if(mddate.size()>0){
                result += mddate.get(0).getValue();    
            }    
            
            Object count = getHibernateSession(context).createSQLQuery("select count(*) as cc from "
                    + ScriptCrossrefSender.TABLE_NAME_DOI2ITEM
                    + " where identifier_doi = :par0").setParameter(0, result).uniqueResult();
            if(count!=null) {
                if((Integer)count>0) {
                    result += "_" + item.getID();
                }
            }
            fieldCache.put("virtual.articledoi", result);
            
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
