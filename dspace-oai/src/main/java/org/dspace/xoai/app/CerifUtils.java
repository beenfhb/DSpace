package org.dspace.xoai.app;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.value.OUPointer;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.utils.DSpace;
import org.dspace.xoai.app.CerifMapping.CerifEntityConfig;
import org.dspace.xoai.app.CerifMapping.CerifMappingException;
import org.dspace.xoai.app.CerifMapping.CrisAttributeMapping;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

import gr.ekt.cerif.entities.base.Project;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.util.Anagrafica4View;

public class CerifUtils {


	
	/*
	 * 
	 * Marshalling functions 
	 * 
	 */
    static public Object  constant(ACrisObject item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {
    	return evaluatedCrisTemplate;
    }
	
    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >
    String getSimpleCrisProperty(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {
    	
    	String propName = (String)evaluatedCrisTemplate;
    	
    	Field f;
		try {
			
			f = item.getClass().getDeclaredField(propName);
			f.setAccessible(true);
			return f.get(item).toString();

		} catch ( Exception e) {
			
			try {
				return (String)( ((List<P>)(item.getAnagrafica4view().get(propName))).get(0)).toString();
			} catch(Exception e2) {
				//VSTODO cercare anche dentro i nested?
				return null;
			}
		}

    }
    
    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >    
    List<Map<String,String>> getMapCrisProperty(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {   
    	
    	return null;
    }

    
    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >    
    List<Map<String,String>> getMapCrisPropertyMultiProp(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {   
    	
    	List<String> l = (List<String>)evaluatedCrisTemplate;
    	for(String propName: (List<String>)evaluatedCrisTemplate) {
    		
    		
    	}
    	
    	return null;
    }
    
    
    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >    
    List<String> getListCrisProperty(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {   
    	
    	List<String> l = new ArrayList();
    	String propName = (String)evaluatedCrisTemplate;
    	for(P prop:(List<P>)(item.getAnagrafica4view().get(propName))) {
    		l.add(prop.toString());
    	}
    	
    	return l;
    }    
      
    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >    
    List<Map<String,String>> getPersonNames(ACrisObject item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {

//		cfFirstNames;
//		cfFamilyNames;
//		cfOtherNames;
//		NameTypes;
//		id;    	
    	
    
    	List<Map<String,String>> l = new ArrayList<Map<String,String>>();
    	List<String> propNames =(List<String>)evaluatedCrisTemplate;
    	

    	
    	Map<String,String> tuple;
    	for(String propName: propNames ) {
        	List<String> fullNames = getListCrisProperty(item,propName,crisMap);
        	for(String fullName: fullNames)
        	if(fullName!= null) {
	        	tuple = new HashMap();

	        	String[] splittedFullName = fullName.split(", ");    	
	        	
	        	
	        	tuple.put("cfFamilyName", splittedFullName[0]);
	        	if(splittedFullName.length>1)    	
		        	tuple.put("cfFirstName", splittedFullName[1]);
	        	if(splittedFullName.length>2)
	        		tuple.put("cfOtherName", splittedFullName[2]);
	        	//VSTODO: render NameTypes configurable 
	        	switch(propName) {
		        	case "fullName": tuple.put("NameType", "Presented Name"); break;
		        	case "translatedName": tuple.put("NameType", "Passport Name"); break;
		        	case "variants": tuple.put("NameType", "Short Name"); break;
		        	default: tuple.put("NameType", "Presented Name");
	        	}
	        	l.add(tuple);	
        	}
    	}
    	return l ;  	
    }
    


    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >    
    List<Map<String,String>> getFederatedIdentifiers(ACrisObject item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {
    	
    	List<Map<String,String>> l = new ArrayList<Map<String,String>>();
    	List<String> propNames =(List<String>)evaluatedCrisTemplate;
    	
    	
    	Map<String,String> tuple;
    	for(String propName: propNames ) {
        	List<String> fullNames = getListCrisProperty(item,propName,crisMap);
        	for(String fullName: fullNames)
        	if(fullName!= null) {
	        	tuple = new HashMap();
	        	
	        	tuple.put("id", fullName);
	        	switch(propName) {
		        	case "scopusId": tuple.put("type", "ScopusAuthorID"); break;
		        	case "orcid": tuple.put("type", "ORCID"); break;
		        	case "researcherId": tuple.put("type", "ResearcherID"); break;
	        	}
	        	l.add(tuple);	
        	}
    	}
    	return l ;  	
    }
    
    
    static public
    <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  >    
    List<Integer> getListOrgUnitIds(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item, Object evaluatedCrisTemplate, CrisAttributeMapping crisMap) {   
    	
    	List<Integer> l = new ArrayList();
    	String propName = (String)evaluatedCrisTemplate;
        for (RPProperty property : ((ResearcherPage)item).getDynamicField().getAnagrafica4view()
                .get("dept"))
        {
            OUPointer pointer = (OUPointer) property.getValue();
            Integer i = pointer.getObject().getId();
            l.add(i);
    	}
    	
    	return l;
    }      
  /*******************************************/  
	
    static 
	<P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,	  ATNO extends ATypeNestedObject<NTP>  > 				
	Object marshalCerifData(ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item,  String cerifAttributeName, P prop, CerifEntityConfig cerifEntityConfig) throws CerifMappingException  {
		 Logger log = LogManager.getLogger(XOAI.class);
		//Calcola il template e chiama la funzione marshal
		// la funzione marshal decide se fare il mapping ricorsivo (quindi non tutte sono obligate a farlo)
		// quindi la funzione marshal deve richiamare questa funzione

	
		  String expression;
		  String marshalFunctionName;
		  //VSTODO: gestire 
		  log.info("processing <"+ cerifAttributeName +">" );

		  if(cerifEntityConfig.getCrisMap() != null && cerifEntityConfig.getCrisMap().containsKey(cerifAttributeName)) {
			CrisAttributeMapping crisAttributeMapping = cerifEntityConfig.getCrisMap().get(cerifAttributeName);
			expression = crisAttributeMapping.getCrisTemplate();
			marshalFunctionName = crisAttributeMapping.getMarshal();

			try {
				Object evaluatedCrisTemplate;
				ExpressionParser parser = new SpelExpressionParser();
				StandardEvaluationContext evalContext = new StandardEvaluationContext(item);
				evalContext.setVariable("prop", prop );
				evalContext.setVariable("cris", item);
				evalContext.setVariable("anag", item.getAnagrafica4view());				
				evalContext.setVariable("nested", getNested(item)); 
				Expression spel;
				if(crisAttributeMapping.getIsSpelTemplate()) {
					TemplateParserContext parserContext = new TemplateParserContext();
  					spel = parser.parseExpression(expression,parserContext);					
				}
				else spel = parser.parseExpression(expression);
				
				evaluatedCrisTemplate = spel.getValue(evalContext);						
				if(marshalFunctionName == null)
					return CerifUtils.class.getMethod("getSimpleCrisProperty",ACrisObject.class,Object.class,CrisAttributeMapping.class).invoke(null, item,evaluatedCrisTemplate,crisAttributeMapping);
				else
					return CerifUtils.class.getMethod(marshalFunctionName,ACrisObject.class,Object.class,CrisAttributeMapping.class).invoke(null, item,evaluatedCrisTemplate,crisAttributeMapping);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();//VSTODO: togliere
				throw new CerifMappingException("error invoking marshalFunction " + expression + " of entity configuration "+ cerifEntityConfig.entityType);
			} 
		  } 
		  else return null;
	}	
	
	static ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName(
            "applicationService", ApplicationService.class);	  
	static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>,ATNO extends ATypeNestedObject<NTP>  > 	
	Map<String, Anagrafica4View<NP, NTP>> getNested(ACrisObject<P, TP, NP, NTP, ACNO, ATNO>
			item) {
		

		Map<String,Anagrafica4View<NP, NTP>> l = new HashMap();
        for (ATNO anestedtype : applicationService.getList(item.getClassTypeNested()))
        {
            List<ACNO> anesteds = applicationService.getNestedObjectsByParentIDAndTypoID(item.getId(),
                            anestedtype.getId(), item.getClassNested());
            for (ACNO anested : anesteds)
            {
                l.put(anested.getTypo().getShortName(),anested.getAnagrafica4view());
            }
        }
        return l;
	}		
		

	
}
