/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

/**
 * Password Authentication to assign a group for domain
 * 
 * @author Riccardo Fazio (riccardo.fazio at 4science dot it)
 *
 */
public class PasswordAdvancedAuthentication extends PasswordAuthentication {

	private static Logger log = Logger.getLogger(PasswordAdvancedAuthentication.class);

	@Override
	public List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
		// Prevents anonymous users from being added to this group, and the
		// second check
		// ensures they are password users
		try {
			if (context.getCurrentUser() != null && StringUtils.isNotBlank(EPersonServiceFactory.getInstance()
					.getEPersonService().getPasswordHash(context.getCurrentUser()).toString())) {
				String groupNames = "";
				String groupNameDefault = ConfigurationManager.getProperty("authentication-password",
						"login.specialgroup");
				String specialDomain = null;
				String userEmail = context.getCurrentUser().getEmail();

				int idx = 1;
				while ((specialDomain = ConfigurationManager.getProperty("authentication-password",
						"login.specialdomain." + idx)) != null) {
					if (StringUtils.endsWith(userEmail, specialDomain)) {

						String specialGroupDomain = ConfigurationManager.getProperty("authentication-password",
								"login.specialgroup." + idx);
						if (StringUtils.isNotBlank(specialGroupDomain)) {
							groupNames = specialGroupDomain;
						} else {
							groupNames = groupNameDefault;
						}

						break;
					}
					idx++;
				}

				if (StringUtils.isNotBlank(groupNames)) {
					String[] groupNameArray = StringUtils.split(groupNames, ",");
					List<Group> specialGroupID = new ArrayList<Group>();

					for (String groupName : groupNameArray) {
						Group specialGroup = EPersonServiceFactory.getInstance().getGroupService().findByName(context,
								groupName);
						if (specialGroup != null) {
							specialGroupID.add(specialGroup);
						} else {
							// Oops - the group isn't there.
							log.warn(LogManager.getHeader(context, "password_specialgroup", "Group named " + groupName
									+ ", defined in modules/authentication-password.cfg, does not exist"));
						}
					}

					if (specialGroupID.isEmpty()) {
						// Oops - the group isn't there.
						log.warn(LogManager.getHeader(context, "password_specialgroup",
								"Groups defined in modules/authentication-password.cfg does not exist"));
						return ListUtils.EMPTY_LIST;
					} else {
						return specialGroupID;
					}
				}
			}
		} catch (Exception e) {
			// The user is not a password user, so we don't need to worry about
			// them
		}
		return ListUtils.EMPTY_LIST;
	}

}
