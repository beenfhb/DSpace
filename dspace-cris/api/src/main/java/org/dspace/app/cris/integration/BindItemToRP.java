/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.model.CrisPotentialMatch;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.AuthorityDAO;
import org.dspace.content.authority.AuthorityDAOFactory;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.utils.DSpace;
import org.hibernate.Session;

/**
 * Utility class for performing Item to ReseacherPage binding
 * 
 * @author cilea
 * 
 */
public class BindItemToRP
{
    /** the logger */
    private static Logger log = Logger.getLogger(BindItemToRP.class);

    private RelationPreferenceService relationPreferenceService;

    public static int automaticClaim(Context context, ResearcherPage rp)
            throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();

        DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);

        RelationPreferenceService relationPreferenceService = dspace
                .getServiceManager()
                .getServiceByName(
                        "org.dspace.app.cris.service.RelationPreferenceService",
                        RelationPreferenceService.class);

        List<RelationPreference> rejected = new ArrayList<RelationPreference>();
        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                .getConfigurationService().getList())
        {
            if (configuration.getRelationConfiguration().getRelationClass()
                    .equals(Item.class))
            {
                rejected = applicationService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(rp
                                .getUuid(), configuration
                                .getRelationConfiguration().getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }

        EPerson eperson = EPersonServiceFactory.getInstance().getEPersonService().find(context, rp.getEpersonID());
        Iterator<Item> items = ContentServiceFactory.getInstance().getItemService().findBySubmitter(context, eperson);
        List<MetadataField> mfs = metadataFieldWithAuthorityRP(context);
        int count = 0;
        while (items.hasNext())
        {
            Item item = items.next();
            if (rejected == null || !rejected.contains(item.getID()))
            {
                boolean found = false;
                for (MetadataField mf : mfs)
                {
                    String schema = mf.getMetadataSchema().getName();
                    String element = mf.getElement();
                    String qualifier = mf.getQualifier();
                    List<IMetadataValue> values = item.getMetadata(schema, element,
                            qualifier, Item.ANY);
                    ContentServiceFactory.getInstance().getItemService().clearMetadata(context, item, schema, element, qualifier, Item.ANY);

                    for (IMetadataValue val : values)
                    {
                        if (val.getAuthority() == null
                                && val.getValue() != null
                                && StringUtils.containsIgnoreCase(val.getValue(),
                                        eperson.getLastName().trim()))
                        {
                            val.setAuthority(ResearcherPageUtils
                                    .getPersistentIdentifier(rp));
                            val.setConfidence(Choices.CF_ACCEPTED);
                            found = true;
                        }
                        ContentServiceFactory.getInstance().getItemService().addMetadata(context, item, schema, element, qualifier,
                                val.getLanguage(), val.getValue(), val.getAuthority(),
                                val.getConfidence());
                    }
                }
                if (found)
                {
                    count++;
                }
            }
        }
        context.restoreAuthSystemState();
        return count;
    }

    public static int countPotentialMatch(Context context, ResearcherPage rp)
            throws SQLException, AuthorizeException, IOException
    {
        return getPotentialMatch(context, rp).size();
    }

    public static long countPendingMatch(Context context, ResearcherPage rp)
            throws SQLException
    {
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        return dao.countIssuedItemsByAuthorityValueInAuthority(
                RPAuthority.RP_AUTHORITY_NAME,
                ResearcherPageUtils.getPersistentIdentifier(rp));
    }

    public static Set<UUID> getPotentialMatch(Context context,
            ResearcherPage researcher) throws SQLException, AuthorizeException,
            IOException
    {
        Set<UUID> invalidIds = new HashSet<UUID>();
        List<Item> iter = getPendingMatch(context, researcher);
		for (Item item : iter) {
			invalidIds.add(item.getID());
		}

        DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);
        RelationPreferenceService relationPreferenceService = dspace
                .getServiceManager()
                .getServiceByName(
                        "org.dspace.app.cris.service.RelationPreferenceService",
                        RelationPreferenceService.class);

        List<RelationPreference> rejected = new ArrayList<RelationPreference>();
        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                .getConfigurationService().getList())
        {
            if (configuration.getRelationConfiguration().getRelationClass()
                    .equals(Item.class))
            {
                rejected = applicationService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                researcher.getUuid(), configuration
                                        .getRelationConfiguration()
                                        .getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }

        for (RelationPreference relationPreference : rejected)
        {
            invalidIds.add(relationPreference.getItemID());
        }

        IRetrievePotentialMatchPlugin plugin = dspace
                .getSingletonService(IRetrievePotentialMatchPlugin.class);
        Set<UUID> result = plugin.retrieve(context, invalidIds, researcher);

        return result;
    }

    private static List<MetadataField> metadataFieldWithAuthorityRP(
            Context context) throws SQLException
    {
        // find all metadata with authority support
        List<MetadataField> fields = ContentServiceFactory.getInstance().getMetadataFieldService().findAll(context);
        List<MetadataField> fieldsWithAuthoritySupport = new LinkedList<MetadataField>();
        for (MetadataField mf : fields)
        {
            String schema = mf.getMetadataSchema()
                    .getName();
            String mdstring = schema
                    + "."
                    + mf.getElement()
                    + (mf.getQualifier() == null ? "" : "." + mf.getQualifier());
            String choicesPlugin = ConfigurationManager
                    .getProperty("choices.plugin." + mdstring);
            if (choicesPlugin != null)
            {
                choicesPlugin = choicesPlugin.trim();
            }
            if ((RPAuthority.RP_AUTHORITY_NAME.equals(choicesPlugin)))
            {
                fieldsWithAuthoritySupport.add(mf);
            }
        }
        return fieldsWithAuthoritySupport;
    }

    public static List<Item> getPendingMatch(Context context,
            ResearcherPage rp) throws SQLException, AuthorizeException,
            IOException
    {
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        return dao.findIssuedByAuthorityValueInAuthority(
                RPAuthority.RP_AUTHORITY_NAME,
                ResearcherPageUtils.getPersistentIdentifier(rp));
    }

    /**
     * Search potential matches for all the ResearcherPage supplied. The
     * algorithm search for any researcher page and any researcher's name
     * (regardless the visibility attribute) all the items published in DSpace
     * using the Browse System (@link using the Browse System (@link
     * #researcherPotentialMatchLookupBrowserIndex}, if a match is found and
     * there is not an existent authority key for the metadata then the rp
     * identifier of the matching researcher page is used as authority key and a
     * confidence value is attributed as follow:
     * <ul>
     * <li>{@link Choices.CF_UNCERTAIN} if there is only a potential matching
     * researcher page</li>
     * <li>{@link Choices.CF_AMBIGUOUS} if there are more than one potential
     * matching reseacher pages</li>
     * </ul>
     * 
     * @param rps
     *            the list of ResearcherPage
     * @param applicationService
     *            the ApplicationService
     * 
     * @see #researcherPotentialMatchLookupBrowserIndex
     * @see Choices#CF_UNCERTAIN
     * @see Choices#CF_AMBIGUOUS
     * 
     */
    public static void work(List<ResearcherPage> rps,
            RelationPreferenceService relationPreferenceService)
    {
        log.debug("Working...building names list");

        List<NameResearcherPage> names = new ArrayList<NameResearcherPage>();

        Map<String, Set<UUID>> mapInvalids = new HashMap<String, Set<UUID>>();
        for (ResearcherPage researcher : rps)
        {
            Set<UUID> invalidIds = new HashSet<UUID>();

            List<RelationPreference> rejected = new ArrayList<RelationPreference>();
            for (RelationPreferenceConfiguration configuration : relationPreferenceService
                    .getConfigurationService().getList())
            {
                if (configuration.getRelationConfiguration().getRelationClass()
                        .equals(Item.class))
                {
                    rejected = relationPreferenceService
                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                    researcher.getUuid(), configuration
                                            .getRelationConfiguration()
                                            .getRelationName(),
                                    RelationPreference.UNLINKED);
                }
            }

            for (RelationPreference relationPreference : rejected)
            {
                invalidIds.add(relationPreference.getItemID());
            }
            
            mapInvalids.put(researcher.getCrisID(), invalidIds);
        }
        log.debug("...DONE building names list size " + names.size());
        log.debug("Create DSpace context and use browse indexing");
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            List<MetadataField> fieldsWithAuthoritySupport = metadataFieldWithAuthorityRP(context);
            IRetrievePotentialMatchPlugin plugin = new DSpace()
                    .getSingletonService(IRetrievePotentialMatchPlugin.class);

            Map<NameResearcherPage, List<Item>> result = plugin
                    .retrieveGroupByName(context, mapInvalids, rps);
            for (NameResearcherPage tempName : result.keySet())
            {
                if(result.get(tempName).size()>0) {
                    bindItemsToRP(relationPreferenceService, context,
                            fieldsWithAuthoritySupport, tempName,
                            result.get(tempName));
                }
            }

        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    public static void bindItemsToRP(
            RelationPreferenceService relationPreferenceService,
            Context context, ResearcherPage researcher, List<Item> items)
            throws SQLException, BrowseException, AuthorizeException
    {
        String authority = researcher.getCrisID();
        int id = researcher.getId();
        List<NameResearcherPage> names = new LinkedList<NameResearcherPage>();
        Set<UUID> invalidIds = new HashSet<UUID>();

        List<RelationPreference> rejected = new ArrayList<RelationPreference>();
        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                .getConfigurationService().getList())
        {
            if (configuration.getRelationConfiguration().getRelationClass()
                    .equals(Item.class))
            {
                rejected = relationPreferenceService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                researcher.getUuid(), configuration
                                        .getRelationConfiguration()
                                        .getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }

        for (RelationPreference relationPreference : rejected)
        {
            invalidIds.add(relationPreference.getItemID());
        }
        NameResearcherPage name = new NameResearcherPage(
                researcher.getFullName(), authority, id, invalidIds);
        names.add(name);
        RestrictedField field = researcher.getPreferredName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_1 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_1);
        }
        field = researcher.getTranslatedName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_2 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_2);
        }
        for (RestrictedField r : researcher.getVariants())
        {
            if (r != null && r.getValue() != null && !r.getValue().isEmpty())
            {
                NameResearcherPage name_3 = new NameResearcherPage(
                        r.getValue(), authority, id, invalidIds);
                names.add(name_3);
            }
        }

        List<MetadataField> fieldsWithAuthoritySupport = metadataFieldWithAuthorityRP(context);
        for (NameResearcherPage tmpname : names)
        {
            bindItemsToRP(relationPreferenceService, context,
                    fieldsWithAuthoritySupport, tmpname, items);
        }
    }

    private static void bindItemsToRP(
            RelationPreferenceService relationPreferenceService,
            Context context, List<MetadataField> fieldsWithAuthoritySupport,
            NameResearcherPage tempName, List<Item> items) throws BrowseException,
            SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        Map<String, Integer> cacheCount = new HashMap<String, Integer>();
        for (Item item : items)
        {
            if (tempName.getRejectItems() != null
                    && tempName.getRejectItems().contains(item.getID()))
            {
                log.warn("Item has been reject for this authority - itemID "
                        + item.getID());
            }
            else
            {
                boolean modified = false;

                List<IMetadataValue> values = null;
                for (MetadataField md : fieldsWithAuthoritySupport)
                {
                    String schema = md.getMetadataSchema().getName();

                    values = item.getMetadata(schema, md.getElement(),
                            md.getQualifier(), Item.ANY);
                    item.getItemService().clearMetadata(context, item, schema, md.getElement(),
                            md.getQualifier(), Item.ANY);
                    for (IMetadataValue value : values)
                    {

                        int matches = 0;

                        if (value.getAuthority() == null
                                && (value.getValue().equals(tempName.getName()) || value.getValue()
                                        .startsWith(tempName.getName() + ";")))
                        {
                            matches = countNamesMatching(cacheCount,
                                    tempName.getName());
                            item.getItemService().addMetadata(context, item,
                                    value.getSchema(),
                                    value.getElement(),
                                    value.getQualifier(),
                                    value.getLanguage(),
                                    tempName.getName(),
                                    tempName.getPersistentIdentifier(),
                                    matches >= 1 ? Choices.CF_AMBIGUOUS
                                            : matches == 1 ? Choices.CF_UNCERTAIN
                                                    : Choices.CF_NOTFOUND);
                            modified = true;
                        }
                        else
                        {
                        	item.getItemService().addMetadata(context, item, value.getSchema(), value.getElement(),
                                    value.getQualifier(), value.getLanguage(),
                                    value.getValue(), value.getAuthority(),
                                    value.getConfidence());
                        }
                    }
                    values = null;
                }
                if (modified)
                {
                    log.debug("Update item with id " + item.getID());
                }
                context.commit();
            }
        }
        context.restoreAuthSystemState();
    }

    private static int countNamesMatching(Map<String, Integer> cacheCount,
            String name)
    {
        if (cacheCount.containsKey(name))
        {
            return cacheCount.get(name);
        }
        ChoiceAuthority ca = (ChoiceAuthority) ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService().getChoiceAuthority(RPAuthority.RP_AUTHORITY_NAME);
        Choices choices = ca.getBestMatch(null, name, null, null);
        cacheCount.put(name, choices.total);
        return choices.total;
    }

    private static void generatePotentialMatches(Context context,
            ResearcherPage researcher) throws SQLException, AuthorizeException,
            IOException
    {
    	
    	DSpace dspace = new DSpace();
    	ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);
    	
        Set<UUID> ids = getPotentialMatch(context, researcher);
        String crisID = researcher.getCrisID();
		deletePotentialMatch(context, crisID);
        for (UUID id : ids)
        {	
        	CrisPotentialMatch cpm = new CrisPotentialMatch();
        	cpm.setItem_id(id);
        	cpm.setRp(researcher.getCrisID());
        	applicationService.saveOrUpdate(CrisPotentialMatch.class, cpm);
        }
        context.commit();
    }

	public static void deletePotentialMatch(Context context, String crisID) throws SQLException {
    	getHibernateSession(context).createSQLQuery("DELETE FROM cris_deduplication WHERE rp like :crisID AND pending IS NULL").setParameter("crisID", crisID).executeUpdate();
	}
	
	
    public static void generatePotentialMatches(
            ApplicationService applicationService, Context context, String rp)
            throws SQLException, AuthorizeException, IOException
    {
        ResearcherPage researcher = applicationService
                .getResearcherByAuthorityKey(rp);
        if (researcher == null)
        {
            return;
        }

        generatePotentialMatches(context, researcher);
    }

    public static void generatePotentialMatches(ResearcherPage researcher)
    {
        Context context = null;
        try
        {
            context = new Context();
            generatePotentialMatches(context, researcher);
            context.complete();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
                context.abort();
        }

    }
    
	public static Session getHibernateSession(Context context) throws SQLException {
		return ((Session) context.getDBConnection().getSession());
	}
}
