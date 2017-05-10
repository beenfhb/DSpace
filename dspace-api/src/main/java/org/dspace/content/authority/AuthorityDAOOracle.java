/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.hibernate.Session;

/**
 * This class is the Oracle driver class for reading information about the
 * authority use. It implements the AuthorityDAO interface, and also has a
 * constructor of the form:
 *
 * AuthorityDAOPostgres(Context context)
 *
 * As required by AuthorityDAOFactory. This class should only ever be loaded by
 * that Factory object.
 *
 * @author bollini
 */
public class AuthorityDAOOracle extends AuthorityDAO {

	private Context context;

	// private static final String SQL_NUM_METADATA_GROUP_BY_AUTHKEY_CONFIDENCE
	// = "select authority, confidence, count(*) as num from item left join
	// metadatavalue on (item.uuid = metadatavalue.resource_id and
	// metadatavalue.resource_type_id=2) where in_archive = 1 and
	// metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) group by authority,
	// confidence";
	private static final String SQL_NUM_METADATA_AUTH_GROUP_BY_CONFIDENCE = "select confidence, count(*) as num from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null group by confidence";

	private static final String SQL_NUM_AUTHORED_ITEMS = "select count(distinct item.uuid) as num from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null";

	private static final String SQL_NUM_ISSUED_ITEMS = "select count(distinct item.uuid) as num from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and confidence <> "
			+ Choices.CF_ACCEPTED;

	// private static final String SQL_NUM_METADATA_GROUP_BY_AUTH_ISSUED =
	// "select authority is not null as hasauthority, confidence <> "+
	// Choices.CF_ACCEPTED +" as bissued, count(*) as num from tem ileft join
	// metadatavalue on (item.uuid = metadatavalue.resource_id and
	// metadatavalue.resource_type_id=2) where in_archive = 1 and
	// metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) group by hasauthority,
	// bissued";

	private static final String SQL_NUM_METADATA = "select count(*) as num from metadatavalue where metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER)";

	private static final String SQL_NUM_AUTHKEY = "select count (distinct authority) as num from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null";

	private static final String SQL_NUM_AUTHKEY_ISSUED = "select count (distinct authority) as num from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and confidence <> "
			+ Choices.CF_ACCEPTED + " and authority is not null";

	private static final String SQL_AUTHKEY_ISSUED = "SELECT * FROM (SELECT rownum rnum, a.* FROM ( SELECT DISTINCT authority FROM item LEFT JOIN metadatavalue ON (item.uuid = metadatavalue.dspace_object_id) WHERE in_archive = 1 AND metadata_field_id IN (QUESTION_ARRAY_PLACE_HOLDER) AND authority IS NOT NULL AND confidence <> "
			+ Choices.CF_ACCEPTED + " ORDER BY authority) a WHERE rownum <=:par0) WHERE rnum >=:par1";

	private static final String SQL_NUM_ITEMSISSUED_BYKEY = "select count (distinct item.uuid) as num from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and confidence <> "
			+ Choices.CF_ACCEPTED + " and authority = :authority";

	private static final String SQL_NEXT_ISSUED_AUTHKEY = "select min(authority) as key from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and authority > :par0 and confidence <> "
			+ Choices.CF_ACCEPTED;

	private static final String SQL_PREV_ISSUED_AUTHKEY = "select max(authority) as key from item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) where in_archive = 1 and  metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and authority < :par0 and confidence <> "
			+ Choices.CF_ACCEPTED;

	private static final String SQL_ITEMSISSUED_BYKEY_AND_CONFIDENCE = "SELECT item.* FROM item left join metadatavalue on (item.uuid = metadatavalue.dspace_object_id) WHERE in_archive=1 AND metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) AND authority = :par0 AND confidence = :par1";

	private static final String SQL_FINDISSUEDBYAUTHORITYVALUEANDFIELDID = "SELECT item.* FROM item left join metadatavalue " +
            "on (item.uuid = metadatavalue.dspace_object_id) " +
            "WHERE in_archive=1 AND metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) AND authority = :par0 AND confidence <> "+ Choices.CF_ACCEPTED;

	public AuthorityDAOOracle(Context context) {
		this.context = context;
	}
	
	public String getSqlFindissuedbyauthorityvalueandfieldid() {
		return SQL_FINDISSUEDBYAUTHORITYVALUEANDFIELDID;
	}

	public Session getHibernateSession() throws SQLException {
		return ((Session) context.getDBConnection().getSession());
	}

	public String getSqlNumMetadataAuthGroupByConfidence() {
		return SQL_NUM_METADATA_AUTH_GROUP_BY_CONFIDENCE;
	}

	public String getSqlNumAuthoredItems() {
		return SQL_NUM_AUTHORED_ITEMS;
	}

	public String getSqlNumIssuedItems() {
		return SQL_NUM_ISSUED_ITEMS;
	}

	public String getSqlNumMetadata() {
		return SQL_NUM_METADATA;
	}

	public String getSqlNumAuthkey() {
		return SQL_NUM_AUTHKEY;
	}

	public String getSqlNumAuthkeyIssued() {
		return SQL_NUM_AUTHKEY_ISSUED;
	}

	public String getSqlAuthkeyIssued() {
		return SQL_AUTHKEY_ISSUED;
	}

	public String getSqlNumItemsissuedBykey() {
		return SQL_NUM_ITEMSISSUED_BYKEY;
	}

	public String getSqlNextIssuedAuthkey() {
		return SQL_NEXT_ISSUED_AUTHKEY;
	}

	public String getSqlPrevIssuedAuthkey() {
		return SQL_PREV_ISSUED_AUTHKEY;
	}

	public String getSqlItemsissuedBykeyAndConfidence() {
		return SQL_ITEMSISSUED_BYKEY_AND_CONFIDENCE;
	}

	@Override
	Context getContext() {
		return context;
	}

}
