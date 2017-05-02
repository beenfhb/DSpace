/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.content.Item;
import org.dspace.content.ItemWrapperIntegration;
import org.dspace.content.MetadataValue;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.util.ReflectionUtils;

public final class CrisItemWrapper implements MethodInterceptor, ItemWrapperIntegration
{

    private static final Logger log = Logger.getLogger(CrisItemWrapper.class);
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
    	if (invocation.getMethod().getName().equals("getTypeText")) {
    		return getTypeText(invocation);
    	}

        if (invocation.getMethod().getName().equals("getMetadata"))
        {
            String schema = ""; 
            String element = "";
            String qualifier = "";
            String lang = "";
            if (invocation.getArguments().length == 4)
            {
            	schema = (String) invocation.getArguments()[0];
                element = (String) invocation.getArguments()[1];
                qualifier = (String) invocation.getArguments()[2];
                lang = (String) invocation.getArguments()[3];
            }
            else if (invocation.getArguments().length == 1)
            {
            	StringTokenizer dcf = new StringTokenizer((String) invocation.getArguments()[0], ".");
                
                String[] tokens = { "", "", "" };
                int i = 0;
                while(dcf.hasMoreTokens())
                {
                    tokens[i] = dcf.nextToken().trim();
                    i++;
                }
                schema = tokens[0];
                element = tokens[1];
                qualifier = tokens[2];
                
                if ("*".equals(qualifier))
                {
                	qualifier = Item.ANY;
                }
                else if ("".equals(qualifier))
                {
                	qualifier = null;
                }
            	
                lang = Item.ANY;
            }
            if("item".equals(schema)) {
            	List<MetadataValue> basic = (List<MetadataValue>) invocation.proceed();
                List<MetadataValue> MetadataValues = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema,
                        element, qualifier, lang);
               return MetadataValues;
            }
            else if ("crisitem".equals(schema))
            {
                List<MetadataValue> basic = (List<MetadataValue>) invocation.proceed();
                List<MetadataValue> MetadataValues = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema,
                        element, qualifier, lang);
                return MetadataValues;
            }
            else if (schema == Item.ANY)
            {
            	List<MetadataValue> basic = (List<MetadataValue>) invocation.proceed();
            	List<MetadataValue> MetadataValuesItem = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema,
                        element, qualifier, lang);
                List<MetadataValue> MetadataValuesCris = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), MetadataValuesItem, schema,
                        element, qualifier, lang);
                return MetadataValuesCris;
            }
        }
        return invocation.proceed();
    }

    private String getTypeText(MethodInvocation invocation) {
    	String metadata = ConfigurationManager.getProperty("globalsearch.item.typing");
		if (StringUtils.isNotBlank(metadata)) {
			Item item = (Item) invocation.getThis();
			List<MetadataValue> MetadataValues = item.getItemService().getMetadataByMetadataString(item, metadata);
			if (MetadataValues != null && MetadataValues.size() > 0) {
				for (MetadataValue dcval : MetadataValues) {
					String value = dcval.getValue();					
					if (StringUtils.isNotBlank(value)) {
						 String valueWithoutWhitespace = StringUtils.deleteWhitespace(value);
				    	 String isDefinedAsSystemEntity = ConfigurationManager.getProperty("facet.type."
										+ valueWithoutWhitespace.toLowerCase());
				    	 if(StringUtils.isNotBlank(isDefinedAsSystemEntity)) {
				    		 return value.toLowerCase();
				    	 }
					}
				}
			}
		}
        return Constants.typeText[Constants.ITEM].toLowerCase();
	}

	private List<MetadataValue> addCrisEnhancedMetadata(Item item, List<MetadataValue> basic,
            String schema, String element, String qualifier, String lang)
    {
        List<MetadataValue> extraMetadata = new ArrayList<MetadataValue>();
        if (schema == Item.ANY)
        {
            List<String> crisMetadata = CrisItemEnhancerUtility
                    .getAllCrisMetadata();
            if (crisMetadata != null)
            {
                for (String cM : crisMetadata)
                {
                	extraMetadata.addAll(CrisItemEnhancerUtility.getCrisMetadata(item, cM));

                }
            }
        }
        else if ("crisitem".equals(schema))
        {
        	extraMetadata.addAll(CrisItemEnhancerUtility
					.getCrisMetadata(item, schema + "." + element + "." + qualifier));

        }
        if (extraMetadata.size() == 0)
        {
            return basic;
        }
        else
        {
            List<MetadataValue> resultList = new ArrayList<MetadataValue>();
            resultList.addAll(basic);
            resultList.addAll(extraMetadata);
            return resultList;
        }
    }
    
    private List<MetadataValue> addEnhancedMetadata(Item item, List<MetadataValue> basic,
            String schema, String element, String qualifier, String lang)
    {
        List<MetadataValue> extraMetadata = new ArrayList<MetadataValue>();
        

		extraMetadata = ItemEnhancerUtility.getMetadata(item, schema + "." + element
				+ (qualifier != null ? "." + qualifier : ""));
        
        if (extraMetadata == null || extraMetadata.size() == 0)
        {
            return basic;
        }
        else
        {
            List<MetadataValue> resultList = new ArrayList<MetadataValue>();
            resultList.addAll(basic);
            resultList.addAll(extraMetadata);
            return resultList;
        }
    }

    @Override
    public Item getWrapper(Item item)
    {        
        AspectJProxyFactory pf = new AspectJProxyFactory(item);
        pf.setProxyTargetClass(true);
        pf.addAdvice(new CrisItemWrapper());
        Item proxy = (Item)(pf.getProxy());
        proxy.extraInfo = item.getExtraInfo();
        Field declaredField = null;
        try {

            declaredField = ReflectionUtils.findField(Item.class, "modifiedMetadata");
            boolean accessible = declaredField.isAccessible();
            declaredField.setAccessible(true);
            declaredField.set(proxy, item.isMetadataModified());
            declaredField.setAccessible(accessible);

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return proxy;
    }
}