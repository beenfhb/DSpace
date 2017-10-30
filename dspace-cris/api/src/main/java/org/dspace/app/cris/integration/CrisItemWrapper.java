/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.ItemWrapperIntegration;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

public class CrisItemWrapper implements ItemWrapperIntegration {

	private static final Logger log = Logger.getLogger(CrisItemWrapper.class);

	public String getTypeText(Item item) {
		String metadata = ConfigurationManager.getProperty("globalsearch.item.typing");
		if (StringUtils.isNotBlank(metadata)) {
			List<IMetadataValue> MetadataValues = item.getItemService().getMetadataByMetadataString(item, metadata);
			if (MetadataValues != null && MetadataValues.size() > 0) {
				for (IMetadataValue dcval : MetadataValues) {
					String value = dcval.getValue();
					if (StringUtils.isNotBlank(value)) {
						String valueWithoutWhitespace = StringUtils.deleteWhitespace(value);
						String isDefinedAsSystemEntity = ConfigurationManager
								.getProperty("facet.type." + valueWithoutWhitespace.toLowerCase());
						if (StringUtils.isNotBlank(isDefinedAsSystemEntity)) {
							return value.toLowerCase();
						}
					}
				}
			}
		}
		return Constants.typeText[Constants.ITEM].toLowerCase();
	}

	private List<IMetadataValue> addCrisEnhancedMetadata(Item item, List<IMetadataValue> basic, String schema,
			String element, String qualifier, String lang) {
		List<IMetadataValue> extraMetadata = new ArrayList<>();
		if (schema == Item.ANY) {
			List<String> crisMetadata = CrisItemEnhancerUtility.getAllCrisMetadata();
			if (crisMetadata != null) {
				for (String cM : crisMetadata) {
					extraMetadata.addAll(CrisItemEnhancerUtility.getCrisMetadata(item, cM));

				}
			}
		} else if ("crisitem".equals(schema)) {
			extraMetadata
					.addAll(CrisItemEnhancerUtility.getCrisMetadata(item, schema + "." + element + "." + qualifier));

		}
		if (extraMetadata.size() == 0) {
			return basic;
		} else {
			List<IMetadataValue> resultList = new ArrayList<>();
			resultList.addAll(basic);
			resultList.addAll(extraMetadata);
			return resultList;
		}
	}

	private List<IMetadataValue> addEnhancedMetadata(Item item, List<IMetadataValue> basic, String schema,
			String element, String qualifier, String lang) {
		List<IMetadataValue> extraMetadata = new ArrayList<>();

		extraMetadata = ItemEnhancerUtility.getMetadata(item,
				schema + "." + element + (qualifier != null ? "." + qualifier : ""));

		if (extraMetadata == null || extraMetadata.size() == 0) {
			return basic;
		} else {
			List<IMetadataValue> resultList = new ArrayList<>();
			resultList.addAll(basic);
			resultList.addAll(extraMetadata);
			return resultList;
		}
	}

	@Override
	public List<IMetadataValue> getMetadata(Item item, String schema, String element, String qualifier, String lang) {
		item.turnOffItemWrapper();
		List<IMetadataValue> basic = (List<IMetadataValue>) item.getMetadata(schema, element, qualifier, lang);
		item.restoreItemWrapperState();
		if ("item".equals(schema)) {
			List<IMetadataValue> MetadataValues = addEnhancedMetadata(item, basic, schema, element, qualifier, lang);
			return MetadataValues;
		} else if ("crisitem".equals(schema)) {
			List<IMetadataValue> MetadataValues = addCrisEnhancedMetadata(item, basic, schema, element, qualifier,
					lang);
			return MetadataValues;
		} else if (schema == Item.ANY) {
			List<IMetadataValue> MetadataValuesItem = addEnhancedMetadata(item, basic, schema, element, qualifier,
					lang);
			List<IMetadataValue> MetadataValuesCris = addCrisEnhancedMetadata(item, MetadataValuesItem, schema, element,
					qualifier, lang);
			return MetadataValuesCris;
		}
		return basic;
	}

	@Override
	public String getMetadata(Item item, String field) {
		StringTokenizer dcf = new StringTokenizer(field, ".");

		String[] tokens = { "", "", "" };
		int i = 0;
		while (dcf.hasMoreTokens()) {
			tokens[i] = dcf.nextToken().trim();
			i++;
		}
		String schema = tokens[0];
		String element = tokens[1];
		String qualifier = tokens[2];

		if ("*".equals(qualifier)) {
			qualifier = Item.ANY;
		} else if ("".equals(qualifier)) {
			qualifier = null;
		}

		String lang = Item.ANY;
		List<IMetadataValue> results = getMetadata(item, schema, element, qualifier, lang);
		if (results != null && !results.isEmpty()) {
			return results.get(0).getValue();
		}
		return null;
	}

}