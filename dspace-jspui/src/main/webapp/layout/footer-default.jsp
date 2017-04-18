<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Footer for home page
  --%>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.core.I18nUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
	// Is anyone logged in?
	EPerson user = (EPerson) request.getAttribute("dspace.current.user");
	
	// Is the logged in user an admin
	Boolean admin = (Boolean)request.getAttribute("is.admin");
	boolean isAdmin = (admin == null ? false : admin.booleanValue());
	
	// E-mail may have to be truncated
	String navbarEmail = null;
	
	if (user != null)
	{
	    navbarEmail = user.getEmail();
	}
	
	// get the locale languages
	Locale[] supportedLocales = I18nUtil.getSupportedLocales();
	Locale sessionLocale = UIUtil.getSessionLocale(request);
%>

            <%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null)
    {
%>
	</div>
	<div class="col-md-3">
                    <%= sidebar %>
    </div>
    </div>       
<%
    }
%>
</div>
</main>
            <%-- Page footer --%>
             <footer class="container navbar navbar-inverse navbar-bottom">
	             <div class="row">
	             	<div class="col-md-3 col-sm-6">
	             	<a href="" target="_blank">
			<img id="logo-sx-footer" src="<%= request.getContextPath() %>/static/fvg/LogoSX-UNITY.png"
				border="0"></a>
	             	<%-- <p><fmt:message key="jsp.layout.footer-default.misc"/>&nbsp;<a href="" target="_blank">[read more <i class="fa fa-external-link"></i>]</a></p> --%>
	             	</div>
	             	<div class="col-md-3 col-sm-6">
	             		<div class="panel panel-primary">
	             			<div class="panel-heading">
	             				<h6 class="panel-title"><fmt:message key="jsp.layout.footer-default.infobox"/></h6>
	             			</div>
	             			<div class="panel-body">
	             			<ul>
           
           						<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfobox1"/>"><fmt:message key="jsp.layout.footer-default.infobox1"/></a></li>
           						<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfobox2"/>"><fmt:message key="jsp.layout.footer-default.infobox2"/></a></li>
           						<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfobox3"/>"><fmt:message key="jsp.layout.footer-default.infobox3"/></a></li>
           
							</ul>
	             			</div>
	             		</div>
	             	</div>
	             	<div class="col-md-6 col-sm-12">
	             		<div class="panel panel-primary">
	             			<div class="panel-heading">
	             				<h6 class="panel-title"><fmt:message key="jsp.layout.footer-default.linkbox"/></h6>
	             			</div>
	             			<div class="panel-body">
	             			<ul>
								<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfolink1"/>"><fmt:message key="jsp.layout.footer-default.infolink1"/></a></li>
           						<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfolink2"/>"><fmt:message key="jsp.layout.footer-default.infolink2"/></a></li>
           						<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfolink3"/>"><fmt:message key="jsp.layout.footer-default.infolink3"/></a></li>
           						<li><a href="<fmt:message key="jsp.layout.footer-default.linkinfolink4"/>"><fmt:message key="jsp.layout.footer-default.infolink4"/></a></li>
							</ul>
	             			</div>
	             		</div>
	             	</div>
<%-- 	             	<div class="col-md-3 col-sm-6">
	             		<div class="panel panel-primary">
	             			<div class="panel-heading">
	             				<h6 class="panel-title"><fmt:message key="jsp.layout.footer-default.contactus"/></h6>
	             			</div>
	             			<div class="panel-body">
	             			<p><fmt:message key="jsp.layout.footer-default.content.contactus"/></p>
	             			</div>
	             		</div>
	             	</div> --%>
	             </div>
             <div class="extra-footer row">
            
	<div class="pull-left col-md-3 col-sm-2">
         <%
    if (user != null)
    {
		%>
		<a href="<%= request.getContextPath() %>/mydspace"><i class="fa fa-user"></i> <fmt:message key="jsp.layout.navbar-default.loggedin">
		      <fmt:param><%= StringUtils.abbreviate(navbarEmail, 20) %></fmt:param>
		  </fmt:message></a> |
		  <a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/> <i class="fa fa-sign-out"></i> </a>
		<%
    } else {
		%>
             <a href="<%= request.getContextPath() %>/mydspace"><i class="fa fa-key"></i> <fmt:message key="jsp.layout.navbar-default.sign"/></a>
	<% } %>             
		<%
		  if (isAdmin)
		  {
		%>
			   |  
               <a href="<%= request.getContextPath() %>/dspace-admin"><fmt:message key="jsp.administer"/> <i class="fa fa-cogs"></i> </a>
		<%
		  }
	if (supportedLocales != null && supportedLocales.length > 1)
     {
 %> |
       <a href="#" class="dropdown-toggle" data-toggle="dropdown"><fmt:message key="jsp.layout.navbar-default.language"/><b class="caret"></b></a>
        <ul class="dropdown-menu">
 <%
    for (int i = supportedLocales.length-1; i >= 0; i--)
     {
 %>
      <li>
        <a onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';
                  document.repost.submit();" href="<%= request.getContextPath() %>?locale=<%=supportedLocales[i].toString()%>">
         <%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>
       </a>
      </li>
 <%
     }
 %>
     </ul>
 <%
   }
 %>
		</div>
		<div id="footer_feedback" class="col-md-6 col-sm-7 text-center">                                    
                     <a target="_blank" href=""><fmt:message key="jsp.layout.footer-default.privacy"/>&nbsp;<i class="fa fa-external-link"></i></a> |
                     <a target="_blank" href=""><fmt:message key="jsp.layout.footer-default.copyright"/>&nbsp;<i class="fa fa-external-link"></i></a> |
                     <a href="<%= request.getContextPath() %>/feedback"><fmt:message key="jsp.layout.footer-default.feedback"/></a>
                     <a href="<%= request.getContextPath() %>/htmlmap"></a></p>
                </div>
                <div id="designedby" class="col-md-3 col-sm-3">
            		<div class="pull-right">
	            	 	<fmt:message key="jsp.layout.footer-default.text"/> - 
	            	 	<fmt:message key="jsp.layout.footer-default.theme-by"/> 
	            	 	<a href="http://www.cineca.it">
	            	 		<img src="<%= request.getContextPath() %>/image/logo-cineca-small.png"
	                                    alt="Logo CINECA" height="32px"/></a>
	                </div>
				</div>
		</div>
    </footer>
    </body>
</html>