/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubscribeServiceImpl implements SubscribeService {
	/** log4j logger */
	private Logger log = Logger.getLogger(SubscribeServiceImpl.class);

	@Autowired(required = true)
	protected SubscriptionDAO subscriptionDAO;

	@Autowired(required = true)
	protected AuthorizeService authorizeService;
	@Autowired(required = true)
	protected CollectionService collectionService;

	protected SubscribeServiceImpl() {

	}

	@Override
	public List<Subscription> findAll(Context context) throws SQLException {
		return subscriptionDAO.findAllOrderedByEPerson(context);
	}

	@Override
	public void subscribe(Context context, EPerson eperson, Collection collection)
			throws SQLException, AuthorizeException {
		// Check authorisation. Must be administrator, or the eperson.
		if (authorizeService.isAdmin(context)
				|| ((context.getCurrentUser() != null) && (context.getCurrentUser().getID().equals(eperson.getID())))) {
			if (!isSubscribed(context, eperson, collection)) {
				Subscription subscription = subscriptionDAO.create(context, new Subscription());
				subscription.setCollection(collection);
				subscription.setePerson(eperson);
			}
		} else {
			throw new AuthorizeException("Only admin or e-person themselves can subscribe");
		}
	}

	@Override
	public List<Subscription> getSubscriptions(Context context, EPerson eperson) throws SQLException {
		return subscriptionDAO.findByEPerson(context, eperson);
	}

	@Override
	public List<Collection> getAvailableSubscriptions(Context context) throws SQLException {
		return getAvailableSubscriptions(context, null);
	}

	@Override
	public List<Collection> getAvailableSubscriptions(Context context, EPerson eperson) throws SQLException {
		List<Collection> collections;
		if (eperson != null) {
			context.setCurrentUser(eperson);
		}
		collections = collectionService.findAuthorized(context, null, Constants.ADD);

		return collections;
	}

	@Override
	public boolean isSubscribed(Context context, EPerson eperson, Collection collection) throws SQLException {
		return subscriptionDAO.findByCollectionAndEPerson(context, eperson, collection) != null;
	}

	@Override
	public void deleteByCollection(Context context, Collection collection) throws SQLException {
		subscriptionDAO.deleteByCollection(context, collection);
	}

	@Override
	public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException {
		subscriptionDAO.deleteByEPerson(context, ePerson);
	}

	@Override
	public void deleteByCommunity(Context context, Community community) throws SQLException {
		subscriptionDAO.deleteByCommunity(context, community);
	}

	@Override
	public void unsubscribe(Context context, EPerson eperson, DSpaceObject dso)
			throws SQLException, AuthorizeException {

		boolean collection = false;
		boolean community = false;
		switch (dso.getType()) {
		case Constants.COLLECTION:
			collection = true;
			break;

		case Constants.COMMUNITY:
			community = true;
			break;
		default:
			throw new IllegalArgumentException(
					"Unsubscribe is not appliable to the resource " + dso.getName() + " of type " + dso.getType());
		}

		// Check authorisation. Must be administrator, or the eperson.
		if (authorizeService.isAdmin(context)
				|| ((context.getCurrentUser() != null) && (context.getCurrentUser().getID().equals(eperson.getID())))) {
			if (dso == null) {
				// Unsubscribe from all
				subscriptionDAO.deleteByEPerson(context, eperson);
			} else {
				if (collection) {
					subscriptionDAO.deleteByCollectionAndEPerson(context, (Collection) dso, eperson);
					log.info(LogManager.getHeader(context, "unsubscribe",
							"eperson_id=" + eperson.getID() + ",collection_id=" + dso.getID()));
				} else {
					subscriptionDAO.deleteByCommunityAndEPerson(context, (Community) dso, eperson);
					log.info(LogManager.getHeader(context, "unsubscribe",
							"eperson_id=" + eperson.getID() + ",community_id=" + dso.getID()));

				}
			}
		} else {
			throw new AuthorizeException("Only admin or e-person themselves can unsubscribe");
		}
	}

	@Override
	public void unsubscribeCollection(Context context, EPerson eperson, Collection c)
			throws SQLException, AuthorizeException {
		if (c != null) {
			unsubscribe(context, eperson, c);
		} else {
			// Check authorisation. Must be administrator, or the eperson.
			if (authorizeService.isAdmin(context) || ((context.getCurrentUser() != null)
					&& (context.getCurrentUser().getID().equals(eperson.getID())))) {
				// Unsubscribe from all collections
				subscriptionDAO.deleteByEPersonWithCollection(context, eperson);
			} else {
				throw new AuthorizeException("Only admin or e-person themselves can unsubscribe");
			}
		}
	}

	@Override
	public void unsubscribeCommunity(Context context, EPerson eperson, Community community)
			throws SQLException, AuthorizeException {
		if (community != null) {
			unsubscribe(context, eperson, community);
		} else {
			// Check authorisation. Must be administrator, or the eperson.
			if (authorizeService.isAdmin(context) || ((context.getCurrentUser() != null)
					&& (context.getCurrentUser().getID().equals(eperson.getID())))) {
				// Unsubscribe from all collections
				subscriptionDAO.deleteByEPersonWithCommunity(context, eperson);
			} else {
				throw new AuthorizeException("Only admin or e-person themselves can unsubscribe");
			}
		}
	}
	
	@Override
	public List<Subscription> getSubscriptionsCollection(Context context, EPerson eperson) throws SQLException {
		return subscriptionDAO.findByEPersonWithCollection(context, eperson);
	}
	@Override
	public List<Subscription> getSubscriptionsCommunity(Context context, EPerson eperson) throws SQLException {
		return subscriptionDAO.findByEPersonWithCommunity(context, eperson);
	}

	@Override
	public void subscribeCommunity(Context context, EPerson eperson, Community community) throws SQLException, AuthorizeException {
		// Check authorisation. Must be administrator, or the eperson.
		if (authorizeService.isAdmin(context)
				|| ((context.getCurrentUser() != null) && (context.getCurrentUser().getID().equals(eperson.getID())))) {
			if (!isSubscribedCommunity(context, eperson, community)) {
				Subscription subscription = subscriptionDAO.create(context, new Subscription());
				subscription.setCommunity(community);
				subscription.setePerson(eperson);
			}
		} else {
			throw new AuthorizeException("Only admin or e-person themselves can subscribe");
		}
	}

	@Override
	public boolean isSubscribedCommunity(Context context, EPerson eperson, Community community) throws SQLException {
		return subscriptionDAO.findByCommunityAndEPerson(context, eperson, community) != null;
	}
}