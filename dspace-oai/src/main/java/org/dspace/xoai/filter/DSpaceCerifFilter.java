/**
 * 
 */
package org.dspace.xoai.filter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.data.DSpaceSolrItem;
import org.dspace.xoai.filter.results.DatabaseFilterResult;
import org.dspace.xoai.filter.results.SolrFilterResult;

import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterValue;

/**
 * 
 * This filter only works with solr
 * 
 * @author v.sabatini
 *
 */
public class DSpaceCerifFilter extends DSpaceFilter {
    private static Logger log = LogManager.getLogger(DSpaceSetSpecFilter.class);

	/**
	 * 
	 */
	public DSpaceCerifFilter() {
		// VSTODO Auto-generated constructor stub
		
	}

	/* (non-Javadoc)
	 * @see org.dspace.xoai.filter.DSpaceFilter#buildDatabaseQuery(org.dspace.core.Context)
	 */
	@Override
	public DatabaseFilterResult buildDatabaseQuery(Context context) {
		// TODO This filter only works with solr
		return null;
	}

	/* (non-Javadoc)
	 * @see org.dspace.xoai.filter.DSpaceFilter#buildSolrQuery()
	 */
	@Override
	public SolrFilterResult buildSolrQuery() {

		ParameterMap config = getConfiguration();
		String operand = config.get("cerifEntity").asSimpleType().asString();
		String operator = config.get("operator").asSimpleType().asString().trim();			
		switch(operator) {
			case "equal": return new SolrFilterResult("item.cerifEntity: " + operand);
			case "notEqual": return new SolrFilterResult("-(item.cerifEntity: " + operand + ")");
			case "isCerif": return new SolrFilterResult("item.isCerif: true ");
			case "isItem": return new SolrFilterResult("item.isCerif: false ");
		}

		return new SolrFilterResult(""); //VSTODO: che ritorno in caso di operatore errato?
		
	}

	/* (non-Javadoc)
	 * @see org.dspace.xoai.filter.DSpaceFilter#isShown(org.dspace.xoai.data.DSpaceItem)
	 */
	@Override
	public boolean isShown(DSpaceItem item) {
		if(!(item instanceof DSpaceSolrItem) ) return true;
		else {
			ParameterMap config = getConfiguration();
			String operand = config.get("cerifEntity").asSimpleType().asString();
			String operator = config.get("operator").asSimpleType().asString().trim();	
			if(((DSpaceSolrItem)item).getCerifEntity()!=null) {
				switch(operator) {
					case "equal": return ((DSpaceSolrItem)item).getCerifEntity().equalsIgnoreCase(operand) ;
					case "notEqual": return !((DSpaceSolrItem)item).getCerifEntity().equalsIgnoreCase(operand) ;
					case "isCerif": return ((DSpaceSolrItem)item).isCerif();
					case "isItem": return !((DSpaceSolrItem)item).isCerif();
				}
			}
		}
		return true;
	}

}
