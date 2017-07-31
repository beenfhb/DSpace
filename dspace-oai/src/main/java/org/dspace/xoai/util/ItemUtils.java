/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.util.Base64Utils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Utils;
import org.dspace.xoai.data.DSpaceItem;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import gr.ekt.cerif.features.additional.*;
import gr.ekt.cerif.features.semantics.Class;
import gr.ekt.cerif.features.semantics.ClassScheme;
import gr.ekt.cerif.xml.loader.CerifToXmlDataLoader;
import gr.ekt.cerif.xml.loadingSpecs.LoadingSpecs;
import gr.ekt.cerif.xml.output.CerifToXmlOutputGenerator;
import gr.ekt.cerif.xml.service.XmlCERIFService;
import gr.ekt.cerif.xml.service.XmlCERIFServiceImpl;
import gr.ekt.transformationengine.core.RecordSet;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import gr.ekt.cerif.CerifEntity;
import gr.ekt.cerif.entities.base.Person;
import gr.ekt.cerif.entities.link.FederatedIdentifier_Class;
import gr.ekt.cerif.entities.link.PersonName_Person;
import gr.ekt.cerif.entities.second.FederatedIdentifier;
import gr.ekt.cerif.enumerations.semantics.ClassEnum;
import gr.ekt.cerif.enumerations.semantics.ClassSchemeEnum;
import gr.ekt.cerif.CerifEntity;
import gr.ekt.cerif.entities.base.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class ItemUtils
{
    private static Logger log = LogManager
            .getLogger(ItemUtils.class);
//	private static XmlCERIFService xmlCERIFService = new ClassPathXmlApplicationContext("config/cerif2xml-context.xml").getBean(XmlCERIFService.class);
    

    private static Element getElement(List<Element> list, String name)
    {
        for (Element e : list)
            if (name.equals(e.getName()))
                return e;

        return null;
    }
    public  static Element create(String name)
    {
        Element e = new Element();
        e.setName(name);
        return e;
    }

    public static Element.Field createValue(
            String name, String value)
    {
        Element.Field e = new Element.Field();
        e.setValue(value);
        e.setName(name);
        return e;
    }
    
    
    public static Metadata retrieveMetadata(ACrisObject item) {
        Metadata metadata;
        metadata = new Metadata();
    
        // create dc.title
    	String val;
    	switch(item.getType())
    	{
    	case CrisConstants.RP_TYPE_ID: val = item.getMetadata("fullName"); break;
    	case CrisConstants.OU_TYPE_ID: val = item.getMetadata("name"); break;
    	case CrisConstants.PROJECT_TYPE_ID: val = item.getMetadata("title"); break;
    	case 1005: val = item.getMetadata("journalsname"); break;  //VSTODO: prendi l' id tramite  il nome dell' entità
    	default: val = null;
    	}
    	if(val != null && !val.isEmpty()) {
        	Element schema = getElement(metadata.getElement(),"dc");
            if (schema == null) {
                schema = create("dc");
                metadata.getElement().add(schema);
            }
            Element element = getElement(schema.getElement(),"title");
            if (element == null) {
            	element = create("title");
            	schema.getElement().add(element);
            }
            Element lang = create("none");
            element.getElement().add(lang);
            lang.getField().add(createValue("value",val));   
    	}
    	
    	//create dc.description
    	switch(item.getType())
    	{
    	case CrisConstants.RP_TYPE_ID: val = item.getMetadata("biography"); break;
    	case CrisConstants.OU_TYPE_ID: val = item.getMetadata("description"); break;
    	case CrisConstants.PROJECT_TYPE_ID: val = item.getMetadata("description"); break;
    	case 1005: val = item.getMetadata("note"); break;  //VSTODO: prendi l' id tramite  il nome dell' entità  
    	default: val = null;
    	}
    	if(val != null && !val.isEmpty()) {
        	Element schema = getElement(metadata.getElement(),"dc");
            if (schema == null) {
                schema = create("dc");
                metadata.getElement().add(schema);
            }
            Element element = getElement(schema.getElement(),"description");
            if (element == null) {
            	element = create("title");
            	schema.getElement().add(element);
            }           
            Element lang = create("none");
            element.getElement().add(lang);
            lang.getField().add(createValue("value",val));   
    	}    	

    	//create dc.date
    	switch(item.getType())
    	{
    	//case CrisConstants.RP_TYPE_ID: val = item.getMetadata("biography"); break;
    	case CrisConstants.OU_TYPE_ID: val = item.getMetadata("year"); break;
    	case CrisConstants.PROJECT_TYPE_ID: val = item.getMetadata("startdate"); break;
    	//case 1005: val = item.getMetadata("note"); break;  //VSTODO: prendi l' id tramite  il nome dell' entità  
    	default: val = null;
    	}
    	if(val != null && !val.isEmpty()) {
        	Element schema = getElement(metadata.getElement(),"dc");
            if (schema == null) {
                schema = create("dc");
                metadata.getElement().add(schema);
            }
            Element element = getElement(schema.getElement(),"date");
            if (element == null) {
            	element = create("title");
            	schema.getElement().add(element);
            }           
            Element lang = create("none");
            element.getElement().add(lang);
            lang.getField().add(createValue("value",val));   
    	}       	
    	addRepositoryInfo(metadata);
    	return metadata;
    }
    
    public static Metadata retrieveMetadata (DSpaceObject item) {
        Metadata metadata;
        
        //DSpaceDatabaseItem dspaceItem = new DSpaceDatabaseItem(item);
        
        // read all metadata into Metadata Object
        metadata = new Metadata();
        
        Metadatum[] vals = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (Metadatum val : vals)
        {
            Element valueElem = null;
            Element schema = getElement(metadata.getElement(), val.schema);
            if (schema == null)
            {
                schema = create(val.schema);
                metadata.getElement().add(schema);
            }
            valueElem = schema;

            // Has element.. with XOAI one could have only schema and value
            if (val.element != null && !val.element.equals(""))
            {
                Element element = getElement(schema.getElement(),
                        val.element);
                if (element == null)
                {
                    element = create(val.element);
                    schema.getElement().add(element);
                }
                valueElem = element;

                // Qualified element?
                if (val.qualifier != null && !val.qualifier.equals(""))
                {
                    Element qualifier = getElement(element.getElement(),
                            val.qualifier);
                    if (qualifier == null)
                    {
                        qualifier = create(val.qualifier);
                        element.getElement().add(qualifier);
                    }
                    valueElem = qualifier;
                }
            }

            // Language?
            if (val.language != null && !val.language.equals(""))
            {
                Element language = getElement(valueElem.getElement(),
                        val.language);
                if (language == null)
                {
                    language = create(val.language);
                    valueElem.getElement().add(language);
                }
                valueElem = language;
            }
            else
            {
                Element language = getElement(valueElem.getElement(),
                        "none");
                if (language == null)
                {
                    language = create("none");
                    valueElem.getElement().add(language);
                }
                valueElem = language;
            }

            valueElem.getField().add(createValue("value", val.value));
            if (val.authority != null) {
                valueElem.getField().add(createValue("authority", val.authority));
                if (val.confidence != Choices.CF_NOVALUE)
                    valueElem.getField().add(createValue("confidence", val.confidence + ""));
            }
        }
        // Done! Metadata has been read!

        addRepositoryInfo(metadata);
        
        return metadata;
    }
    
    public static Metadata addRepositoryInfo(Metadata metadata) {
    // Repository Info
    Element repository = create("repository");
    repository.getField().add(
            createValue("name",
                    ConfigurationManager.getProperty("dspace.name")));
    repository.getField().add(
            createValue("mail",
                    ConfigurationManager.getProperty("mail.admin")));
    metadata.getElement().add(repository);
    return metadata;
    }
    
    public static void addItemInfo(Metadata metadata, Item item) {
        // Now adding bitstream info
        Element bundles = create("bundles");
        metadata.getElement().add(bundles);

    	Bundle[] bs;
        try
        {
            bs = item.getBundles();
            for (Bundle b : bs)
            {
                Element bundle = create("bundle");
                bundles.getElement().add(bundle);
                bundle.getField()
                        .add(createValue("name", b.getName()));

                Element bitstreams = create("bitstreams");
                bundle.getElement().add(bitstreams);
                Bitstream[] bits = b.getBitstreams();
                for (Bitstream bit : bits)
                {
                    Element bitstream = create("bitstream");
                    bitstreams.getElement().add(bitstream);
                    String url = "";
                    String bsName = bit.getName();
                    String sid = String.valueOf(bit.getSequenceID());
                    String baseUrl = ConfigurationManager.getProperty("oai",
                            "bitstream.baseUrl");
                    String handle = null;
                    // get handle of parent Item of this bitstream, if there
                    // is one:
                    Bundle[] bn = bit.getBundles();
                    if (bn.length > 0)
                    {
                        Item bi[] = bn[0].getItems();
                        if (bi.length > 0)
                        {
                            handle = bi[0].getHandle();
                        }
                    }
                    if (bsName == null)
                    {
                        String ext[] = bit.getFormat().getExtensions();
                        bsName = "bitstream_" + sid
                                + (ext.length > 0 ? ext[0] : "");
                    }
                    if (handle != null && baseUrl != null)
                    {
                        url = baseUrl + "/bitstream/"
                                + handle + "/"
                                + sid + "/"
                                + URLUtils.encode(bsName);
                    }
                    else
                    {
                        url = URLUtils.encode(bsName);
                    }

                    String cks = bit.getChecksum();
                    String cka = bit.getChecksumAlgorithm();
                    String oname = bit.getSource();
                    String name = bit.getName();
                    String description = bit.getDescription();

                    if (name != null)
                        bitstream.getField().add(
                                createValue("name", name));
                    if (oname != null)
                        bitstream.getField().add(
                                createValue("originalName", name));
                    if (description != null)
                        bitstream.getField().add(
                                createValue("description", description));
                    bitstream.getField().add(
                            createValue("format", bit.getFormat()
                                    .getMIMEType()));
                    bitstream.getField().add(
                            createValue("size", "" + bit.getSize()));
                    bitstream.getField().add(createValue("url", url));
                    bitstream.getField().add(
                            createValue("checksum", cks));
                    bitstream.getField().add(
                            createValue("checksumAlgorithm", cka));
                    bitstream.getField().add(
                            createValue("sid", bit.getSequenceID()
                                    + ""));
                }
            }
        }
        catch (SQLException e1)
        {
            e1.printStackTrace();
        }
        

        // Other info
        Element other = create("others");

        other.getField().add(
                createValue("handle", item.getHandle()));
        other.getField().add(
                createValue("identifier", DSpaceItem.buildIdentifier(item.getHandle())));
        other.getField().add(
                createValue("lastModifyDate", item
                        .getLastModified().toString()));
        metadata.getElement().add(other);

        
        // Licensing info
        Element license = create("license");
        Bundle[] licBundles;
        try
        {
            licBundles = item.getBundles(Constants.LICENSE_BUNDLE_NAME);
            if (licBundles.length > 0)
            {
                Bundle licBundle = licBundles[0];
                Bitstream[] licBits = licBundle.getBitstreams();
                if (licBits.length > 0)
                {
                    Bitstream licBit = licBits[0];
                    InputStream in;
                    try
                    {
                        in = licBit.retrieve();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        Utils.bufferedCopy(in, out);
                        license.getField().add(
                                createValue("bin",
                                        Base64Utils.encode(out.toString())));
                        metadata.getElement().add(license);
                    }
                    catch (AuthorizeException e)
                    {
                        log.warn(e.getMessage(), e);
                    }
                    catch (IOException e)
                    {
                        log.warn(e.getMessage(), e);
                    }
                    catch (SQLException e)
                    {
                        log.warn(e.getMessage(), e);
                    }

                }
            }
        }
        catch (SQLException e1)
        {
            log.warn(e1.getMessage(), e1);
        }

    }
    

//VSTODO: non funziona    
//	@Autowired
//	private static ObjectFactory<CerifToXmlOutputGenerator> cerifToXmlOutputGenerator;
//	
//    @Autowired
//    private static XmlCERIFService service;  
    
//    
//    public static void addItemInfo(Metadata metadata, ACrisObject item) {
//
//		String testCerif;
//    	
//		List<CerifEntity> ents = new ArrayList<CerifEntity>();
//    	
//		Person person = new Person();
//		person.setId((long)1);
//		person.setBirthDate(new Date(100,0,1));
//		
//		PersonName personName = new PersonName("Sabatini","Vincenzo","");
//		personName.setId((long)2);
//		
//		PersonName_Person personName_Person = new PersonName_Person(person, personName, null, null, null);
//		personName_Person.setId((long)3);
//		
//		ClassScheme identifierTypesScheme = new ClassScheme();
//		identifierTypesScheme.setUuid(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid());
//		
//		Class orcidClass = new Class(new Date(90,0,2),new Date(0,0,2),identifierTypesScheme);
//		orcidClass.setUuid(ClassEnum.ORCID.getUuid());
//
//		FederatedIdentifier federatedIdentifier = new FederatedIdentifier((long)2, "MyOrcidId",new Date(109,10,31), null);
//		federatedIdentifier.setId((long)87686);
//
//		FederatedIdentifier_Class federatedIdentifier_Class = new FederatedIdentifier_Class((long)5, federatedIdentifier, orcidClass, null, null, null);
//
//		federatedIdentifier.setFederatedIdentifiers_classes(java.util.Collections.singleton(federatedIdentifier_Class));
//		
//		person.setFederatedIdentifiers(java.util.Collections.singletonList(federatedIdentifier));
//		person.setPersonNames_persons(java.util.Collections.singleton(personName_Person));
//		
////		Project proj = new Project();
//		
//		ents.add(person);	
////		ents.add(personName);
//			
//		
//		
//		//GENERATE CERIF
//	
////		LoadingSpecs specs = new LoadingSpecs();
////		specs.setEntities(ents);		
//		testCerif = xmlCERIFService.makeXMLString(ents); 
////		testCerif=  "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <IShotTheCerif cerif=\"shot\"> yeah "
////            	+ "<yoyo> yes </yoyo> </IShotTheCerif>";
//
//	
//    	
//        Element cerif = create("cerif");       
//        metadata.getElement().add(cerif);
//        cerif.getField().add(
//                createValue("openaire", testCerif));
//        
//
//	}

}
