/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.dao;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.dto.BitstreamInterface;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.app.cris.batch.dto.MetadataInterface;
import org.dspace.core.Context;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;

public abstract class ImpRecordDAO
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ImpRecordDAO.class);

    private final String GET_IMPRECORDIDTOITEMID_BY_IMP_RECORD_ID_QUERY = "SELECT * FROM imp_record_to_item WHERE imp_record_id = :par0";

    protected Context context;
    
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss");

    protected ImpRecordDAO(Context ctx)
    {
        context = ctx;
    }

    public Context getContext()
    {
        return context;
    }

    public abstract String getNEXTVALUESEQUENCE(String table);

    public int getNextValueSequence(String table) throws SQLException
    {
        int newID = (Integer)getHibernateSession(context).createSQLQuery(getNEXTVALUESEQUENCE(table)).uniqueResult();
        return newID;
    }

    public boolean checkImpRecordIdTOItemID(Integer imp_record_id)
            throws SQLException
    {
        Integer row = (Integer)getHibernateSession(context).createSQLQuery(GET_IMPRECORDIDTOITEMID_BY_IMP_RECORD_ID_QUERY).addScalar("imp_record_id", StandardBasicTypes.INTEGER).setParameter(0, imp_record_id).uniqueResult();
        if (row == null)
        {
            return false;
        }
        return row == -1 ? false : true;
    }

    public void write(DTOImpRecord impRecord, boolean buildUniqueImpRecordId) throws SQLException
    {
        String imp_record_id = impRecord.getImp_record_id();
        if(buildUniqueImpRecordId) {
            Calendar cal = Calendar.getInstance();
            String timestamp = df.format(cal.getTime());
            imp_record_id += ":" + timestamp;
        }
        log.debug("INSERT INTO imp_record " + " VALUES ( "
                + impRecord.getImp_id() + " , " + imp_record_id
                + " , " + impRecord.getImp_eperson_id() + " , "
                + impRecord.getImp_collection_id() + " , "
                + impRecord.getStatus() + " , " + impRecord.getOperation()
                + " , " + impRecord.getLast_modified() + " , "
                + impRecord.getHandle() + " )");
        getHibernateSession(context).createSQLQuery(
                "INSERT INTO imp_record(imp_id, imp_record_id, imp_eperson_id, imp_collection_id, status, operation, integra, last_modified, handle, imp_sourceref)"
                        + " VALUES (:par0, :par1, :par2, :par3, :par4, :par5, null, null, null, :par6)").setParameter(0, 
                impRecord.getImp_id()).setParameter(1, imp_record_id).setParameter(2, impRecord.getImp_eperson_id()).setParameter(3, impRecord.getImp_collection_id()).setParameter(4,
                impRecord.getStatus()).setParameter(5, impRecord.getOperation()).setParameter(6,
                impRecord.getImp_sourceRef()).executeUpdate();

        for (MetadataInterface o : impRecord.getMetadata())
        {
            //TODO authority confidence share
            log.debug("INSERT INTO imp_metadatavalue " + " VALUES ( "
                    + o.getPkey() + " , " + impRecord.getImp_id() + " , "
                    + o.getImp_schema() + " , " + o.getImp_element() + " , "
                    + o.getImp_qualifier() + " , " + o.getImp_value() + " , "
                    + o.getMetadata_order() + ")");
            if(o.getImp_qualifier()!=null && !o.getImp_qualifier().isEmpty()) {
            	getHibernateSession(context).createSQLQuery(
                        "INSERT INTO imp_metadatavalue(imp_metadatavalue_id, imp_id, imp_schema, imp_element, imp_qualifier, imp_value, imp_authority, imp_confidence, imp_share, metadata_order, text_lang)"
                                + " VALUES (:par0, :par1, :par2, :par3, :par4, :par5, null, null, null, :par6, :par7)").setParameter(0, o.getPkey()).setParameter(1, impRecord.getImp_id()).setParameter(2, o.getImp_schema()).setParameter(3, o.getImp_element()).setParameter(4, o.getImp_qualifier()).setParameter(5, o.getImp_value()).setParameter(6,
                        o.getMetadata_order()).setParameter(7, "en").executeUpdate();
            }
            else {
            	getHibernateSession(context).createSQLQuery(
                    "INSERT INTO imp_metadatavalue(imp_metadatavalue_id, imp_id, imp_schema, imp_element, imp_qualifier, imp_value, imp_authority, imp_confidence, imp_share, metadata_order, text_lang)"
                            + " VALUES (:par0, :par1, :par2, :par3, null, :par4, null, null, null, :par5, :par6)").setParameter(0, 
                    o.getPkey()).setParameter(1, impRecord.getImp_id()).setParameter(2, o.getImp_schema()).setParameter(3, o.getImp_element()).setParameter(4, o.getImp_value()).setParameter(5,                    
                    o.getMetadata_order()).setParameter(6, "en").executeUpdate();
            }
        }

        for (BitstreamInterface o : impRecord.getBitstreams())
        {
            // TODO manage blob
            log.debug(
                    "INSERT INTO imp_bitstream(imp_bitstream_id, imp_id, filepath, description, bundle, bitstream_order, primary_bitstream, assetstore, name, imp_blob, embargo_policy, embargo_start_date)"
                            + " VALUES ( " + o.getPkey() + " , "
                            + impRecord.getImp_id() + " , " + o.getFilepath()
                            + " , " + o.getDescription() + " , " + o.getBundle()
                            + " , " + o.getBitstream_order() + " , "
                            + o.getPrimary_bitstream() + " , "
                            + o.getAssetstore() + " , " + "null , "
                            + o.getEmbargoPolicy() + " , "
                            + o.getEmbargoStartDate());

            boolean primarybitstream = false;
            if (o.getPrimary_bitstream() != null
                    && o.getPrimary_bitstream() == true)
            {
                primarybitstream = true;
            }

            getHibernateSession(context).createSQLQuery(
                    "INSERT INTO imp_bitstream(imp_bitstream_id, imp_id, filepath, description, bundle, bitstream_order, primary_bitstream, assetstore, name, imp_blob, embargo_policy, embargo_start_date)"
                            + " VALUES (:imp_bitstream_id, :imp_id, :filepath, :description, :bundle, :bitstream_order, :primary_bitstream, :assetstore, :name, null, :embargo_policy, :embargo_start_date)").setParameter("imp_bitstream_id", 
                    o.getPkey()).setParameter("imp_id", impRecord.getImp_id()).setParameter("filepath", o.getFilepath()).setParameter("description", 
                    o.getDescription()).setParameter("bundle", o.getBundle()).setParameter("bitstream_order", o.getBitstream_order()).setParameter("primary_bitstream", 
                    primarybitstream).setParameter("assetstore", o.getAssetstore()).setParameter("name",o.getName()).setParameter("embargo_policy", 
                    o.getEmbargoPolicy()).setParameter("embargo_start_date", o.getEmbargoStartDate()).executeUpdate();

        }
    }

    
    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
    
}
