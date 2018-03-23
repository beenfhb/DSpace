/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.utils.DSpace;

import com.google.gson.JsonObject;


public class ClaimRPJSONServlet extends JSONRequest{
	Logger log = Logger.getLogger(ClaimRPJSONServlet.class);
	
	private static SearchService searcher;
	 
	private int validateCreateEperson(Context context,String mail, String rpKey) {
		
		ApplicationService applicationservice = new DSpace().getServiceManager().getServiceByName("applicationService", ApplicationService.class);
		
		ResearcherPage r=  applicationservice.getEntityByCrisId(rpKey, ResearcherPage.class);
		int status=1;
		if(StringUtils.equals(mail, r.getEmail().getValue() )){
            context.turnOffAuthorisationSystem();
            EPerson eperson;
			try {
				eperson = EPersonServiceFactory.getInstance().getEPersonService().create(context);
				eperson.setEmail(mail);
				eperson.setCanLogIn(true);
				eperson.setFirstName(context, "");
				eperson.setLanguage(context, "en");
				eperson.setLastName(context, "");
				eperson.setRequireCertificate(false);
				eperson.setSelfRegistered(false);
				
				EPersonServiceFactory.getInstance().getEPersonService().update(context, eperson);
                log.info(LogManager.getHeader(context,
                        "sendtoken_forgotpw", "email=" + mail));
                EPersonServiceFactory.getInstance().getAccountService().sendForgotPasswordInfo(context, mail);
    			
			} catch (SQLException e) {
				status = -1;
				log.error(e.getMessage(), e);
			} catch (AuthorizeException e) {
				status = -1;
				log.error(e.getMessage(), e);
			} catch (IOException e) {
				status = -1;
				log.error(e.getMessage(), e);
			} catch (MessagingException e) {
				status = -1;
				log.error(e.getMessage(), e);
			}finally{
				context.restoreAuthSystemState();
				if(status<0){
					context.abort();
				}else{
					try {
						context.commit();
					} catch (SQLException e) {
						status =-1;
						log.error(e.getMessage(), e);
					}
				}
			}
			return status;
		}
		status =-2;
		return status;
	}

	@Override
	public void doJSONRequest(Context context, HttpServletRequest req,
			HttpServletResponse resp) throws AuthorizeException, IOException {

	    int status =0;
	    
		String mail = req.getParameter("mailUser");
		String rpKey = req.getParameter("rpKey");
		boolean claimEnabled = ConfigurationManager.getBooleanProperty("cris", "rp.claim.enabled");
		boolean isSelfClaimed = false;
        EPerson currUser = context.getCurrentUser();
        
        String nameGroupSelfClaim = ConfigurationManager.getProperty("cris",
                "rp.claim.group.name");
        if (StringUtils.isNotBlank(nameGroupSelfClaim))
        {
            Group selfClaimGroup;
            try
            {
            	GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
                selfClaimGroup = groupService.findByName(context,
                        nameGroupSelfClaim);
                if (groupService.isMember(context, selfClaimGroup))
                {
                    isSelfClaimed = true;
                }
            }
            catch (SQLException e)
            {
                status = -1;
                log.error(e.getMessage(), e);
            }

        }
		
        if (claimEnabled && (isSelfClaimed || currUser != null)
                && StringUtils.isNotBlank(rpKey))
        {
            try
            {
                ApplicationService applicationservice = new DSpace()
                        .getServiceManager().getServiceByName(
                                "applicationService", ApplicationService.class);

                ResearcherPage r = applicationservice.getEntityByCrisId(rpKey,
                        ResearcherPage.class);
                if (r.getEpersonID() == null)
                {
                    r.setEpersonID(currUser.getID());
                    applicationservice.saveOrUpdate(ResearcherPage.class, r);                    
                }
                else
                {
                    status = -1;
                }
            }
            catch (Exception e)
            {
                status = -1;
                log.error(e.getMessage(), e);
            }

        }
        else if(!isSelfClaimed && claimEnabled && StringUtils.isNotBlank(mail) && StringUtils.isNotBlank(rpKey)){
			status = validateCreateEperson(context, mail, rpKey);
		}
        JsonObject jo = new JsonObject();
        jo.addProperty("result", status);
        resp.getWriter().write(jo.toString());
	}
	
	

}
