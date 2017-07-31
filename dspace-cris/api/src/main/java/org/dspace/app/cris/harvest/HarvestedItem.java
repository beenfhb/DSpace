
package org.dspace.app.cris.harvest;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;

public class HarvestedItem extends org.dspace.harvest.HarvestedItem{
	private static Logger log = Logger.getLogger(HarvestedItem.class);   
	 
    HarvestedItem(Context c, TableRow row) {
		super(c, row);
	}

    public static HarvestedItem find(Context c, int item_id) throws SQLException 
    {
    	TableRow row = DatabaseManager.findByUnique(c, "harvested_item", "item_id", item_id);
    	
    	if (row == null) {
    		return null;
    	}
    	
    	return new org.dspace.app.cris.harvest.HarvestedItem(c, row);
    }
    
    public static HarvestedItem create(Context c, int itemId, String itemOAIid) throws SQLException {
    	TableRow row = DatabaseManager.row("harvested_item");
    	row.setColumn("item_id", itemId);
    	row.setColumn("oai_id", itemOAIid);
    	DatabaseManager.insert(c, row);
    	
    	return new org.dspace.app.cris.harvest.HarvestedItem(c, row);    	
    }
    
	public static ResearcherPage getRPByOAIId(Context context, String sourceRef, String rpOaiID){
		DSpace dspace = new DSpace();
		ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
				ApplicationService.class);
		return applicationService.getEntityBySourceId(sourceRef, rpOaiID, ResearcherPage.class);

    }
        
    public static OrganizationUnit getOUByOAIId(Context context, String sourceRef, String ouOaiID){
    	DSpace dspace = new DSpace();
		ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
				ApplicationService.class);
		return applicationService.getEntityBySourceId(sourceRef, ouOaiID, OrganizationUnit.class);
    }
    
    public static ResearcherPage getRPByRemoteId(Context context, String sourceRef, String remoteID){
    	ResearcherPage rp = null;
    	try{
			TableRowIterator tri = DatabaseManager.query(context, 
	                " SELECT crisid FROM jdyna_values jdv JOIN cris_rp_prop crp ON jdv.id = crp.value_id"
	                + " JOIN cris_rp_pdef crpdef ON crp.typo_id = crpdef.id "
	                + "JOIN cris_rpage rp ON rp.id = crp.parent_id WHERE rp.sourceref= ? and shortName='harvestsourceid' and jdv.textvalue= ?",
	                sourceRef, remoteID);
			if(tri.hasNext()){
				TableRow r = tri.next(context);
				String crisID = r.getStringColumn("crisid");
				DSpace dspace = new DSpace();
				ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
						ApplicationService.class);
				rp = applicationService.getEntityByCrisId(crisID, ResearcherPage.class);
			}
    	} catch(Exception e){
    		log.error(e.getMessage(), e);
    	}
		return rp;
    }
    
    public static OrganizationUnit getOUByRemoteId(Context context, String sourceRef, String remoteID){
    	OrganizationUnit ou = null;
    	try{
			TableRowIterator tri = DatabaseManager.query(context, 
	                " SELECT crisid FROM jdyna_values jdv JOIN cris_ou_prop cou ON jdv.id = cou.value_id"
	                + " JOIN cris_ou_pdef coupdef ON cou.typo_id = coupdef.id "
	                + "JOIN cris_orgunit ou ON ou.id = cou.parent_id WHERE ou.sourceref= ? and shortName='harvestsourceid' and jdv.textvalue= ?",
	                sourceRef, remoteID);
			if(tri.hasNext()){
				TableRow r = tri.next(context);
				String crisID = r.getStringColumn("crisid");
				DSpace dspace = new DSpace();
				ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
						ApplicationService.class);
				ou = applicationService.getEntityByCrisId(crisID, OrganizationUnit.class);
			}
    	} catch(Exception e){
    		log.error(e.getMessage(), e);
    	}
		return ou;
    }
    
}
