/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import com.lyncode.xoai.dataprovider.exceptions.ConfigurationException;
import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

import gr.ekt.cerif.CerifEntity;
import gr.ekt.cerif.entities.base.Person;
import gr.ekt.cerif.entities.second.Language;
import gr.ekt.cerif.features.additional.PersonName;
import gr.ekt.cerif.xml.loadingSpecs.LoadingSpecs;
import gr.ekt.cerif.xml.service.XmlCERIFOptions;
import gr.ekt.cerif.xml.service.XmlCERIFService;
import it.cilea.osd.common.core.SingleTimeStampInfo;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.ProjectNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.exceptions.CompilingException;
import org.dspace.xoai.services.api.cache.XOAICacheService;
import org.dspace.xoai.services.api.cache.XOAIItemCacheService;
import org.dspace.xoai.services.api.cache.XOAILastCompilationCacheService;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.database.CollectionsService;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.DSpaceSolrIndexerException;
import org.dspace.xoai.util.ItemUtils;
import org.elasticsearch.common.recycler.Recycler.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;
import static org.dspace.content.Item.find;
import static org.dspace.xoai.util.ItemUtils.retrieveMetadata;

/**
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class XOAI {
    private static Logger log = LogManager.getLogger(XOAI.class);

    private Context context;
    private boolean optimize;
    private boolean verbose;
    private boolean clean;
	private String[] typesToIndex;

    @Autowired
    private SolrServerResolver solrServerResolver;
    @Autowired
    private XOAIManagerResolver xoaiManagerResolver;
    @Autowired
    private ContextService contextService;
    @Autowired
    private XOAILastCompilationCacheService xoaiLastCompilationCacheService;
    @Autowired
    private XOAICacheService xoaiCacheService;
    @Autowired
    private XOAIItemCacheService xoaiItemCacheService;
    @Autowired
    private CollectionsService collectionsService;
 
	private  XmlCERIFService xmlCERIFService = new ClassPathXmlApplicationContext("META-INF/cerif2xml-context.xml").getBean(XmlCERIFService.class);    


	private ApplicationService applicationService =
	 new DSpace().getServiceManager().getServiceByName(
            "applicationService", ApplicationService.class);

	private boolean debug;


    private static List<String> getFileFormats(Item item) {
        List<String> formats = new ArrayList<String>();
        try {
            for (Bundle b : item.getBundles("ORIGINAL")) {
                for (Bitstream bs : b.getBitstreams()) {
                    if (!formats.contains(bs.getFormat().getMIMEType())) {
                        formats.add(bs.getFormat().getMIMEType());
                    }
                }
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
        return formats;
    }

    public XOAI(Context context, boolean optimize, boolean clean, boolean verbose, String[] typesToIndex, boolean debug) {
        this.context = context;
        this.optimize = optimize;
        this.clean = clean;
        this.verbose = verbose; 
        this.typesToIndex = typesToIndex;
        this.debug = debug;
        context.turnOffAuthorisationSystem();
    }

    public XOAI(Context ctx, boolean hasOption) {
        context = ctx;
        verbose = hasOption;
    }

    private void println(String line) {
        System.out.println(line);
    }

    private boolean mustIndex(String s) {
    	if(typesToIndex == null) return true;
    	for(String t:typesToIndex) if(t.equalsIgnoreCase(s)) return true;    	
    	return false;
    }
    
    public int index() throws DSpaceSolrIndexerException {
        int result = 0;
        try {

            if (clean) {
                clearIndex();
                System.out.println("Using full import.");
                result = this.indexAll();
            } else {
                SolrQuery solrParams = new SolrQuery("*:*")
                        .addField("item.lastmodified")
                        .addSortField("item.lastmodified", ORDER.desc).setRows(1);

                SolrDocumentList results = DSpaceSolrSearch.query(solrServerResolver.getServer(), solrParams);
                if (results.getNumFound() == 0) {
                    System.out.println("There are no indexed documents, using full import.");
                    result = this.indexAll();
                } else
                    result = this.index((Date) results.get(0).getFieldValue("item.lastmodified"));

            }
            solrServerResolver.getServer().commit();


            if (optimize) {
                println("Optimizing Index");
                solrServerResolver.getServer().optimize();
                println("Index optimized");
            }

            // Set last compilation date
            xoaiLastCompilationCacheService.put(new Date());
            return result;
        } catch (DSpaceSolrException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private int index(Date last) throws DSpaceSolrIndexerException {
    	int itemIndexed = 0;
        System.out
                .println("Incremental import. Searching for documents modified after: "
                        + last.toString());
        // Index both in_archive items AND withdrawn items. Withdrawn items will be flagged withdrawn
        // (in order to notify external OAI harvesters of their new status)
        String sqlQuery = "SELECT item_id FROM item WHERE (in_archive=TRUE OR withdrawn=TRUE) AND discoverable=TRUE AND last_modified > ?";
        if(DatabaseManager.isOracle()){
                sqlQuery = "SELECT item_id FROM item WHERE (in_archive=1 OR withdrawn=1) AND discoverable=1 AND last_modified > ?";
        }

        try {
            TableRowIterator iterator = DatabaseManager
                    .query(context,
                            sqlQuery,
                            new java.sql.Timestamp(last.getTime()));
            itemIndexed = this.index(iterator);
        	itemIndexed += indexCris(last);
        	return itemIndexed;            
        } catch (SQLException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

        
	private CerifMapping cerifMapping;
	
    private int indexAll() throws DSpaceSolrIndexerException {
    	int itemIndexed = 0;
        System.out.println("Full import");
        try {
            // Index both in_archive items AND withdrawn items. Withdrawn items will be flagged withdrawn
            // (in order to notify external OAI harvesters of their new status)
        	
        	if(mustIndex("item") || mustIndex("itemCerif")) {
	            String sqlQuery = "SELECT item_id FROM item WHERE (in_archive=TRUE OR withdrawn=TRUE) AND discoverable=TRUE";
	            if(DatabaseManager.isOracle()){
	                sqlQuery = "SELECT item_id FROM item WHERE (in_archive=1 OR withdrawn=1) AND discoverable=1";
	            }
	
	            TableRowIterator iterator = DatabaseManager.query(context, sqlQuery);
	            itemIndexed += this.index(iterator);
        	}
        	itemIndexed += indexCris(null);
        	return itemIndexed;
	        } 
        	catch (SQLException ex) {
	            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
	        }
          	catch (Exception ex) {
        	  throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
      }            
    }
      
    
    private int indexCris(Date last) throws DSpaceSolrIndexerException {
    	int itemIndexed = 0;
    	try {
			cerifMapping =  CerifMapping.readConfiguration(ConfigurationManager.getProperty("dspace.dir")+"/config/cerifConfig.xml");
       
        
    	if(mustIndex("rp"))
    		itemIndexed += createCrisIndex(context, ResearcherPage.class,last);
        
        if(mustIndex("pj"))
        	itemIndexed += createCrisIndex(context, Project.class,last);
        
        if(mustIndex("ou"))
        	itemIndexed += createCrisIndex(context, OrganizationUnit.class,last);
        
        if(mustIndex("do"))
        	itemIndexed += createCrisIndex(context, ResearchObject.class,last);
        
        return itemIndexed;          
		} catch (Exception ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		}        

    }
    
    
    private <T extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, 
    TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
    ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> 
    int createCrisIndex(
            Context context, Class<T> classCrisObject, Date last)
    {
    	
   	long tot = applicationService.count(classCrisObject);
    	long n = 0;
    	final int MAX_RESULT = 10;
    	long numpages = (tot / MAX_RESULT) + 1;
    	for (int page = 1; page <= numpages; page++)
        {
			List<T> rpObjects = applicationService.getPaginateList(
					classCrisObject, "id", false, page, MAX_RESULT);
	
	        if (rpObjects != null)
	        {
	            for ( ACrisObject<P, TP, NP, NTP, ACNO, ATNO> cris : rpObjects)
	            {
	            	Date time = cris.getTimeStampInfo().getLastModificationTime();
	                if(last==null || time.after(last)){        	
	                	indexCrisObject(cris, true);
	                	n++;
	                	if(debug && n>=DEBUG_MAXITEM) return (int)n;	            
	                	}
	            }
	        }
        }
        // VSTODO contare in modo corretto i cris indicizzati (escludere errori)
        return (int) n;
    }
    
    private <T extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, 
    TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
    ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> 
    void indexCrisObject(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> cris, boolean force) {
    	
    	try {
            SolrServer server = solrServerResolver.getServer();
            List<SolrInputDocument> listSolrInputDocument = this.index(cris);
            for(SolrInputDocument doc: listSolrInputDocument)   
            	server.add(doc);
            server.commit(); 
    	} catch(Exception e)  {
    		log.error(e.getMessage(), e);
    	}
    	
//    	// indexing nested
//        for (ATNO anestedtype : getApplicationService().getList(
//                cris.getClassTypeNested()))
//        {
//            List<ACNO> anesteds = getApplicationService()
//                    .getNestedObjectsByParentIDAndTypoID(cris.getId(),
//                            anestedtype.getId(), cris.getClassNested());
//            for (ACNO anested : anesteds)
//            {
//                indexNestedObject(anested, true);
//            }
//        }
    }
    
    
        
    private int index(int itemId) throws DSpaceSolrIndexerException {
        try {
            int i = 0;
            int n = 0;
            SolrServer server = solrServerResolver.getServer();

                try {
                	Item item = find(context, itemId);
                    server.add(this.index(item));

//                    SolrInputDocument sol = createCerifPubblication(item);
//                    if(sol!=null) {
//                    	server.add(sol);
//                    	n++;
//                    }                    

                    i++;
                    context.clearCache();
                } catch (SQLException ex) {
                    log.error(ex.getMessage(), ex);
                } catch (MetadataBindException e) {
                    log.error(e.getMessage(), e);
                } catch (ParseException e) {
                    log.error(e.getMessage(), e);
                } catch (XMLStreamException e) {
                    log.error(e.getMessage(), e);
                } catch (WritingXmlException e) {
                    log.error(e.getMessage(), e);
                }

            System.out.println("Total: " + i + " items");
            server.commit();
            return i;
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    	
    	
    }

    private int index(TableRowIterator iterator)
            throws DSpaceSolrIndexerException {
        try {
            int i = 0;
            int n = 0;
            SolrServer server = solrServerResolver.getServer();
            while (iterator.hasNext()) {
                try {
                	Item item = find(context, iterator.next().getIntColumn("item_id"));
                	log.info("indexing item: " + item.getHandle() + " id" + item.getID() + "  " + item.toString());
                	
                	if(mustIndex("item")) {
                    	SolrInputDocument itemDoc = this.index(item);
                    	if(itemDoc != null) {server.add(itemDoc); i++;}                		
                	}
                	
//                	if(mustIndex("itemCerif")) {
//	                    SolrInputDocument sol = createCerifPubblication(item);
//	                    if(sol!=null) {	server.add(sol); n++; }
//            		}
                    context.clearCache(); //VSTODO: clearCache metterlo anche quando indicizzo cerif?
                } 
                
                catch (SQLException ex) {
                    log.error(ex.getMessage(), ex);
                } catch (MetadataBindException e) {
                    log.error(e.getMessage(), e);
                } catch (ParseException e) {
                    log.error(e.getMessage(), e);
                } catch (XMLStreamException e) {
                    log.error(e.getMessage(), e);
                } catch (WritingXmlException e) {
                    log.error(e.getMessage(), e);
                }

                if(debug && (n>=DEBUG_MAXITEM && i>=DEBUG_MAXITEM)) return i; //VSTODO contare bene gli item indicizzati
                if (i % 100 == 0) 	System.out.println(i + " items imported so far...");       	
            }
            System.out.println("Total: " + i + " items");
            server.commit();
            return i;
        } 
        
        catch (SQLException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }
    

    private SolrInputDocument createCerifPubblication(Item item) {
   	
    	SolrInputDocument doc = null;
    	try {
       	doc = prepareCommonSolrDoc(item);
        CerifEntity  cerifEntity = CerifMapping.BuildCerifEntity.buildCerifPublication(item,context);
        if(cerifEntity ==null) return null;
        
        //per ogni entità in listCerifEntity genera un solrDoc mettendoci un attributo che indica il tipo di entità
        int idProg = 0;
        
        XmlCERIFOptions xmlCERIFOptions = new XmlCERIFOptions();
        xmlCERIFOptions.setIncludeDeclaration(true);
    	
        doc.addField("item.public", item.isArchived());
        doc.addField("item.lastmodified", item.getLastModified());
        
        doc.addField("item.handle", "cerif/"+item.getHandle());
        doc.addField("item.identifier", DSpaceItem.buildIdentifier("cerif/"+item.getHandle()));
        doc.addField("item.cerifEntity","gr.ekt.cerif.entities.result.ResultPublication");
        doc.addField("item.isCerif", true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext context = XmlOutputContext.emptyContext(out, Second);
        Metadata metadata = new Metadata();
      
        String cerifText;

       	cerifText = xmlCERIFService.makeXMLString(java.util.Collections.singletonList(cerifEntity),xmlCERIFOptions); 
        Element cerif = ItemUtils.create("cerif");       
        metadata.getElement().add(cerif);
        cerif.getField().add(
        		ItemUtils.createValue("openaire", cerifText));
        
        metadata.write(context);
        context.getWriter().flush();
        context.getWriter().close();
        doc.addField("item.compile", out.toString());

        if (verbose) {
            println("Item with handle " + item.getHandle() + " cerif indexed");
        }
    	} catch( Exception ex) { //VSTODO specializzare l' eccezione
    		ex.printStackTrace();
    	}
		return doc;
	}

	private SolrInputDocument prepareCommonSolrDoc(DSpaceObject item) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
    	SolrInputDocument doc = new SolrInputDocument();
        doc.addField("item.id", item.getID());

        doc.addField("item.typeid", item.getType());
        doc.addField("item.deleted", item.isWithdrawn() ? "true" : "false");
        
        return doc;
    }

	
    private SolrInputDocument index(Item item) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
        
    	SolrInputDocument doc = prepareCommonSolrDoc(item);
    	
try {    	
    	boolean pub = this.isPublic(item);
        doc.addField("item.public", pub);
        doc.addField("item.handle", item.getHandle());
        doc.addField("item.identifier", DSpaceItem.buildIdentifier(item.getHandle()));
        doc.addField("item.isCerif", false);
        doc.addField("item.cerifEntity","gr.ekt.cerif.entities.result.ResultPublication");
        doc.addField("item.lastmodified", item.getLastModified());
        if (item.getSubmitter() != null) {
            doc.addField("item.submitter", item.getSubmitter().getEmail());
        }

        for (Collection col : item.getCollections())
            doc.addField("item.collections",
                    "col_" + col.getHandle().replace("/", "_"));
        for (Community com : collectionsService.flatParentCommunities(item))
            doc.addField("item.communities",
                    "com_" + com.getHandle().replace("/", "_"));

        for (String f : getFileFormats(item)) {
            doc.addField("metadata.dc.format.mimetype", f);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext xmlContext = XmlOutputContext.emptyContext(out, Second); 
        Metadata metadata = retrieveMetadata(item);
        ItemUtils.addItemInfo(metadata, item);
        
            CerifEntity  cerifEntity = CerifMapping.BuildCerifEntity.buildCerifPublication(item,context);

            if(cerifEntity !=null) {
                XmlCERIFOptions xmlCERIFOptions = new XmlCERIFOptions();
                xmlCERIFOptions.setIncludeDeclaration(true);   
                String cerifText;

               	cerifText = xmlCERIFService.makeXMLString(java.util.Collections.singletonList(cerifEntity),xmlCERIFOptions); 
                Element cerif = ItemUtils.create("cerif");       
                metadata.getElement().add(cerif);
                cerif.getField().add(
                		ItemUtils.createValue("openaire", cerifText));                
            };
        	
        
        metadata.write(xmlContext);
        xmlContext.getWriter().flush();
        xmlContext.getWriter().close();
        doc.addField("item.compile", out.toString());

        if (verbose) {
            println("Item with handle " + item.getHandle() + " indexed");
        }
}
catch(Exception ex) {
	log.warn("ERRORE DURANTE INDICIZZAZIONE ITEM " + item.getHandle().toString());
	ex.printStackTrace();
	return null;
}


        return doc;
    }
	
    private SolrInputDocument index_old(Item item) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
        
    	SolrInputDocument doc = prepareCommonSolrDoc(item);
    	
try {    	
    	boolean pub = this.isPublic(item);
        doc.addField("item.public", pub);
        doc.addField("item.handle", item.getHandle());
        doc.addField("item.identifier", DSpaceItem.buildIdentifier(item.getHandle()));
        doc.addField("item.isCerif", false);

        doc.addField("item.lastmodified", item.getLastModified());
        if (item.getSubmitter() != null) {
            doc.addField("item.submitter", item.getSubmitter().getEmail());
        }

        for (Collection col : item.getCollections())
            doc.addField("item.collections",
                    "col_" + col.getHandle().replace("/", "_"));
        for (Community com : collectionsService.flatParentCommunities(item))
            doc.addField("item.communities",
                    "com_" + com.getHandle().replace("/", "_"));

        for (String f : getFileFormats(item)) {
            doc.addField("metadata.dc.format.mimetype", f);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext context = XmlOutputContext.emptyContext(out, Second); 
        Metadata metadata = retrieveMetadata(item);
        ItemUtils.addItemInfo(metadata, item);
        metadata.write(context);
        context.getWriter().flush();
        context.getWriter().close();
        doc.addField("item.compile", out.toString());

        if (verbose) {
            println("Item with handle " + item.getHandle() + " indexed");
        }
}
catch(Exception ex) {
	log.warn("ERRORE DURANTE INDICIZZAZIONE ITEM " + item.getHandle().toString());
	ex.printStackTrace();
	return null;
}


        return doc;
    }


	private List<SolrInputDocument> index(ACrisObject item) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
    	//VSTODO refactor with createCerifPubblications
    	List<SolrInputDocument> docs= new ArrayList();
    	
    	// genera tutte le entità cerif derivabili dal cris item
    	// prende dalla configurazione di mapping la lista dell' entità cerif in base al tipo (typo) di cris
    	// per ogni entità da generare costruisce gli oggetti con le informazioni necessarie (contextEval) alla costruzione dell' entità cerif
    	//  caso 1) contextEval è tutto il cris item
    	//  caso 2) sempre tutto il cris item?? se mi servono cris item linkati li posso accedere con una expression? SI
   	
    	println("Indexing crisId " + item.getCrisID() +" Id= " + item.getID() +" Class " + item.getClass().toString().substring(6) + " Name: " + item.getName());
    	
    	try {
 	
        List<CerifEntity>  listCerifEntity = cerifMapping.generateEntities(item,context);
        
        //per ogni entità in listCerifEntity genera un solrDoc mettendoci un attributo che indica il tipo di entità
        int idProg = 0;
        
        XmlCERIFOptions xmlCERIFOptions = new XmlCERIFOptions();
        xmlCERIFOptions.setIncludeDeclaration(true);
        if(listCerifEntity!=null)       
        for(CerifEntity cerifEntity: listCerifEntity) {
        	if(cerifEntity!=null) {
        	SolrInputDocument doc = prepareCommonSolrDoc(item);
        	
            doc.addField("item.public", item.isArchived());    
            doc.addField("item.lastmodified", item.getTimeStampInfo().getLastModificationTime());
            
            doc.addField("item.cerifEntity",cerifEntity.getClass().toString().substring(6));
            doc.addField("item.isCerif", true);

            long id = getId(cerifEntity); 
            String identifier;
            if(id!=0) {
            	identifier = item.getHandle() + "." + Long.toString(id);  
            }
            else identifier = item.getHandle(); 
            
              
            doc.addField("item.handle", item.getHandle());                  
            doc.addField("item.identifier", DSpaceItem.buildIdentifier(identifier));                 
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XmlOutputContext context = XmlOutputContext.emptyContext(out, Second);
            Metadata metadata = retrieveMetadata(item);
            //ItemUtils.addItemInfo(metadata, item);
          
            String cerifText;

            if(cerifEntity != null)
    		cerifText = xmlCERIFService.makeXMLString(java.util.Collections.singletonList(cerifEntity),xmlCERIFOptions); 
            else return null;

            //VSTODO: spostare in retrieveMetadata
            Element cerif = ItemUtils.create("cerif");       
            metadata.getElement().add(cerif);
            cerif.getField().add(
            		ItemUtils.createValue("openaire", cerifText));
            
            metadata.write(context);
            context.getWriter().flush();
            context.getWriter().close();
            doc.addField("item.compile", out.toString()); 

            if (verbose) {
                println("Item with handle " + item.getHandle() + " indexed");
            }

            docs.add(doc); 
            
        }
    	}
   
    	} catch( Exception ex) { //VSTODO specializzare l' eccezione
    		ex.printStackTrace();
    	}

        return docs;
    }
    
	
	
	//get the internal id of the cerifEntity (the one that is exposed in the metadata)
    private long getId(CerifEntity cerifEntity) {

    	Field f;
    	long id;
		try {
			f = cerifEntity.getClass().getDeclaredField("id");
			f.setAccessible(true);
			id=(long)(f.get(cerifEntity));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			return 0;
		}

    	return id;
		
	}

	private boolean isPublic(Item item) {
        boolean pub = false;
        try {
            //Check if READ access allowed on this Item
            pub = AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
        return pub;
    }


    private static boolean getKnownExplanation(Throwable t) {
        if (t instanceof ConnectException) {
            System.err.println("Solr server ("
                    + ConfigurationManager.getProperty("oai", "solr.url")
                    + ") is down, turn it on.");
            return true;
        }

        return false;
    }

    private static boolean searchForReason(Throwable t) {
        if (getKnownExplanation(t))
            return true;
        if (t.getCause() != null)
            return searchForReason(t.getCause());
        return false;
    }

    private void clearIndex() throws DSpaceSolrIndexerException {
        try {
        	
        	String solrQuery="";
        	if(mustIndex("all")) solrQuery="*:*";
        	else {
        		solrQuery="(-*:*) ";
        		if(mustIndex("rp")) solrQuery += " OR (item.isCerif:true AND item.cerifEntity:\"gr.ekt.cerif.entities.base.Person\") ";
        		if(mustIndex("ou")) solrQuery += " OR (item.isCerif:true AND item.cerifEntity:\"gr.ekt.cerif.entities.base.OrganisationUnit\") ";
        		if(mustIndex("pj")) solrQuery += " OR (item.isCerif:true AND item.cerifEntity:\"gr.ekt.cerif.entities.base.Project\") ";
        		if(mustIndex("itemcerif")) solrQuery +=" OR (item.isCerif:true AND item.cerifEntity:\"gr.ekt.cerif.entities.result.ResultPublication\") ";
        		if(mustIndex("item")) solrQuery +=" OR (item.isCerif:false) ";
        		
        		//VSTODO: prendere da cerifConfig la lista delle entità cerif generate dai dynamic objects
        		if(mustIndex("do")) throw new DSpaceSolrIndexerException("Clearing index of dynamic object not supported");    
        		
        	}
            System.out.println("Clearing index" + solrQuery);
            solrServerResolver.getServer().deleteByQuery(solrQuery);
            solrServerResolver.getServer().commit();
            System.out.println("Index cleared");
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private static void cleanCache(XOAIItemCacheService xoaiItemCacheService,  XOAICacheService xoaiCacheService) throws IOException {
        System.out.println("Purging cached OAI responses.");
        xoaiItemCacheService.deleteAll();
        xoaiCacheService.deleteAll();
    }

    private static final String COMMAND_IMPORT = "import";
    private static final String COMMAND_CLEAN_CACHE = "clean-cache";
    private static final String COMMAND_COMPILE_ITEMS = "compile-items";
    private static final String COMMAND_ERASE_COMPILED_ITEMS = "erase-compiled-items";
    private static final int DEBUG_MAXITEM = 10;
   

    public static void main(String[] argv) throws IOException, ConfigurationException {


        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(new Class[]{
                BasicConfiguration.class
        });

        ConfigurationService configurationService = applicationContext.getBean(ConfigurationService.class);
        XOAICacheService cacheService = applicationContext.getBean(XOAICacheService.class);
        XOAIItemCacheService itemCacheService = applicationContext.getBean(XOAIItemCacheService.class);
        
        
        DSpace dspace = new DSpace();
        
        Context ctx = null;

        try {
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("c", "clear", false, "Clear index before indexing");
            options.addOption("o", "optimize", false,
                    "Optimize index at the end");
            options.addOption("v", "verbose", false, "Verbose output");
            options.addOption("h", "help", false, "Shows some help");
            options.addOption("t", "types", true, "Index only this entities types");
            options.addOption("d", "debug", false, "limit to " + DEBUG_MAXITEM +" entities");
            CommandLine line = parser.parse(options, argv);

            String[] validSolrCommands = {COMMAND_IMPORT, COMMAND_CLEAN_CACHE};
            String[] validDatabaseCommands = {COMMAND_CLEAN_CACHE, COMMAND_COMPILE_ITEMS, COMMAND_ERASE_COMPILED_ITEMS};


            boolean solr = true; // Assuming solr by default
            solr = !("database").equals(configurationService.getProperty("oai", "storage"));


            boolean run = false;
            if (line.getArgs().length > 0) {
                if (solr) {
                    if (Arrays.asList(validSolrCommands).contains(line.getArgs()[0])) {
                        run = true;
                    }
                } else {
                    if (Arrays.asList(validDatabaseCommands).contains(line.getArgs()[0])) {
                        run = true;
                    }
                }
            }

            if (!line.hasOption('h') && run) {
                System.out.println("OAI 2.0 manager action started");
                long start = System.currentTimeMillis();

                String command = line.getArgs()[0];

                if (COMMAND_IMPORT.equals(command)) {
                    ctx = new Context();

                    XOAI indexer = new XOAI(ctx,
                            line.hasOption('o'),
                            line.hasOption('c'),
                            line.hasOption('v'),
                            line.getOptionValues('t'),
                            line.hasOption('d'));

                    applicationContext.getAutowireCapableBeanFactory().autowireBean(indexer);

                    int imported = indexer.index();
                    if (imported > 0) cleanCache(itemCacheService, cacheService);
                } else if (COMMAND_CLEAN_CACHE.equals(command)) {
                    cleanCache(itemCacheService, cacheService);
                } else if (COMMAND_COMPILE_ITEMS.equals(command)) {

                    ctx = new Context();
                    XOAI indexer = new XOAI(ctx, line.hasOption('v'));
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(indexer);

                    indexer.compile();

                    cleanCache(itemCacheService, cacheService);
                } else if (COMMAND_ERASE_COMPILED_ITEMS.equals(command)) {
                    cleanCompiledItems(itemCacheService);
                    cleanCache(itemCacheService, cacheService);
                }

                System.out.println("OAI 2.0 manager action ended. It took "
                        + ((System.currentTimeMillis() - start) / 1000)
                        + " seconds.");
            } else {
                usage();
            }
        } catch (Throwable ex) {
            if (!searchForReason(ex)) {
                ex.printStackTrace();
            }
            log.error(ex.getMessage(), ex);
        }
        finally
        {
            // Abort our context, if still open
            if(ctx!=null && ctx.isValid())
                ctx.abort();
        }
    }

    private static void cleanCompiledItems(XOAIItemCacheService itemCacheService) throws IOException {
        System.out.println("Purging compiled items");
        itemCacheService.deleteAll();
    }

    private void compile() throws CompilingException {
        ItemIterator iterator;
        try {
            Date last = xoaiLastCompilationCacheService.get();

            if (last == null) {
                System.out.println("Retrieving all items to be compiled");
                iterator = Item.findAll(context);
            } else {
                System.out.println("Retrieving items modified after " + last + " to be compiled");
                String query = "SELECT * FROM item WHERE last_modified>?";
                iterator = new ItemIterator(context, DatabaseManager.query(context, query, new java.sql.Date(last.getTime())));
            }

            while (iterator.hasNext()) {
                Item item = iterator.next();
                if (verbose) System.out.println("Compiling item with handle: " + item.getHandle());
                xoaiItemCacheService.put(item, retrieveMetadata(item));
                context.clearCache();
            }

            xoaiLastCompilationCacheService.put(new Date());
        } catch (SQLException e) {
            throw new CompilingException(e);
        } catch (IOException e) {
            throw new CompilingException(e);
        }
        System.out.println("Items compiled");
    }

    private static void usage() {
        boolean solr = true; // Assuming solr by default
        solr = !("database").equals(ConfigurationManager.getProperty("oai", "storage"));

        if (solr) {
            System.out.println("OAI Manager Script");
            System.out.println("Syntax: oai <action> [parameters]");
            System.out.println("> Possible actions:");
            System.out.println("     " + COMMAND_IMPORT + " - To import DSpace items into OAI index and cache system");
            System.out.println("     " + COMMAND_CLEAN_CACHE + " - Cleans the OAI cached responses");
            System.out.println("> Parameters:");
            System.out.println("     -o 	 Optimize index after indexing (" + COMMAND_IMPORT + " only)");
            System.out.println("     -c 	 Clear index (" + COMMAND_IMPORT + " only)");
            System.out.println("	 -t type Index only entity of type \"type\". Can be used multiple times");
            System.out.println("         item = dspace pubblications (not cerif) ");
            System.out.println("         itemcerif = dspace pubblications (cerif) ");
            System.out.println("         rp = researchers");
            System.out.println("         pj = projects");
            System.out.println("         ou = orgunits");
            System.out.println("         do = dinamic objects (other research entities)");
            
            System.out.println("     -v Verbose output");
            System.out.println("     -h Shows this text");
        } else {
            System.out.println("OAI Manager Script");
            System.out.println("Syntax: oai <action> [parameters]");
            System.out.println("> Possible actions:");
            System.out.println("     " + COMMAND_CLEAN_CACHE + " - Cleans the OAI cached responses");
            System.out.println("     " + COMMAND_COMPILE_ITEMS + " - Compiles all DSpace items");
            System.out.println("     " + COMMAND_ERASE_COMPILED_ITEMS + " - Erase the OAI compiled items");
            System.out.println("> Parameters:");
            System.out.println("     -v Verbose output");
            System.out.println("     -h Shows this text");
        }
    }
}
