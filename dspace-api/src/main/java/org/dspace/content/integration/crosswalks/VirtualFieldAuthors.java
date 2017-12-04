/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.IMetadataValue;

/**
 * Build author citation format for APA, VANCOUVER, CHICAGO
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class VirtualFieldAuthors implements VirtualFieldDisseminator, VirtualFieldIngester {
	private static Logger log = Logger.getLogger(VirtualFieldAuthors.class);

	public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {

		String metadata = "dc.contributor.author";

		// Get the citation from the item
		List<IMetadataValue> dcvs = item.getMetadataValueInDCFormat(metadata);

		switch (fieldName) {
		case "virtual.authors.apa":
			return processAPAStyle(dcvs);
		case "virtual.authors.vancouver":
			return processVancouverStyle(dcvs);
		case "virtual.authors.chicago":
			return processChicagoStyle(dcvs);
		default:
			if (dcvs != null && dcvs.size() > 0) {
				StringBuffer sb = new StringBuffer();
				for (IMetadataValue a : dcvs) {
					String[] split = a.getValue().split(", ");
					int splitLength = split.length;
					String str = (splitLength > 1) ? split[1] : "";
					String str2 = split[0];
					if (StringUtils.isNotBlank(str2)) {
						sb.append(str).append(" ");
					}
					sb.append(str2).append(" and ");
				}
				return new String[] { sb.substring(0, sb.length() - 5) };
			}
		}
		return null;
	}

	private String[] processChicagoStyle(List<IMetadataValue> dcvs) {
		String[] results = new String[dcvs.size()];
		int index = 0;
		for(IMetadataValue imv : dcvs) {
    		int separator = StringUtils.indexOf(imv.getValue(), ",");
    		String lastname = StringUtils.substring(imv.getValue(), 0, separator);
    		String firstname = StringUtils.substring(imv.getValue(), separator+1);
    		
    		String result = lastname;
    		if(StringUtils.isNotBlank(firstname)) {
    			if(index > 0) {
    				result += " " + firstname.trim();	
    			}
    			else {
    				result += ", " + firstname.trim();
    			}
    		}
    		if(index < dcvs.size()-1) {
    			result += ",";
    		}
    		
    		if(index == dcvs.size()-2) {
    			result += " and ";	
    		}

    		results[index] = result;
    		index++;
		}
		return results;
	}

	private String[] processVancouverStyle(List<IMetadataValue> dcvs) {
		String[] results = new String[dcvs.size()];
		int index = 0;
		for(IMetadataValue imv : dcvs) {
    		int separator = StringUtils.indexOf(imv.getValue(), ",");
    		String lastname = StringUtils.substring(imv.getValue(), 0, separator);
    		String firstname = StringUtils.substring(imv.getValue(), separator+1);
    		
    		String result = lastname;
    		if(StringUtils.isNotBlank(firstname)) {
    			result += ", " + firstname.trim().substring(0, 1) + ".";
    		}
    		if(index < dcvs.size()-2) {
    			result += ",";
    		}
    		
    		if(index == dcvs.size()-2) {
    			result += " and ";	
    		}
    		
    		results[index] = result;
    		index++;
		}
		return results;
	}

	private String[] processAPAStyle(List<IMetadataValue> dcvs) {
		String[] results = new String[dcvs.size()];
		int index = 0;
		for(IMetadataValue imv : dcvs) {
    		int separator = StringUtils.indexOf(imv.getValue(), ",");
    		String lastname = StringUtils.substring(imv.getValue(), 0, separator);
    		String firstname = StringUtils.substring(imv.getValue(), separator+1);
    		
    		String result = lastname;
    		if(StringUtils.isNotBlank(firstname)) {
    			String initialFirstname = firstname.trim().substring(0, 1)+".";
    			String totalFirstname = firstname.trim();
    			if(!initialFirstname.equals(totalFirstname)) {
    				result += ", " + initialFirstname +" [" + totalFirstname + "]";
    			}
    			else {
    				result += ", " + initialFirstname;
    			}
    		}
    		if(index < dcvs.size()-1) {
    			result += ",";
    		}
    		results[index] = result;
    		index++;
		}
		return results;
	}

	public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
		// NOOP - we won't add any metadata yet, we'll pick it up when we
		// finalise the item
		return true;
	}

	public boolean finalizeItem(Item item, Map<String, String> fieldCache) {
		return false;
	}
}
