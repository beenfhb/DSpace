package org.dspace.harvest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;


public class HarvestUtil {

	private static Logger log = Logger.getLogger(HarvestUtil.class);
	 
	public static List<String> getEternalResource(Context context, InputStream is){
    	Namespace ATOM_NS = Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    	Namespace ORE_ATOM = Namespace.getNamespace("oreatom", "http://www.openarchives.org/ore/atom/");
    	Namespace RDF_NS = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    	Namespace DCTERMS_NS = Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
		List<String> ret = new ArrayList<>();
	
		Document doc = null;
		try {
    		SAXBuilder sb=new SAXBuilder();
    	    doc = sb.build(is);
		} catch (Exception e){
			log.error(e.getMessage(), e);
		} 
		if(doc == null){
			return ret;
		}

        XPath xpathLinks;
        List<Element> externalResources = null;
		try {
			xpathLinks = XPath.newInstance("/atom:entry/oreatom:triples/rdf:Description/dcterms:description[text()='ORIGINAL']");
			xpathLinks.addNamespace(ATOM_NS);
			xpathLinks.addNamespace(ORE_ATOM);
			xpathLinks.addNamespace(RDF_NS);
			xpathLinks.addNamespace(DCTERMS_NS);
			externalResources = xpathLinks.selectNodes(doc);
		} catch (JDOMException e) {
			log.error(e.getMessage(), e);
		}
		if(externalResources == null){
			return ret;
		}
		log.info("External resources found: " + externalResources.size());
        for (Element resource : externalResources) {
        	String href = resource.getParentElement().getAttributeValue("about", RDF_NS);
        	if (href == null){
        		continue;
        	}
        	log.debug("ORE processing: " + href);
        	try {
        		ret.add(href);
        	} catch (Exception e) {
        		log.error(e.getMessage(), e);
        	}
        }
        return ret;
	}
}
