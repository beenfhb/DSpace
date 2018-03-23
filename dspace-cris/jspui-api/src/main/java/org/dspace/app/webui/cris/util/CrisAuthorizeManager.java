package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

import it.cilea.osd.jdyna.model.AuthorizationContext;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.ITabService;

public class CrisAuthorizeManager
{

    public static <A extends AuthorizationContext, T extends ACrisObject, PD extends PropertiesDefinition> boolean authorize(
            Context context, ITabService applicationService, Class<T> clazz, Class<PD> classPD,
            Integer id, A authorizedObject) throws SQLException
    {
        Integer visibility = authorizedObject.getVisibility();
        if (VisibilityTabConstant.HIGH.equals(visibility))
        {
            return true;
        }

        boolean result = false;
        T object = null;
        try
        {
            object = ((ApplicationService) applicationService).get(clazz, id);
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException(e);
        }

        EPerson currUser = context.getCurrentUser();

        if (visibility != VisibilityTabConstant.POLICY)
        {
            boolean isOwner = false;

            if (currUser != null)
            {
                isOwner = object.isOwner(currUser);
            }

            // check admin authorization
            if (AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context))
            {
                if (VisibilityTabConstant.ADMIN.equals(visibility)
                        || VisibilityTabConstant.STANDARD.equals(visibility))
                {
                    return true;
                }
                if (isOwner)
                {
                    return true;
                }
                return false;
            }
            if (VisibilityTabConstant.LOW.equals(visibility)
                    || VisibilityTabConstant.STANDARD.equals(visibility))
            {
                if (isOwner)
                {
                    return true;
                }
            }

        }

        if (currUser != null)
        {
            List<PD> listPolicySingle = authorizedObject
                    .getAuthorizedSingle();

            if (listPolicySingle != null && !listPolicySingle.isEmpty())
            {
                for (PD policy : listPolicySingle)
                {
                    String data = object.getMetadata(policy.getShortName());
                    if (StringUtils.isNotBlank(data))
                    {
                        if (currUser.getID() == UUID.fromString(data))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        List<PD> listPolicyGroup = authorizedObject.getAuthorizedGroup();

        if (listPolicyGroup != null && !listPolicyGroup.isEmpty())
        {
            for (PD policy : listPolicyGroup)
            {
                List<String> policies = object.getMetadataValue(policy.getShortName());
                for (String data : policies)
                {
                    if (StringUtils.isNotBlank(data))
                    {
                        Group group = EPersonServiceFactory.getInstance().getGroupService().find(context,
                                UUID.fromString(data));
                        if (group != null)
                        {
                            if (currUser == null)
                            {
                                return false;
                            }
                            else if (EPersonServiceFactory.getInstance().getGroupService().isMember(context, group))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    
    public static <T extends ACrisObject> boolean isAdmin(
            Context context, T crisObject) throws SQLException 
    {
        // check admin authorization
        if (AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context))
        {
            return true;
        }

        String crisObjectTypeText = crisObject.getTypeText();
        EPerson currUser = context.getCurrentUser();        
        String groupName = ConfigurationManager.getProperty("cris", "admin" + crisObjectTypeText);
        if(StringUtils.isBlank(groupName)) {
            groupName = "Administrator "+crisObjectTypeText;
        }
        Group group = EPersonServiceFactory.getInstance().getGroupService().findByName(context, groupName);
        if (group != null)
        {
            if (currUser == null)
            {
                boolean isMember = EPersonServiceFactory.getInstance().getGroupService().isMember(context, Group.ADMIN);
                if (isMember)
                {
                    return true;
                }
            }
            if (EPersonServiceFactory.getInstance().getGroupService().isMember(context, group))
            {
                return true;
            }
        }
        return false;
    }
}
