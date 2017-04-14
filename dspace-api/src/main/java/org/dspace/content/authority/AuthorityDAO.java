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
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.hibernate.Session;

/**
 * Interface for any class wishing to investigate the current use of authority
 * for Read Only operations. If you wish to modify the contents of a metadata
 * (authority key, confidence, etc.) you need to use the Item API.
 *
 * If you implement this class, and you wish it to be loaded via the
 * AuthorityDAOFactory you must supply a constructor of the form:
 *
 * public AuthorityDAOImpl(Context context) {}
 *
 * Where Context is the DSpace Context object
 *
 * @author bollini
 */
public abstract class AuthorityDAO {

	abstract Context getContext();

	public Session getHibernateSession() throws SQLException {
		return ((Session) getContext().getDBConnection().getSession());
	}

	abstract String getSqlNumMetadataAuthGroupByConfidence();

	abstract String getSqlNumAuthoredItems();

	abstract String getSqlNumIssuedItems();

	abstract String getSqlNumMetadata();

	abstract String getSqlNumAuthkey();

	abstract String getSqlNumAuthkeyIssued();

	abstract String getSqlAuthkeyIssued();

	abstract String getSqlNumItemsissuedBykey();

	abstract String getSqlNextIssuedAuthkey();

	abstract String getSqlPrevIssuedAuthkey();

	abstract String getSqlItemsissuedBykeyAndConfidence();
	
	abstract String getSqlFindissuedbyauthorityvalueandfieldid();
	
	public AuthorityInfo getAuthorityInfo(String md) throws SQLException {
		int[] fieldIds = new int[] { getFieldId(md) };
		return getAuthorityInfoByFieldIds(md, fieldIds);
	}

	public AuthorityInfo getAuthorityInfoByAuthority(String authorityName) throws SQLException {
		int[] fieldIds = getFieldIds(authorityName);
		return getAuthorityInfoByFieldIds(authorityName, fieldIds);
	}

	public AuthorityInfo getAuthorityInfoByFieldIds(String scope, int[] fieldIds) throws SQLException {
		long[] numMetadataByConfidence = new long[7];

		List<Object[]> tri = getHibernateSession()
				.createSQLQuery(getFinalQueryString(getSqlNumMetadataAuthGroupByConfidence(), fieldIds))
				.addScalar("confidence").addScalar("num").list();
		for (Object[] row : tri) {
			int conf = (Integer) row[0];
			if (conf < Choices.CF_NOVALUE || conf > Choices.CF_ACCEPTED) {
				conf = 0;
			}
			numMetadataByConfidence[conf / 100] = (Integer) row[1];
		}

		Object row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlNumAuthoredItems(), fieldIds))
				.addScalar("num").uniqueResult();
		long numItems = (Integer) row;
		if (numItems == -1) {
			numItems = 0;
		}

		row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlNumIssuedItems(), fieldIds))
				.addScalar("num");
		long numIssuedItems = (Integer) row;
		if (numIssuedItems == -1) {
			numIssuedItems = 0;
		}

		row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlNumMetadata(), fieldIds)).addScalar("num");
		long numTotMetadata = (Integer) row;

		row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlNumAuthkey(), fieldIds)).addScalar("num");
		long numAuthorityKey = (Integer) row;

		long numAuthorityIssued = countIssuedAuthorityKeys(fieldIds);

		return new AuthorityInfo(scope, fieldIds.length > 1, numMetadataByConfidence, numTotMetadata, numAuthorityKey,
				numAuthorityIssued, numItems, numIssuedItems);
	}

	public List<String> listAuthorityKeyIssued(String md, int limit, int page) throws SQLException {
		int[] fieldIds = new int[] { getFieldId(md) };
		return listAuthorityKeyIssuedByFieldId(fieldIds, limit, page);
	}

	public List<String> listAuthorityKeyIssuedByAuthority(String authorityName, int limit, int page)
			throws SQLException {
		int[] fieldIds = getFieldIds(authorityName);
		return listAuthorityKeyIssuedByFieldId(fieldIds, limit, page);
	}

	private List<String> listAuthorityKeyIssuedByFieldId(int[] fieldId, int limit, int page) throws SQLException {
		List<String> keys = new ArrayList<String>();

		List<Object[]> tri = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlAuthkeyIssued(), fieldId))
				.addScalar("authority").setParameter(0, page + limit).setParameter(1, limit).list();
		for (Object[] obj : tri) {
			String row = (String) obj[0];
			keys.add(row);
		}
		return keys;
	}

	public long countIssuedAuthorityKeys(String metadata) throws SQLException {
		return countIssuedAuthorityKeys(getFieldId(metadata));
	}

	public long countIssuedAuthorityKeysByAuthority(String authorityName) throws SQLException {
		return countIssuedAuthorityKeys(getFieldIds(authorityName));
	}

	private long countIssuedAuthorityKeys(int... fieldId) throws SQLException {
		Object row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlAuthkeyIssued(), fieldId))
				.addScalar("num").uniqueResult();
		long numAuthorityIssued = (Integer) row;
		return numAuthorityIssued;
	}

	public List<Item> findIssuedByAuthorityValue(String metadata, String authority)
			throws SQLException, AuthorizeException, IOException {
		int[] fieldId = new int[] { getFieldId(metadata) };

		return findIssuedByAuthorityValueAndFieldId(authority, fieldId);
	}

	public List<Item> findIssuedByAuthorityValueInAuthority(String authorityName, String authority)
			throws SQLException, AuthorizeException, IOException {
		int[] fieldId = getFieldIds(authorityName);

		return findIssuedByAuthorityValueAndFieldId(authority, fieldId);
	}

	private List<Item> findIssuedByAuthorityValueAndFieldId(String authority, int[] fieldId) throws SQLException {
		List<Item> rows = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlFindissuedbyauthorityvalueandfieldid(), fieldId)).addEntity(Item.class).setParameter(0,authority).list();
		return rows;
	}

	public long countIssuedItemsByAuthorityValue(String metadata, String key) throws SQLException {
		int[] fieldId = new int[] { getFieldId(metadata) };
		return countIssuedItemsByAuthorityValueAndFieldId(key, fieldId);
	}

	public long countIssuedItemsByAuthorityValueInAuthority(String authorityName, String key) throws SQLException {
		int[] fieldId = getFieldIds(authorityName);
		return countIssuedItemsByAuthorityValueAndFieldId(key, fieldId);
	}

	private long countIssuedItemsByAuthorityValueAndFieldId(String key, int[] fieldId) throws SQLException {
		Object row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlNumItemsissuedBykey(), fieldId)).addScalar("num").setParameter(0,key);
		long numAuthorityIssued = (Integer)row;
		return numAuthorityIssued;
	}

	public String findNextIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
		int[] fieldId = new int[] { getFieldId(metadata) };
		return findNextIssuedAuthorityKeyByFieldId(focusKey, fieldId);
	}

	public String findNextIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException {
		int[] fieldId = getFieldIds(authorityName);
		return findNextIssuedAuthorityKeyByFieldId(focusKey, fieldId);
	}

	private String findNextIssuedAuthorityKeyByFieldId(String focusKey, int[] fieldId) throws SQLException {
		Object row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlNextIssuedAuthkey(), fieldId)).setParameter(0, focusKey).uniqueResult();
		if (row != null) {
			return (String)row;
		}
		return null;
	}

	public String findPreviousIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
		int[] fieldId = new int[] { getFieldId(metadata) };
		return findPreviousIssuedAuthorityKeyByFieldId(focusKey, fieldId);
	}

	public String findPreviousIssuedAuthorityKeyInAuthority(String authorityName, String focusKey) throws SQLException {
		int[] fieldId = getFieldIds(authorityName);
		return findPreviousIssuedAuthorityKeyByFieldId(focusKey, fieldId);
	}

	private String findPreviousIssuedAuthorityKeyByFieldId(String focusKey, int[] fieldId) throws SQLException {
		Object row = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlPrevIssuedAuthkey(), fieldId)).setParameter(0, focusKey).uniqueResult();
		if (row != null) {
			return (String)row;
		}
		return null;
	}

	public List<Item> findIssuedByAuthorityValueAndConfidence(String metadata, String authority, int confidence)
			throws SQLException, AuthorizeException, IOException {
		int[] fieldId = new int[] { getFieldId(metadata) };

		return findIssuedByAuthorityValueAndConfidenceAndFieldId(authority, confidence, fieldId);
	}

	public List<Item> findIssuedByAuthorityValueAndConfidenceInAuthority(String authorityName, String authority,
			int confidence) throws SQLException, AuthorizeException, IOException {
		int[] fieldId = getFieldIds(authorityName);

		return findIssuedByAuthorityValueAndConfidenceAndFieldId(authority, confidence, fieldId);
	}

	private List<Item> findIssuedByAuthorityValueAndConfidenceAndFieldId(String authority, int confidence,
			int[] fieldId) throws SQLException {
		List<Item> rows = getHibernateSession().createSQLQuery(getFinalQueryString(getSqlItemsissuedBykeyAndConfidence(), fieldId)).addEntity(Item.class).setParameter(0,authority).setParameter(1,confidence).list();
		return rows;
	}

	/*
	 * UTILITY METHODS
	 */
	private int getFieldId(String md) throws IllegalArgumentException, SQLException {
		String[] metadata = md.split("\\.");
		int fieldId = -1;
		try {			
			fieldId = ContentServiceFactory.getInstance().getMetadataFieldService().findByElement(getContext(), metadata[0], metadata[1], metadata[2]).getID();
		} catch (NullPointerException npe) {
			// the metadata field is not defined
			throw new IllegalArgumentException("Error retriving metadata field for input the supplied string: " + md,
					npe);
		}

		return fieldId;
	}

	private int[] getFieldIds(String authorityName) throws IllegalArgumentException, SQLException {
		List<String> metadata = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService()
				.getAuthorityMetadataForAuthority(authorityName);
		int[] ids = new int[metadata.size()];

		for (int i = 0; i < metadata.size(); i++) {
			ids[i] = getFieldId(metadata.get(i));
		}
		return ids;
	}

	private String getFinalQueryString(String queryTemplate, int[] fieldId) {
		String questions = String.valueOf(fieldId[0]);
		// start from 1 the first question mark is build-in
		for (int idx = 1; idx < fieldId.length; idx++) {
			questions += ", " + fieldId[idx];
		}
		return queryTemplate.replace("QUESTION_ARRAY_PLACE_HOLDER", questions);
	}

}
