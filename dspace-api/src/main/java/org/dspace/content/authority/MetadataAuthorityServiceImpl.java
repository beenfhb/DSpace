/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Broker for metadata authority settings configured for each metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 *  {@code
 *  # is field authority controlled (i.e. store authority, confidence values)?
 *  authority.controlled.<FIELD> = true
 *
 *  # is field required to have an authority value, or may it be empty?
 *  # default is false.
 *  authority.required.<FIELD> = true | false
 *
 *  # default value of minimum confidence level for ALL fields - must be
 *  # symbolic confidence level, see org.dspace.content.authority.Choices
 *  authority.minconfidence = uncertain
 *
 *  # minimum confidence level for this field
 *  authority.minconfidence.SCHEMA.ELEMENT.QUALIFIER = SYMBOL
 *    e.g.
 *  authority.minconfidence.dc.contributor.author = accepted
 *  }
 * NOTE: There is *expected* to be a "choices" (see ChoiceAuthorityManager)
 * configuration for each authority-controlled field.
 *
 * @see ChoiceAuthorityServiceImpl
 * @see Choices
 * @author Larry Stone
 */
public class MetadataAuthorityServiceImpl implements MetadataAuthorityService
{
    private static Logger log = Logger.getLogger(MetadataAuthorityServiceImpl.class);

    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;

    @Autowired(required = true)
    protected MetadataValueService metadataValueService;
    
    // map of field key to authority plugin
    protected Map<String,Boolean> controlled = new HashMap<String,Boolean>();

    // map of field key to answer of whether field is required to be controlled
    protected Map<String,Boolean> isAuthorityRequired = null;

    /**
     * map of field key to answer of which is the min acceptable confidence
     * value for a field with authority
     */
    protected Map<String, Integer> minConfidence = new HashMap<String, Integer>();

    /** fallback default value unless authority.minconfidence = X is configured. */
    protected int defaultMinConfidence = Choices.CF_ACCEPTED;

    private AuthorityDAO authorityDAO = null;
    
    protected MetadataAuthorityServiceImpl()
    {

    }

    public void init() {

        if(isAuthorityRequired == null)
        {
            isAuthorityRequired = new HashMap<String,Boolean>();
            Enumeration pn = ConfigurationManager.propertyNames();
            final String authPrefix = "authority.controlled.";
            Context context = new Context();
            try {            	
            	authorityDAO = AuthorityDAOFactory.getInstance(context);
                while (pn.hasMoreElements())
                {
                    String key = (String)pn.nextElement();
                    if (key.startsWith(authPrefix))
                    {
                        // field is expected to be "schema.element.qualifier"
                        String field = key.substring(authPrefix.length());
                        int dot = field.indexOf('.');
                        if (dot < 0)
                        {
                            log.warn("Skipping invalid MetadataAuthority configuration property: "+key+": does not have schema.element.qualifier");
                            continue;
                        }
                        String schema = field.substring(0, dot);
                        String element = field.substring(dot+1);
                        String qualifier = null;
                        dot = element.indexOf('.');
                        if (dot >= 0)
                        {
                            qualifier = element.substring(dot+1);
                            element = element.substring(0, dot);
                        }

                        String fkey = null;
                        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
                        if(metadataField == null)
                        {
							log.warn("Error while configuring authority control, metadata field: " + field
									+ " could not be found");
							log.warn(
									"Instead thrown exception add the follow field to the authority controlled environment: "
											+ field);

							if (qualifier == null) {
								fkey = schema + "_" + element;
							} else {
								fkey = schema + "_" + element + "_" + qualifier;
							}
                        }
                        else {
                        	fkey = metadataField.toString();
                        }
                        boolean ctl = ConfigurationManager.getBooleanProperty(key, true);
                        boolean req = ConfigurationManager.getBooleanProperty("authority.required."+field, false);
                        controlled.put(fkey, ctl);
                        isAuthorityRequired.put(fkey, req);

                        // get minConfidence level for this field if any
                        int mci = readConfidence("authority.minconfidence."+field);
                        if (mci >= Choices.CF_UNSET)
                        {
                            minConfidence.put(metadataField.toString(), mci);
                        }
                        log.debug("Authority Control: For schema="+schema+", elt="+element+", qual="+qualifier+", controlled="+ctl+", required="+req);
                    }
                }
            } catch (SQLException e) {
                log.error("Error reading authority config", e);
            }


            // get default min confidence if any:
            int dmc = readConfidence("authority.minconfidence");
            if (dmc >= Choices.CF_UNSET)
            {
                defaultMinConfidence = dmc;
            }
        }        
        
    }

    private int readConfidence(String key)
    {
        String mc = ConfigurationManager.getProperty(key);
        if (mc != null)
        {
            int mci = Choices.getConfidenceValue(mc.trim(), Choices.CF_UNSET-1);
            if (mci == Choices.CF_UNSET-1)
            {
                log.warn("IGNORING bad value in DSpace Configuration, key="+key+", value="+mc+", must be a valid Authority Confidence keyword.");
            }
            else
            {
                return mci;
            }
        }
        return Choices.CF_UNSET-1;
    }

    @Override
    public boolean isAuthorityControlled(MetadataField metadataField)
    {
        init();
        return isAuthorityControlled(makeFieldKey(metadataField));
    }

    @Override
    public boolean isAuthorityControlled(String fieldKey)
    {
        init();
        return controlled.containsKey(fieldKey) && controlled.get(fieldKey);
    }

    @Override
    public boolean isAuthorityRequired(MetadataField metadataField)
    {
        init();
        return isAuthorityRequired(makeFieldKey(metadataField));
    }

    @Override
    public boolean isAuthorityRequired(String fieldKey)
    {
        init();
        Boolean result = isAuthorityRequired.get(fieldKey);
        return (result != null) && result;
    }

    @Override
    public String makeFieldKey(MetadataField metadataField)
    {
        init();
        return metadataField.toString();
    }

    @Override
    public String makeFieldKey(String schema, String element, String qualifier) {
        init();
        if (qualifier == null)
        {
            return schema + "_" + element;
        }
        else
        {
            return schema + "_" + element + "_" + qualifier;
        }
    }


    /**
     * Give the minimal level of confidence required to consider valid an authority value
     * for the given metadata.
     * @param metadataField metadata field
     * @return the minimal valid level of confidence for the given metadata
     */
    @Override
    public int getMinConfidence(MetadataField metadataField)
    {
        init();
        Integer result = minConfidence.get(makeFieldKey(metadataField));
        return result == null ? defaultMinConfidence : result;
    }

    @Override
    public List<String> getAuthorityMetadata() {
        init();
        List<String> copy = new ArrayList<>();
        for (String s : controlled.keySet())
        {
            copy.add(s.replaceAll("_","."));
        }
        return copy;
    }

	@Override
	public AuthorityInfo getAuthorityInfo(Context context, String md) throws SQLException {
		init();
        int[] fieldIds = new int[]{getFieldId(context, md)};
        return authorityDAO.getAuthorityInfoByFieldIds(md, fieldIds);
	}

	@Override
	public List<String> listAuthorityKeyIssued(String md, int limit, int page) throws SQLException {
		init();
		return authorityDAO.listAuthorityKeyIssued(md, limit, page);
	}

	@Override
	public long countIssuedAuthorityKeys(String metadata) throws SQLException {
		init();
		return authorityDAO.countIssuedAuthorityKeys(metadata);
	}

	@Override
	public List<Item> findIssuedByAuthorityValue(String metadata, String authority)
			throws SQLException, AuthorizeException, IOException {
		init();
		return authorityDAO.findIssuedByAuthorityValue(metadata, authority);
	}

	@Override
	public long countIssuedItemsByAuthorityValue(String metadata, String key) throws SQLException {
		init();
		return authorityDAO.countIssuedItemsByAuthorityValue(metadata, key);
	}

	@Override
	public String findNextIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
		init();
		return authorityDAO.findNextIssuedAuthorityKey(metadata, focusKey);
	}

	@Override
	public String findPreviousIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
		init();
		return authorityDAO.findPreviousIssuedAuthorityKey(metadata, focusKey);
	}

	@Override
	public List<Item> findIssuedByAuthorityValueAndConfidence(String metadata, String authority, int confidence)
			throws SQLException, AuthorizeException, IOException {
		init();
		return authorityDAO.findIssuedByAuthorityValueAndConfidence(metadata, authority, confidence);
	}

	@Override
	public AuthorityInfo getAuthorityInfoByAuthority(String authorityName) throws SQLException {
		init();
		return authorityDAO.getAuthorityInfoByAuthority(authorityName);
	}

	@Override
	public List<String> listAuthorityKeyIssuedByAuthority(String authorityName, int limit, int page)
			throws SQLException {
		init();
		return authorityDAO.listAuthorityKeyIssuedByAuthority(authorityName, limit, page);
	}

	@Override
	public long countIssuedAuthorityKeysByAuthority(String authorityName) throws SQLException {
		init();
		return authorityDAO.countIssuedAuthorityKeysByAuthority(authorityName);
	}

	@Override
	public List<Item> findIssuedByAuthorityValueInAuthority(String authorityName, String authority)
			throws SQLException, AuthorizeException, IOException {
		init();
		return authorityDAO.findIssuedByAuthorityValueInAuthority(authorityName, authority);
	}

	@Override
	public long countIssuedItemsByAuthorityValueInAuthority(String authorityName, String key) throws SQLException {
		init();
		return authorityDAO.countIssuedItemsByAuthorityValueInAuthority(authorityName, key);
	}

	@Override
	public String findNextIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException {
		init();
		return authorityDAO.findNextIssuedAuthorityKeyInAuthority(authorityName, focusKey);
	}

	@Override
	public String findPreviousIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException {
		init();
		return authorityDAO.findPreviousIssuedAuthorityKeyInAuthority(authorityName, focusKey);		
	}

	@Override
	public List<Item> findIssuedByAuthorityValueAndConfidenceInAuthority(String authorityName, String authority,
			int confidence) throws SQLException, AuthorizeException, IOException {
		init();
		return authorityDAO.findIssuedByAuthorityValueAndConfidenceInAuthority(authorityName, authority, confidence);
	}
	
	/*
	 * UTILITY METHODS
	 */
    private int getFieldId(Context context, String md) throws IllegalArgumentException, SQLException {
        String[] metadata = md.split("\\.");
        return ContentServiceFactory.getInstance().getMetadataFieldService().findFieldsByElementNameUnqualified(context, metadata[0], md).get(0).getID();
    }
    
    private int[] getFieldIds(Context context, String authorityName) throws IllegalArgumentException, SQLException {
        List<String> metadata = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService()
                .getAuthorityMetadataForAuthority(authorityName);
        int[] ids = new int[metadata.size()];
        
        for (int i = 0; i < metadata.size(); i++)
        {
            ids[i] = getFieldId(context, metadata.get(i));
        }
        return ids;
    }
    
	@Override
	public int getMinConfidence(Context context, String schema, String element, String qualifier) throws SQLException {
		return getMinConfidence(metadataFieldService.findByElement(context, schema, element, qualifier));
	}

}
