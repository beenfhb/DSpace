package org.dspace.xoai.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.CrisComponentsService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.components.ItemsConfigurerComponent;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.utils.DSpace;
import org.dspace.xoai.util.URLUtils;
import org.eclipse.jetty.util.log.Log;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import gr.ekt.cerif.CerifEntity;
import gr.ekt.cerif.entities.base.OrganisationUnit;
import gr.ekt.cerif.entities.base.Person;
import gr.ekt.cerif.entities.link.FederatedIdentifier_Class;
import gr.ekt.cerif.entities.link.PersonName_Person;
import gr.ekt.cerif.entities.link.person.Person_Class;
import gr.ekt.cerif.entities.link.person.Person_ElectronicAddress;
import gr.ekt.cerif.entities.link.person.Person_OrganisationUnit;
import gr.ekt.cerif.entities.link.person.Person_ResultPublication;
import gr.ekt.cerif.entities.link.result.ResultPublication_Class;
import gr.ekt.cerif.entities.result.ResultPublication;
import gr.ekt.cerif.entities.second.ElectronicAddress;
import gr.ekt.cerif.entities.second.FederatedIdentifier;
import gr.ekt.cerif.entities.second.Language;
import gr.ekt.cerif.enumerations.Gender;
import gr.ekt.cerif.enumerations.semantics.ClassEnum;
import gr.ekt.cerif.enumerations.semantics.ClassSchemeEnum;
import gr.ekt.cerif.features.additional.PersonName;
import gr.ekt.cerif.features.multilingual.OrganisationUnitName;
import gr.ekt.cerif.features.multilingual.ProjectTitle;
import gr.ekt.cerif.features.multilingual.ResultPublicationAbstract;
import gr.ekt.cerif.features.multilingual.ResultPublicationKeyword;
import gr.ekt.cerif.features.multilingual.ResultPublicationSubtitle;
import gr.ekt.cerif.features.multilingual.ResultPublicationTitle;
import gr.ekt.cerif.features.multilingual.Translation;
import gr.ekt.cerif.features.semantics.Class;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

@XmlRootElement



public class CerifMapping {
	
	public static class CerifMappingException extends Exception {

		public CerifMappingException(String string) {
			// VSTODO Auto-generated constructor stub
		}
	}

	static String MAPPEDMETADATA = "mappedMetadata";
	static String DEFAULTPOLICY = "DefaultPolicy";
	
	public static class CrisAttributeMapping {		

		String crisTemplate; //spel expression
		String marshal; 
		boolean mandatory=false;
		boolean isSpelTemplate=true;
		Map<String , CrisAttributeMapping>  crisMap;

		
		public String getCrisTemplate() {
			return crisTemplate;
		}
		public void setCrisTemplate(String crisTemplate) {
			this.crisTemplate = crisTemplate;
		}
		public boolean isMandatory() {
			return mandatory;
		}
		public void setMandatory(boolean mandatory) {
			this.mandatory = mandatory;
		}
		public String getMarshal() {
			return marshal;
		}
		public void setMarshal(String marshal) {
			this.marshal = marshal;
		}
		public Map<String, CrisAttributeMapping> getCrisMap() {
			return crisMap;
		}
		public void setCrisMap(Map<String, CrisAttributeMapping> crisMap) {
			this.crisMap = crisMap;
		}
		public boolean getIsSpelTemplate() {
			return isSpelTemplate;
		}
		public void setIsSpelTemplate(boolean isSpelTemplate) {
			this.isSpelTemplate = isSpelTemplate;
		}

	}

	@XmlType(propOrder={"entityType", "oaiId","indicatorAttribute","crisMap"})
	public static class CerifEntityConfig {
		
		// The type of the entity this configuration generates 
		//Class<? extends CerifEntity> entityType;
		String entityType;
		
		String oaiId;
		
		boolean indexed; 
		
		// spel expression identifying an attribute
		// if this string is not null, an entity of this type must be generated for each repetition of the attribute identified by this expression
		String indicatorAttribute = null;
		
		// Map<cerifAttributeName,  CrisAttributeMapping> 
		Map<String , CrisAttributeMapping> crisMap;
		

		public String getEntityType() {
			return entityType;
		}

		public void setEntityType(String entityType) {
			this.entityType = entityType;
		}

		public String getIndicatorAttribute() {
			return indicatorAttribute;
		}

		public void setIndicatorAttribute(String indicatorAttribute) {
			this.indicatorAttribute = indicatorAttribute;
		}

		public Map<String, CrisAttributeMapping> getCrisMap() {
			return crisMap;
		}

		public void setCrisMap(Map<String, CrisAttributeMapping> crisMap) {
			this.crisMap = crisMap;
		}

		public String getOaiId() {
			return oaiId;
		}

		public void setOaiId(String oaiId) {
			this.oaiId = oaiId;
		}
		
	}
	
	
	private static class ListCerifEntityConfig {
		

		private ArrayList<CerifEntityConfig> listCerifEntityConfig;


		public ListCerifEntityConfig() {
			this.listCerifEntityConfig = new ArrayList<CerifEntityConfig>();
		}
		
		public ArrayList<CerifEntityConfig> getListCerifEntityConfig() {
			return listCerifEntityConfig;
		}

		@XmlElement(name = "EntityConfig")
		public void setListCerifEntityConfig(ArrayList<CerifEntityConfig> listCerifEntityConfig) {
			this.listCerifEntityConfig = listCerifEntityConfig;
		}

		public boolean add(CerifEntityConfig e) {
			
			return listCerifEntityConfig.add(e);
			
		}
		public ArrayList<CerifEntityConfig> getArrayList() {
			return listCerifEntityConfig;
		}
				
	}
	
	private Map<String, ListCerifEntityConfig > crisCerif;
	
	public Map<String, ListCerifEntityConfig> getCrisCerifList() {
		return crisCerif;
	}

	public void setCrisCerifList(Map<String, ListCerifEntityConfig> crisCerifList) {
		this.crisCerif = crisCerifList;
	}

	public static CerifMapping readConfiguration(String filename) throws Exception  {
		   // create JAXB context and initializing Marshaller
		   JAXBContext jaxbContext = JAXBContext.newInstance(CerifMapping.class);

		   Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		   // specify the location and name of xml file to be read
		   File XMLfile = new File(filename);

		   // this will create Java object from the XML file
		   CerifMapping cerifMapping = (CerifMapping) jaxbUnmarshaller.unmarshal(XMLfile);	
		   return cerifMapping;
   
	}
	
	public void writeConfiguration(String filename) throws Exception {
		   // create JAXB context and initializing Marshaller
		   JAXBContext jaxbContext = JAXBContext.newInstance(CerifMapping.class);
		   Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		   // for getting nice formatted output
		   jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		   //specify the location and name of xml file to be created
		   File XMLfile = new File(filename);

		   // Writing to XML file
		   jaxbMarshaller.marshal(this, XMLfile); 
		   // Writing to console
		   jaxbMarshaller.marshal(this, System.out); 		
		
	}

	public 
	<P extends Property<TP>, 
	  TP extends PropertiesDefinition, 
	  NP extends ANestedProperty<NTP>, 
	  NTP extends ANestedPropertiesDefinition, 
	  ACNO extends ACrisNestedObject<NP, NTP, P, TP>, 
	  ATNO extends ATypeNestedObject<NTP>  > 
	List<CerifEntity> generateEntities(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, Context ctx)   throws Exception {
		
		List<CerifEntity> ents = new ArrayList<CerifEntity>();
		
    	String itemType = "";
    	
    	if(item instanceof ResearcherPage) {
    		itemType = "rp";
    	}
    	else if(item instanceof Project ) {
    		itemType = "pj";
    	}
    	else if(item instanceof OrganizationUnit) {
    		itemType = "ou";
    	}
    	else if(item instanceof ResearchObject) {
    		itemType = ((ResearchObject)item).getTypo().getShortName();
    	}
    	
		if(itemType == "") {
			log.warn("unknown type for cris object " + item.getCrisID() + " type: " + item.getType());
			return null;
		}
    	if(!crisCerif.containsKey(itemType)) log.warn("no cerif Entity configured for cris object type: " + itemType ); 
    	else for(CerifEntityConfig cerifEntityConfig: crisCerif.get(itemType).getArrayList()){
    		BuildCerifEntity b = new BuildCerifEntity(cerifEntityConfig,ctx);
    		CerifEntity ce;
			if(cerifEntityConfig.indicatorAttribute == null ) {
				ce = b.getCerifEntity(item, null);
				ents.add(ce);				
			}
			else {
				for(P prop: (List<P>)(getCrisPropertyFromTemplate(item,cerifEntityConfig.indicatorAttribute)) )
				{
					ce = b.getCerifEntity(item,prop);
					/*if(ce != null)*/ ents.add(ce);
				}
			}
			
		}
				
		return ents;
	}
	
	public static class BuildCerifEntity {
		
		private CerifEntityConfig cerifEntityConfig;
        private DSpace dspace;
        private CrisComponentsService compService;
        private Context context ;		

		public static HashMap HashInitFromConfig() {
			String fileName = ConfigurationManager.getProperty("dspace.dir")+"/config/crosswalks/cerif-type-mapping.properties";
			FileReader s= null;
			BufferedReader bs= null;
			String line;
			int lineNumber=0;
			HashMap h = new HashMap();
			try {
				s = new FileReader(fileName);
				bs = new BufferedReader(s);
				while((line=bs.readLine())!=null) {
					lineNumber++;
					line=line.trim();
					if(line.length()>0)
					if(line.charAt(0)!='#') {
						String lineSplit[] = line.split("=");
						if(lineSplit.length == 2) {
							h.put(lineSplit[0].trim(), lineSplit[1].trim());							
						}
						else log.info(" error at line: " + lineNumber + " file: " + fileName  );
					}
				}			
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				if(s!=null)
					try {
						s.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			return h;
		}    	
    	static HashMap mapPubblicationType = HashInitFromConfig();
        
        BuildCerifEntity(CerifEntityConfig cerifEntityConfig, Context ctx) throws SQLException{
			this.cerifEntityConfig=cerifEntityConfig;	
	        dspace = new DSpace();
			compService = dspace.getServiceManager().getServiceByName("rpComponentsService", CrisComponentsService.class);
	        context =ctx;
		}
		
		public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  > 	
		CerifEntity getCerifEntity(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, P prop)   throws Exception {
			
			try {
			String methodName = "buildCerif"+cerifEntityConfig.entityType;
			//Method[] ms = this.getClass().getDeclaredMethods();
			Method m = this.getClass().getDeclaredMethod(methodName, ACrisObject.class, it.cilea.osd.jdyna.model.Property.class);
			m.setAccessible(true);
			
			org.dspace.app.cris.model.jdyna.DynamicProperty dummyProp = new  org.dspace.app.cris.model.jdyna.DynamicProperty();
			return (CerifEntity)(m.invoke(this,item, prop));
			}
			catch(NoSuchMethodException  | IllegalArgumentException | SecurityException e) {
				Log.warn("entity Type unknown: " + cerifEntityConfig.entityType);
				}
			catch(InvocationTargetException e){
				e.printStackTrace();
			}	
			return null;
		}		

		static long idProg = 0; //VSTODO: inizializzare con il max preso da solr
		
		private  <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  > 	
		gr.ekt.cerif.entities.base.Project buildCerifProject(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, P prop)   throws Exception {
			
			if(prop == null ) {
			gr.ekt.cerif.entities.base.Project project = new gr.ekt.cerif.entities.base.Project();
			project.setId(Long.valueOf(gca(item,"cfProjId")));
			ProjectTitle projectTitle = new ProjectTitle();
			projectTitle.setTitle(gca(item,"cfTitle" ));
			
			project.setProjectTitles(java.util.Collections.singleton(projectTitle));
			
			return project;
			}
			else throw new Exception("indicator attribute not supported for Person entity"); //VSTODO: specializzare l' eccezione
		}
		
		private  <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  > 	
		gr.ekt.cerif.entities.base.OrganisationUnit buildCerifOrganisation(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, P prop)   throws Exception {
			
			if(prop == null ) {
			gr.ekt.cerif.entities.base.OrganisationUnit organisationUnit = new gr.ekt.cerif.entities.base.OrganisationUnit();
			organisationUnit.setId(Long.valueOf(gca(item,"cfOrgUnitId")));
			String s = gca(item,"cfAcro");
			if(s!=null)	organisationUnit.setAcronym(s);
			s =  gca(item,"cfName");
			if(s!=null)	 
			organisationUnit.setOrganisationUnitNames(java.util.Collections.singleton(
					new OrganisationUnitName(organisationUnit,new Language("en"),Translation.o,s)));


		
			return organisationUnit;
			}
			else throw new Exception("indicator attribute not supported for OrgUnit entity"); //VSTODO: specializzare l' eccezione
		}
		
		static ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName(
	            "applicationService", ApplicationService.class);		

	    static ResultPublication buildCerifPublication(Item item, Context context) {
			List<Metadatum> metadataList;
			
			Language language = new Language("en");

			//Internal	Identifier	1
			ResultPublication resultPublication = new ResultPublication();
			resultPublication.setId((long)item.getID());
			
			
			String metadata;
			String metadataName;
			//Publication Date	0..1
//			metadataList = item.getMetadata("dc","date","issued",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfResPublDate");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setDate(metadata);
			}			
			
			//Journal/Report Number	0..1		
//			metadataList = item.getMetadata("dc","relation","issue",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfNum");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setNum(metadata);
			}	
			
			//Volume
//			metadataList = item.getMetadata("dc","relation","volume",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfVol");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setVol(metadata);
			}	
			
			//Edition
//			metadataList = item.getMetadata("dc","relation","edition",Item.ANY, Item.ANY);  //
			metadataName = (String)mapPubblicationType.get("cfEdition");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setEdition(metadata);
			}				
					
			//Issue
//			metadataList = item.getMetadata("dc","relation","issue",Item.ANY, Item.ANY);  //
			metadataName = (String)mapPubblicationType.get("cfIssue");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setIssue(metadata);
			}		

			//Page range start	
//			metadataList = item.getMetadata("dc","relation","firstpage",Item.ANY, Item.ANY);  
			metadataName = (String)mapPubblicationType.get("cfStartPage");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setStartPage(metadata);
			}				
			
			//Page range end	
//			metadataList = item.getMetadata("dc","relation","lastpage",Item.ANY, Item.ANY);  //
			metadataName = (String)mapPubblicationType.get("cfEndPage");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			if(metadataList!= null && !metadataList.isEmpty()) { 
				metadata=metadataList.get(0).value;
				resultPublication.setEndPage(metadata);
			}	
		
			
			// one and only one title per oai-pmh guidelines
			//Title	
			ResultPublicationTitle resultPublicationTitle = new ResultPublicationTitle(Translation.o, resultPublication, language, item.getName());
			resultPublication.setResultPublicationTitles(java.util.Collections.singleton(resultPublicationTitle) );

			//SubTitle
//			metadataList = item.getMetadata("dc","title","alternative",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfSubTitle");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			metadata=" ";
			if(metadataList!=null && !metadataList.isEmpty() ) 
			{
				metadata=metadataList.get(0).value;
				ResultPublicationSubtitle  resultPublicationSubtitle = new ResultPublicationSubtitle(Translation.o,resultPublication,language,metadata);			
				resultPublication.setResultPublicationSubtitles(java.util.Collections.singleton(resultPublicationSubtitle));
			}
								
			// one and only one abstract per oai-pmh guidelines
			//Description (Abstract)
//			metadataList = item.getMetadata("dc","description","abstract",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfAbstr");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			metadata=" ";
			if(metadataList!=null && !metadataList.isEmpty() ) metadata=metadataList.get(0).value;
			ResultPublicationAbstract  resultPublicationAbstract = new ResultPublicationAbstract(Translation.o,resultPublication,language,metadata);			
			resultPublication.setResultPublicationAbstracts(java.util.Collections.singleton(resultPublicationAbstract));
						
			//Subject (free text) (keywords) 0..N
//			metadataList = item.getMetadata("dc","subject","keywords",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfKeyw");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			Set<ResultPublicationKeyword>   resultPublicationKeywords = new HashSet();
			for(Metadatum m: metadataList) {
				ResultPublicationKeyword resultPublicationKeyword = new ResultPublicationKeyword(Translation.o,resultPublication,language,m.value); 
				resultPublicationKeywords.add(resultPublicationKeyword);
			}
			resultPublication.setResultPublicationKeywords(resultPublicationKeywords);
			
			//ResultPublication_Class
			Set<ResultPublication_Class> resultPublications_classes = new HashSet();	
			
			//Language			
//			metadataList = item.getMetadata("dc","language","iso",Item.ANY, Item.ANY);	
			metadataName = (String)mapPubblicationType.get("cfResPubl_Class_OPENAIRE_LANGUAGES");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			metadata=" ";
			if(metadataList!=null && !metadataList.isEmpty() ) 
			{
				metadata=metadataList.get(0).value;
				ResultPublication_Class resultPublication_Class = new ResultPublication_Class();
				Class clazz = new Class(ClassSchemeEnum.OPENAIRE_LANGUAGES.getUuid(), metadata);
				resultPublication_Class.setTheClass(clazz);
				resultPublications_classes.add(resultPublication_Class);			
			}
			
			//Publication Type	
			ResultPublication_Class clazzPubType = new ResultPublication_Class();
			Class pubType = null;
//			metadataList = item.getMetadata("dc","type","full",Item.ANY, Item.ANY);
//			Metadatum[] metadati = item.getMetadataByMetadataString2((String)mapPubblicationType.get(MAPPEDMETADATA));
			metadataName = (String)mapPubblicationType.get("cfResPubl_Class_OUTPUT_TYPES");
			Metadatum[] metadati;
			if(metadataName==null) metadati=null;
			else metadati = item.getMetadataByMetadataString2(metadataName);
			if(metadati!=null && metadati.length>0 ) 
			{
				metadata=metadati[0].value;
				String uuid = (String)mapPubblicationType.get(metadata.trim());
				if(uuid != null) {
					pubType = new Class(ClassSchemeEnum.OUTPUT_TYPES.getUuid(),uuid);
					clazzPubType.setTheClass(pubType);
					resultPublications_classes.add(clazzPubType);
				}					
			}	
								
			//Open Access Type
			ResultPublication_Class clazzOpenAccessType = new ResultPublication_Class();		
			int openAccessTypeLevel=-1;
			Class openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.RESTRICTED_ACCESS.getUuid());
			java.util.Date soonestEmbargoDate = new java.util.Date("01/01/2999");
			//License Type
			ResultPublication_Class clazzLicense = new ResultPublication_Class();
			Class licenseType = null;
			
			
			//FederatedIdentifiers		
			List<FederatedIdentifier> setFederatedIdentifiers = new ArrayList();
			FederatedIdentifier f;
			Class clazz;
//			metadataList = item.getMetadata("dc","identifier",Item.ANY,Item.ANY, Item.ANY);			
			metadataName = (String)mapPubblicationType.get("cfFedId");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName+".*"));
			for(Metadatum m: metadataList) {
				FederatedIdentifier federatedIdentifier = new FederatedIdentifier((long)0, m.value, null,null);
				clazz=null;			
				switch( m.qualifier) {
				case "doi": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.DOI.getUuid());break;
//				case "handle": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.HANDLE.getUuid());break;
				case "pmid": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.PMCID.getUuid());break;
				case "isi": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.ISI_NUMBER.getUuid());break;
				case "scopus": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.SCPNUMBER.getUuid());break;
				case "issn": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.ISSN.getUuid());break;
				case "isbn": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.ISBN.getUuid());break;
				case "url": clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.URL.getUuid());break;
				}
				if(clazz!=null) {
					Set<FederatedIdentifier_Class> federatedIdentifiers_classes =new HashSet();
					FederatedIdentifier_Class federatedIdentifiers_class = new FederatedIdentifier_Class();
					federatedIdentifiers_class.setTheClass(clazz);
					federatedIdentifiers_classes.add(federatedIdentifiers_class);
					federatedIdentifier.setFederatedIdentifiers_classes(federatedIdentifiers_classes);	
					setFederatedIdentifiers.add(federatedIdentifier);
				}
			}
			
			//handle
			String handleItem = item.getHandle();
			if(handleItem != null && !handleItem.isEmpty())  {
				FederatedIdentifier federatedIdentifier = new FederatedIdentifier((long)0, "http://hdl.handle.net/"+handleItem, null,null);
				clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.HANDLE.getUuid());
				Set<FederatedIdentifier_Class> federatedIdentifiers_classes =new HashSet();
				FederatedIdentifier_Class federatedIdentifiers_class = new FederatedIdentifier_Class();
				federatedIdentifiers_class.setTheClass(clazz);
				federatedIdentifiers_classes.add(federatedIdentifiers_class);
				federatedIdentifier.setFederatedIdentifiers_classes(federatedIdentifiers_classes);	
				setFederatedIdentifiers.add(federatedIdentifier);			
			}			
			
				//Link a bitstream  				
		    	Bundle[] bs;
//		    	ArrayList<String> url = new ArrayList<String>();
		    	String url = "";
		        try
		        {
		            bs = item.getBundles("ORIGINAL");		            
//		            for (Bundle b : bs)
//		            {
		            if(bs.length>0)
		            {
		            	Bundle b = bs[0];
		            	Bitstream[] bits = b.getBitstreams();		            	
		                for (Bitstream bit : bits)
	                	// Skip internal types
		            	if(bit != null)
	                	if (!bit.getFormat().isInternal()) 		            	
		                {
		            		System.out.println("Bitstream id=" + bit.getID() + " nome= " + bit.getName());
		                    url = "";
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
		                    System.out.println("URL: "+ url);
		    		        if(!url.isEmpty())
		    		        {	
		    					//Link a bitstream esterno 				
		    					FederatedIdentifier federatedIdentifier = new FederatedIdentifier((long)0, url, null,null);
		    					clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.URI.getUuid());
		    					Set<FederatedIdentifier_Class> federatedIdentifiers_classes =new HashSet();
		    					FederatedIdentifier_Class federatedIdentifiers_class = new FederatedIdentifier_Class();
		    					federatedIdentifiers_class.setTheClass(clazz);
		    					federatedIdentifiers_classes.add(federatedIdentifiers_class);
		    					federatedIdentifier.setFederatedIdentifiers_classes(federatedIdentifiers_classes);	
		    					setFederatedIdentifiers.add(federatedIdentifier);					
		    				}			                    
		                    
		                    // recupera openAccess Type
							List<ResourcePolicy> policies = AuthorizeManager.findPoliciesByDSOAndType(context,
							bit, ResourcePolicy.TYPE_INHERITED);		                    
							if(policies.isEmpty()) {
								System.out.println("OPENACCESS: NOPOLICY "); 
							}
							for (ResourcePolicy rpolicy : policies) {
								if (rpolicy.getGroupID() == -1)
									continue;
								if (rpolicy.getStartDate() != null && rpolicy.getStartDate().after(new Date())) {
									openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.EMBARGOED_ACCESS.getUuid());
									openAccessType.setStartDate(rpolicy.getStartDate());	
									
									if(1>=openAccessTypeLevel) openAccessTypeLevel=1;
									if(rpolicy.getStartDate().before(soonestEmbargoDate)) 
									soonestEmbargoDate = rpolicy.getStartDate();
									if (rpolicy.getGroupID() == 0) {
										System.out.println("OPENACCESS: embargo " + rpolicy.getStartDate());
									} else {
										System.out.println("OPENACCESS: embargo gruppo: " + rpolicy.getStartDate() + "gruppo:  " + rpolicy.getGroup().getName());
									}
								} else {
									if (rpolicy.getGroupID() == 0) {
										openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.OPEN_ACCESS.getUuid());
										System.out.println("OPENACCESS: openAccess ");
										if(2>=openAccessTypeLevel) openAccessTypeLevel=2;								

									} else {
										openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.RESTRICTED_ACCESS.getUuid());
										System.out.println("OPENACCESS: ristretto gruppo: " + rpolicy
												.getGroup().getName() ); 
									}
								}
							}		   
							
		                }	
		            }
		        }
		        catch (SQLException e1)
		        {
		            e1.printStackTrace();
		        }
		        
		        switch(openAccessTypeLevel) {
		        case -1: 
		        	if(mapPubblicationType.get("DefaultPolicy")!=null && mapPubblicationType.get("DefaultPolicy").toString().equalsIgnoreCase("open")) 
						{
		        			openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.OPEN_ACCESS.getUuid());
		        			System.out.println("OPENACCESS: policy assigned: openAccess ");
		        			break;
						}
		        	openAccessType = null;
		        case 0: openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.RESTRICTED_ACCESS.getUuid()); 
		        		System.out.println("OPENACCESS: policy assigned: restricted ");

		        		break;
		        case 1: openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.EMBARGOED_ACCESS.getUuid());
		        		openAccessType.setStartDate(soonestEmbargoDate);
		        		System.out.println("OPENACCESS: policy assigned: embargo ");
		        		break;
		        case 2: openAccessType = new Class(ClassSchemeEnum.OPEN_ACCESS_TYPES.getUuid(),ClassEnum.OPEN_ACCESS.getUuid());
		        		System.out.println("OPENACCESS: policy assigned: open ");
		        		break;
		        
		        }
		        
		        if(openAccessType!=null){
		        	clazzOpenAccessType.setTheClass(openAccessType);
		        	resultPublications_classes.add(clazzOpenAccessType);		        	
		        }

		        if(licenseType != null) {
		        	clazzLicense.setTheClass(licenseType);
		        	resultPublications_classes.add(clazzLicense);
		        }
		        
				metadataList = item.getMetadata("dc","identifier","urlfvg",Item.ANY, Item.ANY);
//				if(metadataList!= null && !metadataList.isEmpty()) { 
//					metadata=metadataList.get(0).value;
//				}
//				else if(!url.isEmpty())
//					metadata=url; ////
//					
//		        if(!metadata.isEmpty())
		        	
		        for(Metadatum metadatum: metadataList)
		        {	
		        	metadata = metadatum.value;
			        if(!metadata.isEmpty()) {		        	
					//Link a bitstream esterno 				
					FederatedIdentifier federatedIdentifier = new FederatedIdentifier((long)0, metadata, null,null);
					clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(), ClassEnum.URI.getUuid());
					Set<FederatedIdentifier_Class> federatedIdentifiers_classes =new HashSet();
					FederatedIdentifier_Class federatedIdentifiers_class = new FederatedIdentifier_Class();
					federatedIdentifiers_class.setTheClass(clazz);
					federatedIdentifiers_classes.add(federatedIdentifiers_class);
					federatedIdentifier.setFederatedIdentifiers_classes(federatedIdentifiers_classes);	
					setFederatedIdentifiers.add(federatedIdentifier);
			        }
				}					
	        
			resultPublication.setFederatedIdentifiers(setFederatedIdentifiers);
			
			
			//Person (autore)
//			metadataList = item.getMetadata("dc","contributor","author",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfPers_ResPubl_Author");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			
			Set<Person_ResultPublication> persons_resultPublications=new HashSet();
			int nAuthor = 0;
			for(Metadatum m: metadataList) {
				if(m.authority!=null) {
				int idAuthor = applicationService.getEntityByCrisId(m.authority).getID();
				Person person = new Person();person.setId((long)idAuthor);
				Person_ResultPublication person_ResultPublication = new Person_ResultPublication(person, resultPublication, new Class(ClassSchemeEnum.PERSON_OUTPUT_CONTRIBUTIONS.getUuid(),ClassEnum.AUTHOR.getUuid()), null, null);
				persons_resultPublications.add(person_ResultPublication);	
				nAuthor++;
				}
				else {
					//VSTODO autori esterni modificare tipo return in lista e fare loop dove è usata
					// l.add( buildCerifpersonExternal(m.value)); 
					// l.resultPublication alla fine
				}
			}

			//Person (Editor)
//			metadataList = item.getMetadata("dc","contributor","editor",Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfPers_ResPubl_Editor");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			
			int nEditor = 0;
			for(Metadatum m: metadataList) {
				if(m.authority!=null) {
				int idAuthor = applicationService.getEntityByCrisId(m.authority).getID();
				Person person = new Person();person.setId((long)idAuthor);
				Person_ResultPublication person_ResultPublication = new Person_ResultPublication(person, resultPublication, new Class(ClassSchemeEnum.PERSON_OUTPUT_CONTRIBUTIONS.getUuid(),ClassEnum.EDITOR.getUuid()), null, null);
				persons_resultPublications.add(person_ResultPublication);	
				nEditor++;
				}
				else {					
					//VSTODO autori esterni modificare tipo return in lista e fare loop dove è usata
					// l.add( buildCerifpersonExternal(m.value)); 
					// l.resultPublication alla fine
				}
			}

			//Person (Editor)
			metadataList = item.getMetadata("dc","publisher",null,Item.ANY, Item.ANY);
			metadataName = (String)mapPubblicationType.get("cfPers_ResPubl_Publisher");
			if(metadataName==null) metadataList=null;
			else metadataList = Arrays.asList(item.getMetadataByMetadataString2(metadataName));
			
			
			int nPublisher = 0;
			for(Metadatum m: metadataList) {
				if(m.authority!=null) {
				int idAuthor = applicationService.getEntityByCrisId(m.authority).getID();
				Person person = new Person();person.setId((long)idAuthor);
				Person_ResultPublication person_ResultPublication = new Person_ResultPublication(person, resultPublication, new Class(ClassSchemeEnum.PERSON_OUTPUT_CONTRIBUTIONS.getUuid(),ClassEnum.PUBLISHER.getUuid()), null, null);
				persons_resultPublications.add(person_ResultPublication);	
				nPublisher++;
				}
				else {			
					//VSTODO autori esterni modificare tipo return in lista e fare loop dove è usata
					// l.add( buildCerifpersonExternal(m.value)); 
					// l.resultPublication alla fine
				}
			}			
			
			
			resultPublication.setPersons_resultPublications(persons_resultPublications);
			resultPublication.setResultPublications_classes(resultPublications_classes);
	
			
			return resultPublication;
		}
		
		
		@SuppressWarnings("unchecked")
		private  <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  > 		
		gr.ekt.cerif.entities.base.Person buildCerifPerson(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, P prop)  throws Exception {
				
			if(prop == null) {
			Class clazz;
			Set<Person_Class> persons_classes;
			List<FederatedIdentifier> setFederatedIdentifiers;
			String id = gca(item,"cfPersId");
			if(id == null) return null;
			
			Person person = new Person();
			person.setId(Long.valueOf(id));
			
			List<Map<String,String>> listPersonNameTuple =(List<Map<String,String>>) CerifUtils.marshalCerifData(item,"personNames", null, cerifEntityConfig);
						
			//PersonName mandatory per oah-pmh guidelines
			if(listPersonNameTuple == null || listPersonNameTuple.size()==0) return null;
			
			HashSet<PersonName_Person> setPersonName_Person = new HashSet<PersonName_Person>();
			PersonName personName;
			for(int i=0; i<listPersonNameTuple.size(); i++) {
				personName = new PersonName(listPersonNameTuple.get(i).get("cfFirstName"),listPersonNameTuple.get(i).get("cfFamilyName"),listPersonNameTuple.get(i).get("cfOtherName"));
				//personName.setId(Long.valueOf(listPersonNameTuple.get(i).get("id"))); non serve
				switch(listPersonNameTuple.get(i).get("NameType")) {
				case "Presented Name": clazz = new Class(ClassSchemeEnum.PERSON_NAMES.getUuid(),ClassEnum.PRESENTED_NAME.getUuid());break;
				case "Short Name": clazz = new Class(ClassSchemeEnum.PERSON_NAMES.getUuid(),ClassEnum.SHORT_NAME.getUuid());break;
				case "Passport Name": clazz = new Class(ClassSchemeEnum.PERSON_NAMES.getUuid(),ClassEnum.PASSPORT_NAME.getUuid());break;
				default: clazz = new Class(ClassSchemeEnum.PERSON_NAMES.getUuid(),ClassEnum.PRESENTED_NAME.getUuid());
				}
				setPersonName_Person.add(new PersonName_Person(person, personName, clazz, null, null));
			}
			person.setPersonNames_persons(setPersonName_Person);	
			
			String gender = gca(item,"cfGender");
			if(gender!=null) {
				switch(gender) {
				case "m": person.setGender(Gender.m); break;
				case "f": person.setGender(Gender.f); break;
				default: person.setGender(Gender.u);
				}
			}
			else person.setGender(Gender.u);
						
			
			setFederatedIdentifiers = new ArrayList();
			List<Map<String,String>> federatedIdentifiers =  (List<Map<String,String>>)CerifUtils.marshalCerifData(item,"federatedIdentifiers",null,cerifEntityConfig);
			for(int i=0;i<federatedIdentifiers.size();i++) {
			FederatedIdentifier federatedIdentifier = new FederatedIdentifier(idProg++,federatedIdentifiers.get(i).get("id"),null,null);
			
				if(federatedIdentifiers.get(i).get("type")!=null) {
					clazz = null;
					switch(federatedIdentifiers.get(i).get("type")) {
						case "ORCID": 
							clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(),ClassEnum.ORCID.getUuid()); 
							break;
						case "ResearcherID": 
							clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(),ClassEnum.RESEARCHERID.getUuid());
							break;
						case "ScopusAuthorID":
							clazz = new Class(ClassSchemeEnum.IDENTIFIER_TYPES.getUuid(),ClassEnum.SCOPUSAUTHORID.getUuid());
							break;
						
					}
					if(clazz!=null) {
						FederatedIdentifier_Class federatedIdentifier_Class = new FederatedIdentifier_Class((long)0, federatedIdentifier, clazz, null, null, null);
						federatedIdentifier.setFederatedIdentifiers_classes(java.util.Collections.singleton(federatedIdentifier_Class));
						setFederatedIdentifiers.add(federatedIdentifier);
					}
				}
			}			
			person.setFederatedIdentifiers(setFederatedIdentifiers);
			
			//contact
			//only email per oai-pmh guidelines
			Set<Person_ElectronicAddress> setPerson_ElectronicAddress = new HashSet<Person_ElectronicAddress>();
			List<String> emails =(List<String>) CerifUtils.marshalCerifData(item,"email", null, cerifEntityConfig);
			for(int i=0;i<emails.size();i++) {
				
			//VSTODO: startdate enddate sono obligatori? per OAI-PMH sembra di no---> modificare il template velocity
			Person_ElectronicAddress person_ElectronicAddress = new Person_ElectronicAddress(person,
					new ElectronicAddress(null, emails.get(i)),null,null);
			setPerson_ElectronicAddress.add(person_ElectronicAddress);
			}
			
			person.setPersons_electronicAddresses(setPerson_ElectronicAddress);
			
			Set<Person_OrganisationUnit> setPerson_OrganisationUnit = new HashSet();
						
 			List<Integer> orgs = (List<Integer>) CerifUtils.marshalCerifData(item,"cfPers_OrgUnit", null, cerifEntityConfig);
			if(orgs!=null) {
				for(int i=0; i<orgs.size();i++) {
					OrganisationUnit organisationUnit = new OrganisationUnit();
					organisationUnit.setId(Long.valueOf(orgs.get(i)));
					clazz = new Class(ClassSchemeEnum.PERSON_ORGANISATION_ROLES.getUuid(),ClassEnum.AFFILIATION.getUuid());
					Person_OrganisationUnit person_OrganisationUnit = new Person_OrganisationUnit(person,
							organisationUnit,clazz,null,null);
					setPerson_OrganisationUnit.add(person_OrganisationUnit);
				}
			}
			
			person.setPersons_organisationUnits(setPerson_OrganisationUnit);
			//person.setPersons_classes(persons_classes);
				
			ItemsConfigurerComponent component=(ItemsConfigurerComponent)(compService.getComponents().get("dspaceitemsforcerif"));
			
			
//DiscoverResult res = component.search(context,null,"all",item,0,Integer.MAX_VALUE,"search.resourceid",false,null);
		    DiscoverResult res = component.search(context,null,"all",item,0,Integer.MAX_VALUE,"search.resourceid",false);
//		    search(Context context, String type, ACrisObject cris, int start, int rpp, String orderfield,boolean ascending) throws SearchServiceException
//    	    search(Context context, HttpServletRequest request, String type, ACrisObject cris, int start, int rpp, String orderfield,boolean ascending, List<String> extraFields) throws SearchServiceException			

			Set<Person_ResultPublication> persons_resultPublications = new HashSet();
			for(DSpaceObject i: res.getDspaceObjects()) {
              try {
              	int itemId = i.getID();
//              Item publication = find(context, itemId);
              	ResultPublication resultPublication = new ResultPublication();
              	resultPublication.setId((long)itemId);
              	clazz = new Class(ClassSchemeEnum.PERSON_OUTPUT_CONTRIBUTIONS.getUuid(),ClassEnum.AUTHOR.getUuid());
              	Person_ResultPublication person_ResultPublication = new Person_ResultPublication(person, resultPublication,clazz,null,null);
              	persons_resultPublications.add(person_ResultPublication);
              }  catch (Exception ex) {
                  log.error(ex.getMessage(), ex);
              } 				
				
			};
			person.setPersons_resultPublications(persons_resultPublications );			
			
			return person;
			}
			else throw new Exception("indicator attribute not supported for Person entity"); //VSTODO: specializzare l' eccezione
		

		}	
		
		String gca(ACrisObject item, String cerifName) throws Exception {
			return (String)CerifUtils.marshalCerifData(item, cerifName, null,cerifEntityConfig);
		}
		
		public static String getBitstreamRetriveLink(String  baseUrl, Item item, Bitstream bitstream) {
			String handle = item != null ? item.getHandle() : null;
			String prefix = baseUrl + "/retrieve/";
			if (StringUtils.isNotBlank(handle)) {
				prefix += "handle/" + item.getHandle() + "/";
			}
			try {
				return prefix + bitstream.getID() + "/"
						+ UIUtil.encodeBitstreamName(bitstream.getName(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}		

	}	

	//usata solo per l' attributo indicatore
	Object getCrisPropertyFromTemplate(ACrisObject item, String cerifAttributeTemplate) {
			  

	      ExpressionParser parser = new SpelExpressionParser();
	      StandardEvaluationContext evalContext = new StandardEvaluationContext(item);
	      evalContext.setVariable("cris", item);
	      evalContext.setVariable("anag", item.getAnagrafica4view());  
	      
	      //VSTODO: gestire nested
	      evalContext.setVariable("nested", CerifUtils.getNested(item));
	      
	      Expression spel = parser.parseExpression(cerifAttributeTemplate);
	      return spel.getValue(evalContext);	
	}	
		

	private static Logger log = LogManager.getLogger(XOAI.class);

	public CerifMapping() {
		
	
	}

}
