/**
 * Copyright 2012 Lyncode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lyncode.xoai.dataprovider.format;

import com.lyncode.xoai.dataprovider.data.ICustomFormatter;
import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.data.internal.CustomMetadataFormat;
import com.lyncode.xoai.dataprovider.data.internal.MetadataFormat;
import com.lyncode.xoai.dataprovider.data.internal.MetadataFormatSuper;
import com.lyncode.xoai.dataprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.dataprovider.exceptions.ConfigurationException;
import com.lyncode.xoai.dataprovider.filter.FilterManager;
import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.CustomFormatConfiguration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.FormatConfiguration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.FormatConfigurationSuper;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**

 * @version 3.1.0
 */
public class MetadataFormatManager {

    private static Logger log = LogManager.getLogger(MetadataFormatManager.class);

    private Map<String, MetadataFormatSuper> contexts;

    public MetadataFormatManager(ResourceResolver resolver, List<FormatConfigurationSuper> config,
                                 FilterManager filterManager) throws ConfigurationException {
        contexts = new HashMap<String, MetadataFormatSuper>();
        for (FormatConfigurationSuper format : config) {
        	if(format instanceof  FormatConfiguration) {
	            Transformer transformer = null;
	
	            try {
	                transformer = resolver.getTransformer(((FormatConfiguration)format).getXslt());
	            } catch (TransformerConfigurationException e) {
	                throw new ConfigurationException(e.getMessage(), e);
	            } catch (IOException e) {
	                throw new ConfigurationException(e.getMessage(), e);
	            }
	
	            MetadataFormat metadataFormat = new MetadataFormat(format.getPrefix(), transformer, format.getNamespace(), format.getSchemaLocation());
	            if (format.hasFilter())
	                metadataFormat.setFilter(filterManager.getFilter(format.getFilter().getReference()));
	            contexts.put(format.getId(), metadataFormat);
        	}
        	else {
        		//VSTODO: implementare CustomFormat
        		//STUB implementation
        		try {
					CustomMetadataFormat metadataFormat = new CustomMetadataFormat(format.getPrefix(), (Class<? extends ICustomFormatter>) Class.forName(((CustomFormatConfiguration)format).getClazz()), null, format.getNamespace(), format.getSchemaLocation());
					contexts.put(format.getId(), metadataFormat);
				} catch (ClassNotFoundException e) {
					// VSTODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        		
        	}
        }
    }

    public boolean formatExists(String id) {
        return this.contexts.containsKey(id);
    }

    public MetadataFormatSuper getFormat(String id) {
        return contexts.get(id);
    }

    public List<MetadataFormatSuper> getFormats(ItemIdentifier item) {
        List<MetadataFormatSuper> formats = new ArrayList<MetadataFormatSuper>();
        for (MetadataFormatSuper format : contexts.values())
            if (format.isApplicable(item))
                formats.add(format);
        return formats;
    }

    public MetadataFormatSuper getFormatByPrefix(String prefix)
            throws BadArgumentException {
        for (MetadataFormatSuper f : contexts.values())
            if (f.getPrefix().equals(prefix))
                return f;
        throw new BadArgumentException(
                "There is no metadata schema with the given metadataPrefix");
    }
}
