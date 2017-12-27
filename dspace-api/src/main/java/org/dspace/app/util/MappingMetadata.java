/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.utils.DSpace;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public abstract class MappingMetadata {

	private final static Logger log = Logger.getLogger(MappingMetadata.class);
    
	protected BrowsableDSpaceObject item;

	protected String itemURL;

	// Configuration keys and fields
	protected Map<String, String> configuredFields = new HashMap<String, String>();

	// Google field names (e.g. citation_fieldname) and formatted metadata
	// values
	protected ListMultimap<String, String> metadataMappings = ArrayListMultimap.create();

	private static final int SINGLE = 0;

	private static final int MULTI = 1;

	private static final int ALL_FIELDS_IN_OPTION = 2;

	// Patter to extract the converter name if any
	private static final Pattern converterPattern = Pattern.compile(".*\\((.*)\\)");

    private GoogleBitstreamComparator googleBitstreamComparator;
    
	// Load configured fields from google-metadata.properties
	public void init(String configuration) {
	    
		File loadedFile = null;
		URL url = null;
		InputStream is = null;

		String googleConfigFile = ConfigurationManager.getProperty(configuration);
		log.info("Using [" + googleConfigFile + "] for configuration");

		loadedFile = new File(googleConfigFile);
		try {
			url = loadedFile.toURL();

		} catch (MalformedURLException mux) {
			log.error("Can't find " + configuration + " metadata configuration file: " + googleConfigFile, mux);
		}

		Properties properties = new Properties();
		try {
			is = url.openStream();
			properties.load(is);

		} catch (IOException iox) {
			log.error("Could not read metadata configuration file: " + googleConfigFile, iox);
		}

		Enumeration propertyNames = properties.propertyNames();

		while (propertyNames.hasMoreElements()) {
			String key = ((String) propertyNames.nextElement()).trim();

			if (key.startsWith(getPrefix())) {

				String name = key.substring(getPrefix().length());
				String field = properties.getProperty(key);

				if (null != name && !name.equals("") && null != field && !field.equals("")) {
					configuredFields.put(name.trim(), field.trim());
				}
			}
		}

		if (log.isDebugEnabled()) {
			logConfiguration();
		}
	}

	protected abstract String getPrefix();

    /**
     * Add a single metadata value to the Google field, defaulting to the
     * first-encountered instance of the field for this Item.
     * 
     * @param fieldName
     * @return successful?
     */
    protected boolean addSingleField(String fieldName)
    {

		String config = configuredFields.get(fieldName);

        if (null == config || config.equals(""))
        {
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Processing " + fieldName);
        }

        if (config.equals("$handle"))
        {
            if (null != itemURL && !itemURL.equals(""))
            {
                metadataMappings.put(fieldName, itemURL);
                return true;
            }
            else
            {
                return false;
            }
        }

        if (config.equals("$simple-pdf"))
        {
            String pdf_url = getPDFSimpleUrl(item);
            if(pdf_url.length() > 0)
            {
                metadataMappings.put(fieldName, pdf_url);
                return true;
            } else
            {
                return false;
            }
        }

        IMetadataValue v = resolveMetadataField(config);

        if (null != v && (null != v.getValue()) && !v.getValue().trim().equals(""))
        {
            metadataMappings.put(fieldName, v.getValue());
            return true;
        }
        else
        {
            // No values found
            return false;
        }
    }

	/**
	 * Gets the URL to a PDF using a very basic strategy by assuming that the
	 * PDF is in the default content bundle, and that the item only has one
	 * public bitstream and it is a PDF.
	 *
	 * @param item
	 * @return URL that the PDF can be directly downloaded from
	 */
	private String getPDFSimpleUrl(BrowsableDSpaceObject item) {
		try {
			Bitstream bitstream = findLinkableFulltext(item);
			if (bitstream != null) {
				StringBuilder path = new StringBuilder();
				path.append(ConfigurationManager.getProperty("dspace.url"));

				if (item.getHandle() != null) {
					path.append("/bitstream/");
					path.append(item.getHandle());
					path.append("/");
					path.append(bitstream.getSequenceID());
				} else {
					path.append("/retrieve/");
					path.append(bitstream.getID());
				}

				path.append("/");
				path.append(Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING));
				return path.toString();
			}
		} catch (UnsupportedEncodingException ex) {
			log.debug(ex.getMessage());
		} catch (SQLException ex) {
			log.debug(ex.getMessage());
		}

		return "";
	}

    /**
     * A singular version of resolveMetadata to return only one field value
     * instead of an aggregate.
     * 
     * @param configFilter
     * @return The first configured match of metadata field for the item.
     */
    protected IMetadataValue resolveMetadataField(String configFilter)
    {

        ArrayList<IMetadataValue> fields = resolveMetadata(configFilter, SINGLE);
        if (null != fields && fields.size() > 0)
        {
            return fields.get(0);
        }

        return null;
    }

    /**
     * A plural version of resolveMetadata for aggregate fields.
     * 
     * @param configFilter
     * @return Aggregate of all matching metadata fields configured in the first
     *         option field-set to return any number of filter matches.
     */
    protected ArrayList<IMetadataValue> resolveMetadataFields(String configFilter)
    {

        ArrayList<IMetadataValue> fields = resolveMetadata(configFilter, MULTI);
        if (null != fields && fields.size() > 0)
        {
            return fields;
        }
        return null;
    }

    /**
     * Aggregate an array of DCValues present on the current item that pass the
     * configuration filter.
     * 
     * @param configFilter
     * @param returnType
     * @return Array of configuration to item-field matches
     */
    protected ArrayList<IMetadataValue> resolveMetadata(String configFilter,
            int returnType)
    {

        if (null == configFilter || configFilter.trim().equals("")
                || !configFilter.contains("."))
        {
            log.error("The configuration string [" + configFilter
                    + "] is invalid.");
            return null;
        }
        else
        {
            configFilter = configFilter.trim();
        }
        ArrayList<ArrayList<String>> parsedOptions = parseOptions(configFilter);

        if (log.isDebugEnabled())
        {
            log
                    .debug("Resolved Fields For This Item Per Configuration Filter:");
            for (int i = 0; i < parsedOptions.size(); i++)
            {
                ArrayList<String> optionFields = parsedOptions.get(i);

                log.debug("Option " + (i + 1) + ":");
                for (String f : optionFields)
                {
                    log.debug("{" + f + "}");
                }
            }
        }

        // Iterate through each configured option's field-set until
        // we have a match.
        for (ArrayList<String> optionFields : parsedOptions)
        {

            int optionMatches = 0;
            String[] components;
            List<IMetadataValue> values;
            ArrayList<IMetadataValue> resolvedFields = new ArrayList<IMetadataValue>();

            for (String field : optionFields)
            {

                components = parseComponents(field);
                values = item.getMetadata(components[0], components[1],
                        components[2], Item.ANY);

                if (values.size() > 0)
                {
                    for (IMetadataValue v : values)
                    {

                        resolvedFields.add(v);

                        if (returnType == SINGLE)
                        {
                            if (!resolvedFields.isEmpty())
                            {
                                if (log.isDebugEnabled())
                                {
                                    log
                                            .debug("Resolved Field Value For This Item:");
                                    for (IMetadataValue r : resolvedFields)
                                    {
                                        log.debug("{" + r.getValue() + "}");
                                    }
                                }
                                return resolvedFields;
                            }
                        }
                    }
                }
            }

            // If the item had any of the fields contained in this option,
            // return them, otherwise move on to the next option's field-set.
            if (!resolvedFields.isEmpty())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Resolved Field Values For This Item:");
                    for (IMetadataValue v : resolvedFields)
                    {
                        log.debug("{" + v.getValue() + "}");
                    }
                }

                // Check to see if this is a full option match
                if (ALL_FIELDS_IN_OPTION == returnType)
                {
                    if (resolvedFields.size() == optionMatches)
                    {
                        return resolvedFields;
                    }
                    // Otherwise, if there are any matches for the option,
                    // return them.
                }
                else if (MULTI == returnType)
                {
                    return resolvedFields;
                }
            }
        }
        return null;
    }

    /**
     * Parse first-match path of metadata field-group options for the given
     * configuration.
     * 
     * @param configFilter
     * @return array of parsed options or null
     */
    protected ArrayList<ArrayList<String>> parseOptions(String configFilter)
    {

        ArrayList<String> options = new ArrayList<String>();
        ArrayList<ArrayList<String>> parsedOptions = new ArrayList<ArrayList<String>>();

        if (null == configFilter || configFilter.equals(""))
        {
            return null;
        }

        if (configFilter.contains("|"))
        {

            String[] configOptions = configFilter.split("\\|");

            for (String option : configOptions)
            {
                options.add(option.trim());
            }
        }
        else
        {
            options = new ArrayList<String>();
            options.add(configFilter);
        }

        // Parse first-match path options. The first option (field-set)
        // to match fields present in the item is used.
        ArrayList<String> parsedFields;

        // Parse the fields for each field-set in order.
        for (String option : options)
        {

            ArrayList<String> fields;
            parsedFields = new ArrayList<String>();

            if (option.contains(","))
            {
                fields = parseFields(option);
            }
            else
            {
                fields = new ArrayList<String>();
                fields.add(option);
            }

            // Parse field list for this field-set, expanding any wildcards.
            for (String field : fields)
            {

                if (field.contains("*"))
                {

                    ArrayList<String> wc = parseWildcard(field);
                    for (String wcField : wc)
                    {
                        if (!parsedFields.contains(wcField))
                        {
                            parsedFields.add(wcField);
                        }
                    }

                }
                else
                {
                    if (!parsedFields.contains(field))
                    {
                        parsedFields.add(field);
                    }
                }
            }

            parsedOptions.add(parsedFields);
        }

        if (null != parsedOptions)
        {
            return parsedOptions;
        }
        else
        {
            return null;
        }
    }

    /**
     * Build a Vector of fields that can be added to when expanding wildcards.
     * 
     * @param configString
     *            - Value of one metadata field configuration
     * @return A vector of raw field configurations.
     */
    protected ArrayList<String> parseFields(String configString)
    {

        ArrayList<String> fields = new ArrayList<String>();

        for (String field : configString.split("\\,"))
        {
            fields.add(field.trim());
        }

        return fields;
    }

    /**
     * Pull apart an individual field structure.
     * 
     * @param field
     *            The configured field for one metadata field map
     * @return Schema, Element, Qualifier of metadata field
     */
    protected String[] parseComponents(String field)
    {

        int index = 0;
        String[] components = new String[3];

        for (String c : field.split("\\."))
        {
            components[index] = c.trim();
            index++;
        }

        return components;
    }

    /**
     * Expand any wildcard characters to an array of all matching fields for
     * this item. No order consistency is implied.
     * 
     * @param field
     *            The field identifier containing a wildcard character.
     * @return Expanded field list.
     */
    protected ArrayList<String> parseWildcard(String field)
    {

        if (!field.contains("*"))
        {
            return null;
        }
        else
        {
            String[] components = parseComponents(field);

            for (int i = 0; i < components.length; i++)
            {
                if (components[i].trim().equals("*"))
                {
                    components[i] = Item.ANY;
                }
            }

            List<IMetadataValue> allMD = item.getMetadata(components[0], components[1],
                    components[2], Item.ANY);

            ArrayList<String> expandedDC = new ArrayList<String>();
            for (IMetadataValue v : allMD)
            {

                // De-dup multiple occurrences of field names in item
                if (!expandedDC.contains(buildFieldName(v)))
                {
                    expandedDC.add(buildFieldName(v));
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("Field Names From Expanded Wildcard \"" + field
                        + "\"");
                for (String v : expandedDC)
                {
                    log.debug("    " + v);
                }
            }

            return expandedDC;
        }
    }
    
    /**
     * Construct metadata field name out of IMetadataValue components
     * 
     * @param v
     *            The IMetadataValue to construct a name for.
     * @return The complete metadata field name.
     */
    protected String buildFieldName(IMetadataValue v)
    {

        StringBuilder name = new StringBuilder();

        MetadataField metadataField = v.getMetadataField();
        MetadataSchema metadataSchema = v.getMetadataField().getMetadataSchema();
        name.append(metadataSchema.getName()).append(".").append(metadataField.getElement());
        if (null != metadataField.getQualifier())
        {
            name.append("." + metadataField.getQualifier());
        }

        return name.toString();
    }
    
	/**
	 * Dump Metadata field mapping to log
	 * 
	 */
	public void logConfiguration() {
		log.debug("Google Metadata Configuration Mapping:");

		for (String name : configuredFields.keySet()) {
			log.debug("  " + name + " => " + configuredFields.get(name));
		}
	}

	
    /**
     * Gets the URL to a PDF using a very basic strategy by assuming that the PDF
     * is in the default content bundle, and that the item only has one public bitstream
     * and it is a PDF.
     *
     * @param item
     * @return URL that the PDF can be directly downloaded from
     */
    protected String getPDFSimpleUrl(Item item)
    {
        try {
            Bitstream bitstream = findLinkableFulltext(item);
            if (bitstream != null) {
                StringBuilder path = new StringBuilder();
                path.append(ConfigurationManager.getProperty("dspace.url"));

                if (item.getHandle() != null) {
                    path.append("/bitstream/");
                    path.append(item.getHandle());
                    path.append("/");
                    path.append(bitstream.getSequenceID());
                } else {
                    path.append("/retrieve/");
                    path.append(bitstream.getID());
                }

                path.append("/");
                path.append(Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING));
                return path.toString();
            }
        } catch (UnsupportedEncodingException ex) {
            log.debug(ex.getMessage());
        } catch (SQLException ex) {
            log.debug(ex.getMessage());
        }

        return "";
    }

    /**
     * A bitstream is considered linkable fulltext when it is either
     * <ul>
     *     <li>the item's only bitstream (in the ORIGINAL bundle); or</li>
     *     <li>the primary bitstream</li>
     * </ul>
     * Additionally, this bitstream must be publicly viewable.
     * @param item
     * @return a linkable bitstream or null if none found
     * @throws SQLException if database error
     */
    protected Bitstream findLinkableFulltext(BrowsableDSpaceObject dso) throws SQLException {
        Bitstream bestSoFar = null;
        if (item instanceof Item)
        {
            Item item = (Item) dso;
            List<Bundle> contentBundles = item.getItemService().getBundles(item,
                    "ORIGINAL");
            for (Bundle bundle : contentBundles)
            {
                List<Bitstream> bitstreams = bundle.getBitstreams();
                Collections.sort(bitstreams, googleBitstreamComparator);

                for (Bitstream candidate : bitstreams) {
                    if (candidate.equals(bundle.getPrimaryBitstream())) { // is primary -> use this one
                        if (isPublic(candidate)) {
                            return candidate;
                        }
                    } else {
                        if (bestSoFar == null && isPublic(candidate)) { //if bestSoFar is null but the candidate is not public you don't use it and try to find another
                            bestSoFar = candidate;
                        }
                    }
                }
            }
        }
        return bestSoFar;
    }

    protected boolean isPublic(Bitstream bitstream) {
        if (bitstream == null) {
            return false;
        }
        boolean result = false;
        Context context = null;
        try {
            context = new Context();
            result = AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context, bitstream, Constants.READ, true);
        } catch (SQLException e) {
            log.error("Cannot determine whether bitstream is public, assuming it isn't. bitstream_id=" + bitstream.getID(), e);
        }
        return result;
    }

    /**
     * 
     * 
     * @param field
     *            to aggregate all values of in a matching option
     * @param delimiter
     *            to delimit field values with
     */
    protected void addAggregateValues(String field, String delimiter)
    {

        String authorConfig = configuredFields.get(field);
        ArrayList<IMetadataValue> fields = resolveMetadataFields(authorConfig);

        if (null != fields && !fields.isEmpty())
        {

            StringBuilder fieldMetadata = new StringBuilder();
            int count = 0;

            for (IMetadataValue metadataValue : fields)
            {
                fieldMetadata.append(metadataValue.getValue());
                if (count < fields.size() - 1)
                {
                    fieldMetadata.append(delimiter).append(" ");
                    count++;
                }
            }
            metadataMappings.put(field, fieldMetadata.toString());
        }
    }

    /**
     * If metadata field contains multiple values, then add each value to the map separately
     * @param FIELD
     */
    protected void addMultipleValues(String FIELD)
    {
        String fieldConfig = configuredFields.get(FIELD);
        ArrayList<IMetadataValue> fields = resolveMetadataFields(fieldConfig);

        if (null != fields && !fields.isEmpty())
        {
            for (IMetadataValue field : fields)
            {
                //TODO if this is author field, first-name first
                metadataMappings.put(FIELD, field.getValue());
            }
        }
    }
    
	protected void addMultipleWithAuthorityValues(String FIELD) {
		String fieldConfig = configuredFields.get(FIELD);
		ArrayList<IMetadataValue> fields = resolveMetadataFields(fieldConfig);

		if (null != fields && !fields.isEmpty()) {
			for (IMetadataValue field : fields) {
				if (StringUtils.isNotBlank(field.getAuthority())) {
					metadataMappings.put(FIELD, field.getAuthority());
				} else {
					metadataMappings.put(FIELD, field.getValue());
				}
			}
		}
	}


	/**
	 * Identifies if this item matches a particular configuration of fields and
	 * values for those fields to identify the type based on a type- cataloging
	 * metadata practice.
	 * 
	 * @param dConfig
	 * @return
	 */
	protected boolean identifyItemType(String dConfig) {
		// FIXME: Shouldn't have to parse identifiers for every identification.

		ArrayList<ArrayList<String>> options = parseOptions(dConfig);
		HashMap<String, ArrayList<String>> mdPairs = new HashMap<String, ArrayList<String>>();

		// Parse field/value pairs from field identifier string
		for (ArrayList<String> option : options) {

			String pair = option.get(0);
			String[] parsedPair = pair.split("\\:");
			if (2 == parsedPair.length) {
				// If we've encountered this field before, add the value to the
				// list
				if (mdPairs.containsKey(parsedPair[0].trim())) {
					mdPairs.get(parsedPair[0].trim()).add(parsedPair[1]);
					if (log.isDebugEnabled()) {
						log.debug("Registering Type Identifier:  " + parsedPair[0] + " => " + parsedPair[1]);
					}
				} else {
					// Otherwise, add it as the first occurrence of this field
					ArrayList<String> newField = new ArrayList<String>();
					newField.add(parsedPair[1].trim());
					mdPairs.put(parsedPair[0].trim(), newField);

					if (log.isDebugEnabled()) {
						log.debug("Registering Type Identifier:  " + parsedPair[0] + " => " + parsedPair[1]);
					}
				}
			} else {
				log.error("Malformed field identifier name/value pair");
			}
		}

		// Build config string without values, only field names
		StringBuilder sb = new StringBuilder();
		for (String value : mdPairs.keySet()) {
			sb.append(value + " | ");
		}

		// Check resolved/present metadata fields against configured values
		ArrayList<IMetadataValue> presentMD = resolveMetadataFields(sb.toString());
		if (null != presentMD && presentMD.size() != 0) {
			for (IMetadataValue v : presentMD) {
				String fieldName = buildFieldName(v);
				if (mdPairs.containsKey(fieldName)) {
					for (String configValue : mdPairs.get(fieldName)) {
						if (configValue.equals(v.getValue())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected boolean addCitation(String fieldName) {

		String config = configuredFields.get(fieldName);

		if (null == config || config.equals("")) {
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Processing " + fieldName);
		}
		final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
				.getNamedPlugin(StreamDisseminationCrosswalk.class, config);

		OutputStream outputStream = new ByteArrayOutputStream();
		String citation = "";
		Context context = null;
		try {
			context = new Context();
			streamCrosswalkDefault.disseminate(context, item, outputStream);
			citation = outputStream.toString();
		} catch (CrosswalkException | IOException | AuthorizeException | SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}

		if (citation.length() > 0) {
			metadataMappings.put(fieldName, citation);
			return true;
		} else {
			return false;
		}
	}

	protected boolean addLanguageField(String fieldName) {

		String config = configuredFields.get(fieldName);
		String languageConfig = configuredFields.get(fieldName + ".language");

		if (null == config || config.equals("")) {
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Processing " + fieldName);
		}

		String language = "";
		// check if exist another configuration for language
		// check if the key got language
		Matcher converterMatcher = converterPattern.matcher(config);
		if (converterMatcher.matches()) {
			language = converterMatcher.group(1);
			config = config.replaceAll("\\(" + language + "\\)", "");
		}
		if (StringUtils.isEmpty(language)) {
			IMetadataValue v = resolveMetadataField(languageConfig);
			language = v.getValue();
		}

		IMetadataValue v = resolveMetadataField(config);

		if (null != v && StringUtils.isNotBlank(v.getValue())) {
			metadataMappings.put(fieldName, v.getValue());
			metadataMappings.put(fieldName + ".language", language);
			return true;
		} else {
			// No values found
			return false;
		}
	}

	protected boolean addDateField(String fieldName) {

		String config = configuredFields.get(fieldName);
		if (null == config || config.equals("")) {
			return false;
		}

		DSpace dspace = new DSpace();
		MultiFormatDateParser multiFormatDateParser = dspace.getSingletonService(MultiFormatDateParser.class);

		IMetadataValue v = resolveMetadataField(config);
		if (null != v && StringUtils.isNotBlank(v.getValue())) {
			Date date = multiFormatDateParser.parse(v.getValue());
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			int day = cal.get(Calendar.DAY_OF_MONTH);
			metadataMappings.put(fieldName + ".year", "" + year);
			metadataMappings.put(fieldName + ".month", "" + (month + 1));
			metadataMappings.put(fieldName + ".day", "" + day);
			return true;
		}
		return false;
	}

	protected boolean addInvertedValues(String fieldName) {

		String config = configuredFields.get(fieldName);

		if (null == config || config.equals("")) {
			return false;
		}

		String identifierCode = "";
		// check if exist another configuration for language
		if (null == identifierCode || identifierCode.equals("")) {
			// check if the key got language
			Matcher converterMatcher = converterPattern.matcher(config);
			if (converterMatcher.matches()) {
				identifierCode = converterMatcher.group(1);
				config = config.replaceAll("\\(" + identifierCode + "\\)", "");
			}
		}

		IMetadataValue v = resolveMetadataField(config);

		if (null != v && StringUtils.isNotBlank(v.getValue())) {
			metadataMappings.put(fieldName, v.getValue());
			metadataMappings.put(v.getValue(), identifierCode);
			return true;
		} else {
			// No values found
			return false;
		}
	}

	protected boolean addMultiInvertedValues(String fieldName) {

		String config = configuredFields.get(fieldName);

		if (null == config || config.equals("")) {
			return false;
		}

		Map<String, String> map = new HashMap<String, String>();
        String result = "";
        String[] ss = config.split(",");
        int index = 0;
        for(String s : ss) {
            String identifierCode = "";
            // check if exist another configuration for language
            if (null == identifierCode || identifierCode.equals("")) {
                // check if the key got language
                Matcher converterMatcher = converterPattern.matcher(s);
                if (converterMatcher.matches()) {
                    identifierCode = converterMatcher.group(1);                    
                    if(!s.contains("$simple-handle")) {
                        s = s.replaceAll("\\(" + identifierCode + "\\)", "");
                        if(index>0) {
                            result += ",";
                        }
                        result += s;
                        map.put(s.trim(), identifierCode);
                        index++;
                    }
                    else {
                        map.put("handle", identifierCode);
                    }
                }
            }
        }
		
        if (config.contains("$simple-handle")) {
            if (null != item.getHandle() && !item.getHandle().equals("")) {
                metadataMappings.put(fieldName, item.getHandle());
                metadataMappings.put(item.getHandle(), map.get("handle"));
            } 
        }		
		
		ArrayList<IMetadataValue> fields = resolveMetadataFields(result);

		if (null != fields && !fields.isEmpty()) {
			for (IMetadataValue v : fields) {
				if (null != v && StringUtils.isNotBlank(v.getValue())) {
					metadataMappings.put(fieldName, v.getValue());
					metadataMappings.put(v.getValue(), map.get(v.getMetadataField()));
				}
			}
		} else {
			// No values found
			return false;
		}
		return true;
	}

	protected boolean addCurrencyField(String fieldName) {

		String config = configuredFields.get(fieldName);
		String languageConfig = configuredFields.get(fieldName + ".currencycode");

		if (null == config || config.equals("")) {
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Processing " + fieldName);
		}

		String language = "";
		// check if exist another configuration for language
		// check if the key got language
		Matcher converterMatcher = converterPattern.matcher(config);
		if (converterMatcher.matches()) {
			language = converterMatcher.group(1);
			config = config.replaceAll("\\(" + language + "\\)", "");
		}
		if (StringUtils.isEmpty(language)) {
			IMetadataValue v = resolveMetadataField(languageConfig);
			if (null != v && StringUtils.isNotBlank(v.getValue())) {
				language = v.getValue();
			}
		}

		IMetadataValue v = resolveMetadataField(config);

		if (null != v && StringUtils.isNotBlank(v.getValue())) {
			metadataMappings.put(fieldName, v.getValue());
			metadataMappings.put(fieldName + ".currencycode", language);
			return true;
		} else {
			// No values found
			return false;
		}
	}

	public GoogleBitstreamComparator getGoogleBitstreamComparator() {
		return googleBitstreamComparator;
	}

	public void setGoogleBitstreamComparator(GoogleBitstreamComparator googleBitstreamComparator) {
		this.googleBitstreamComparator = googleBitstreamComparator;
	}
}
