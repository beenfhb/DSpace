/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.AuthorityInfo;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;

/**
 * Broker for metadata authority settings configured for each metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 *  # is field authority controlled (i.e. store authority, confidence values)?
 *  {@code authority.controlled.<FIELD> = true}
 *
 *  # is field required to have an authority value, or may it be empty?
 *  # default is false.
 *  {@code authority.required.<FIELD> = true | false}
 *
 *  # default value of minimum confidence level for ALL fields - must be
 *  # symbolic confidence level, see org.dspace.content.authority.Choices
 *  {@code authority.minconfidence = uncertain}
 *
 *  # minimum confidence level for this field
 *  {@code authority.minconfidence.SCHEMA.ELEMENT.QUALIFIER = SYMBOL}
 *    e.g.
 *  {@code authority.minconfidence.dc.contributor.author = accepted}
 *
 * NOTE: There is *expected* to be a "choices" (see ChoiceAuthorityManager)
 * configuration for each authority-controlled field.
 *
 * @see org.dspace.content.authority.ChoiceAuthorityServiceImpl
 * @see org.dspace.content.authority.Choices
 * @author Larry Stone
 */
public interface MetadataAuthorityService {

    /** 
     * Predicate - is field authority-controlled?
     * @param metadataField metadata field
     * @return true/false
     */
    public boolean isAuthorityControlled(MetadataField metadataField);

    /** 
     * Predicate - is field authority-controlled?
     * @param fieldKey field key
     * @return true/false
     */
    public boolean isAuthorityControlled(String fieldKey);

    /** 
     * Predicate - is authority value required for field?
     * @param metadataField metadata field
     * @return true/false 
     */
    public boolean isAuthorityRequired(MetadataField metadataField);

    /** 
     * Predicate - is authority value required for field?
     * @param fieldKey field key
     * @return  true/false 
     */
    public boolean isAuthorityRequired(String fieldKey);


    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     * @param metadataField metadata field
     * @return field key
     */
    public String makeFieldKey(MetadataField metadataField);

    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     * @param schema schema
     * @param element element
     * @param qualifier qualifier
     * @return field key
     */
    public String makeFieldKey(String schema, String element, String qualifier);

    /**
     * Give the minimal level of confidence required to consider valid an authority value
     * for the given metadata.
     * @param metadataField metadata field
     * @return the minimal valid level of confidence for the given metadata
     */
    public int getMinConfidence(MetadataField metadataField);
    public int getMinConfidence(Context context, String schema, String element, String qualifier) throws SQLException;

    /**
     * Return the list of metadata field with authority control. The strings
     * are in the form <code>schema.element[.qualifier]</code>
     *
     * @return the list of metadata field with authority control
     */
    public List<String> getAuthorityMetadata();

    public AuthorityInfo getAuthorityInfo(Context context, String md) throws SQLException;

    public List<String> listAuthorityKeyIssued(String md, int limit, int page) throws SQLException;

    public long countIssuedAuthorityKeys(String metadata) throws SQLException;

     /**
     * Find all the items in the archive with a given authority key value
     * in the indicated metadata field and a confidence level not acceptable.
     *
     * @see Choices#CF_ACCEPTED
     * @param context DSpace context object
     * @param metadata metadata field schema.element.qualifier
     * @param authority the value of authority key to look for
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     */
    public List<Item> findIssuedByAuthorityValue(String metadata,
            String authority) throws SQLException, AuthorizeException, IOException;

    public long countIssuedItemsByAuthorityValue(String metadata, String key) throws SQLException;

    public String findNextIssuedAuthorityKey(String metadata, String focusKey) throws SQLException;

    public String findPreviousIssuedAuthorityKey(String metadata, String focusKey) throws SQLException;
    
    public List<Item> findIssuedByAuthorityValueAndConfidence(String metadata,
            String authority, int confidence) throws SQLException, AuthorizeException, IOException;
    
    
    /*
     *	Methods for query an authority about all metadata binded to it  
     */
    
    public AuthorityInfo getAuthorityInfoByAuthority(String authorityName) throws SQLException;

    public List<String> listAuthorityKeyIssuedByAuthority(String authorityName, int limit, int page) throws SQLException;

    public long countIssuedAuthorityKeysByAuthority(String authorityName) throws SQLException;

    public List<Item> findIssuedByAuthorityValueInAuthority(String authorityName,
            String authority) throws SQLException, AuthorizeException, IOException;

    public long countIssuedItemsByAuthorityValueInAuthority(String authorityName, String key) throws SQLException;

    public String findNextIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException;

    public String findPreviousIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException;
    
    public List<Item> findIssuedByAuthorityValueAndConfidenceInAuthority(String authorityName,
            String authority, int confidence) throws SQLException, AuthorizeException, IOException;
}
