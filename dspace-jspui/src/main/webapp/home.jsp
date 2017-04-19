<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  -    recent.submissions - RecetSubmissions
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@page import="org.dspace.services.factory.DSpaceServicesFactory"%>
<%@page import="org.dspace.services.ConfigurationService"%>
<%@page import="org.dspace.browse.BrowsableDSpaceObject"%>
<%@page import="org.dspace.core.factory.CoreServiceFactory"%>
<%@page import="org.dspace.core.service.NewsService"%>
<%@page import="org.dspace.content.service.CommunityService"%>
<%@page import="org.dspace.content.factory.ContentServiceFactory"%>
<%@page import="org.dspace.content.service.ItemService"%>
<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.discovery.configuration.DiscoveryViewConfiguration" %>
<%@page import="org.dspace.app.webui.components.MostViewedBean"%>
<%@page import="org.dspace.app.webui.components.MostViewedItem"%>
<%@page import="org.dspace.discovery.SearchUtils"%>
<%@page import="org.dspace.discovery.IGlobalSearchResult"%>
<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@ page import="org.dspace.app.webui.util.LocaleUIHelper" %>

<%
    List<Community> communities = (List<Community>) request.getAttribute("communities");

    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    NewsService newsService = CoreServiceFactory.getInstance().getNewsService();
    String topNews = newsService.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String sideNews = newsService.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    boolean feedEnabled = configurationService.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        // FeedData is expected to be a comma separated list
        String[] formats = configurationService.getArrayProperty("webui.feed.formats");
        String allFormats = StringUtils.join(formats, ",");
        feedData = "ALL:" + allFormats;
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recent.submissions");
    MostViewedBean mostViewedItem = (MostViewedBean) request.getAttribute("mostViewedItem");
    MostViewedBean mostCitedItem = (MostViewedBean) request.getAttribute("mostCitedItem");
    MostViewedBean mostViewedBitstream = (MostViewedBean) request.getAttribute("mostDownloadedItem");
    boolean isRtl = StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "","rtl"));
%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData %>">
<div class="row">
	<div class="col-md-8 sm-12 pull-<%= isRtl? "right":"left" %>">
        <%= topNews %>

	<%
    	int discovery_panel_cols = 8;
    	int discovery_facet_cols = 4;
    	Map<String, List<FacetResult>> mapFacetes = (Map<String, List<FacetResult>>) request.getAttribute("discovery.fresults");
    	List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig");
    	String processorSidebar = (String) request.getAttribute("processorSidebar");
    	String processorGlobal = (String) request.getAttribute("processorGlobal");
          
    if(processorGlobal!=null && processorGlobal.equals("global")) {
		%>
	<%@ include file="discovery/static-globalsearch-component-facet.jsp" %>
	<% } %>        
		  </div>
	<div class="col-md-4 sm-12 pull-<%= isRtl? "left":"right" %>">
    <%@ include file="components/recent-submissions.jsp" %>
	</div>
</div>
<div class="row">
	<div class="col-md-4 <%= isRtl ? "pull-right":""%>">
		<%@ include file="components/most-viewed.jsp" %>	
	</div>
	<div class="col-md-4 <%= isRtl ? "pull-right":""%>">
		<%@ include file="components/most-downloaded.jsp" %>
	</div>
	<div class="col-md-4 <%= isRtl ? "pull-left":""%>">
	<%= sideNews %>
	<%-- <%@ include file="discovery/static-tagcloud-facet.jsp" %> --%>
	<%-- <%@ include file="components/most-cited.jsp" %> --%>
	</div>
</div>
<%
if (communities != null && communities.size() != 0)
{
%>
<div class="row">
	<div class="col-md-5">		
               <h3><fmt:message key="jsp.home.com1"/></h3>
                <p><fmt:message key="jsp.home.com2"/></p>
				<div class="list-group">
<%
	boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.home-page.logos", true);
    for (int i = 0; i < communities.size(); i++)
    {
%><div class="list-group-item row">
<%  
		Bitstream logo = communities.get(i).getLogo();
		if (showLogos && logo != null) { %>
	<div class="col-md-3">
        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" /> 
	</div>
	<div class="col-md-9">
<% } else { %>
	<div class="col-md-12">
<% }  %>		
		<h4 class="list-group-item-heading"><a href="<%= request.getContextPath() %>/handle/<%= communities.get(i).getHandle() %>"><%= communities.get(i).getMetadata("name") %></a>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
		<span class="badge pull-right"><%= ic.getCount(communities.get(i)) %></span>
<%
        }

%>
		</h4>
		<p><%= communities.get(i).getMetadata("short_description") %></p>
    </div>
</div>                            
	
<%
}
}
    
    if(processorSidebar!=null && processorSidebar.equals("sidebar")) {
	%>
	<div class="col-md-7">
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
	</div>
	<% } %>	
</div>
<div class="row">
	<%@ include file="discovery/static-tagcloud-facet.jsp" %>
</div>
</dspace:layout>
