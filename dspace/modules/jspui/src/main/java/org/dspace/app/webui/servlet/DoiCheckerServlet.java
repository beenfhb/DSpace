package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.webui.util.DOIQueryConfigurator;
import org.dspace.app.webui.util.DoiFactoryUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;


public class DoiCheckerServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DoiCheckerServlet.class);
    
    DSpace dspace = new DSpace();

    SearchService searcher = dspace.getServiceManager().getServiceByName(
            SearchService.class.getName(), SearchService.class);
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
		if (!ConfigurationManager.getBooleanProperty("doi.admin.feature")) {
			JSPManager.showJSP(request, response, "/doi/notactive.jsp");
			return;
		}
    	String[] criteria = ConfigurationManager.getProperty("doi.list").split(",");
    	DOIQueryConfigurator doiConfigurator = new DOIQueryConfigurator();
    	//contains results to show on jsp -> key: criteria / value: #results -> e.g key: PG_Thesis / value: 266 
    	Map<String,Integer> result = new HashMap<String, Integer>();
    	
    	boolean haveResultFixed = false;
    	for(String type : criteria) {
    	    
    	    Query sqlQuery = getHibernateSession(context).createSQLQuery(
                    DoiFixUtilityCheckerServlet.getQuery()).addScalar("item_id").setParameter("criteria", type);
    	    List<UUID> tri = sqlQuery.list();
    	    if(tri!=null && !tri.isEmpty()) {
    	        haveResultFixed = true;
    	    }
    	    
    		String query = doiConfigurator.getQuery(type, new String[] {});
    		List<String> filters = doiConfigurator.getFilters(type);
    		
    		SolrQuery solrQuery = new SolrQuery();
    		solrQuery.setQuery(query);
    		DoiFactoryUtils.prepareDefaultSolrQuery(solrQuery);
    		solrQuery.setQuery(query);
    		solrQuery.setFields("search.resourceid", "search.resourcetype",
    				"handle");
    		solrQuery.setRows(0);
    		if (filters != null) {
    			for (String filter : filters) {
    				solrQuery.addFilterQuery(filter);
    			}
    		}
    		
    		QueryResponse rsp;
			try {
				rsp = searcher.search(solrQuery);				
	    		result.put(type, (int)rsp.getResults().getNumFound());
			} catch (SearchServiceException e) {
				log.error(e.getMessage(), e);
			}    		
    	}
    	
    	request.setAttribute("results", result);       
    	request.setAttribute("showFixLink", haveResultFixed);       
    	
        
        JSPManager.showJSP(request, response, "/doi/checkerDoiHome.jsp");
        
    }

    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
    
}
