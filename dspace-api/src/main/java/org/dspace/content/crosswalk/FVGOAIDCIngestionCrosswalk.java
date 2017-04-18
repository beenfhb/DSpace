/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * DIM ingestion crosswalk
 * <p>
 * Processes Dublic Core metadata encased in an oai_dc:dc wrapper
 *
 * @author Riccardo Fazio
 * 
 */
public class FVGOAIDCIngestionCrosswalk
    implements IngestionCrosswalk
{
    private static final Namespace DC_NS = Namespace.getNamespace("http://www.dspace.org/xmlns/dspace/dim");
    private static final Namespace OAI_DC_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    private static final String PREFIX = "whodc";
    
    private static final String RENDER_HANDLE ="handle";
    private static final String RENDER_ISSUED ="issued";
    
    private HashMap<String, String> oaidc2metadata = new HashMap<String, String>();
    
    
    
    private void init() throws IOException{
		String propsFilename = ConfigurationManager.getProperty("crosswalk."+PREFIX+".properties."+PREFIX);
		String parent = ConfigurationManager.getProperty("dspace.dir") +
            File.separator + "config" + File.separator;
        File propsFile = new File(parent, propsFilename);
        Properties qdcProps = new Properties();
        FileInputStream pfs = null;
        try
        {
            pfs = new FileInputStream(propsFile);
            qdcProps.load(pfs);
        }
        finally
        {
            if (pfs != null)
            {
                try
                {
                    pfs.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }
        Enumeration<String> pe = (Enumeration<String>)qdcProps.propertyNames();
        while (pe.hasMoreElements())
        {
            String oaidc = pe.nextElement();
            String whodc = qdcProps.getProperty(oaidc);
            oaidc2metadata.put(oaidc,whodc);
        }
    }
    
	public void ingest(Context context, DSpaceObject dso, List<Element> metadata) throws CrosswalkException,  SQLException, AuthorizeException, IOException {
        Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
        init();
		wrapper.addContent(metadata);
		ingest(context,dso,wrapper);
	}

	public void ingest(Context context, DSpaceObject dso, Element root) throws CrosswalkException,  SQLException, AuthorizeException, IOException {
            
		init();    
		if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("DIMIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }
        
        List<Element> metadata = root.getChildren();
        for (Element element : metadata) {
		// get language - prefer xml:lang, accept lang.
			String lang = element.getAttributeValue("lang", Namespace.XML_NAMESPACE);
			if (lang == null) {
				lang = element.getAttributeValue("lang");
			}
			if( oaidc2metadata.containsKey(element.getName()) ){
				if(StringUtils.isBlank(element.getText()) ){
					continue;
				}
				String md = oaidc2metadata.get(element.getName());
				String render ="";
				if(StringUtils.contains(md, "(")){
					String meta=StringUtils.substringBefore(md, "(");
					render=StringUtils.substringAfter(StringUtils.remove(md, ")" ), "(");
					md=meta;
				}
				String[] mdArray= StringUtils.split(md, "\\.");
				String schema ="";
				String elmnt ="";
				String qlfr = "";
				
				if(mdArray != null && mdArray.length>=2){
					schema= mdArray[0];
					elmnt= mdArray[1];
					qlfr= mdArray.length ==3 ? mdArray[2] : null;
					if(StringUtils.equalsIgnoreCase(render,RENDER_HANDLE) ){
						if(isValidHandle(element.getText())){
							item.addMetadata(schema, elmnt, qlfr, lang, element.getText());							
						}else{
							item.addMetadata("dc", element.getName(), null, lang, element.getText());			
						}
					}else if(StringUtils.equalsIgnoreCase(render,RENDER_ISSUED)){
						if(isDateIssued(element.getText())){
							item.addMetadata(schema, elmnt, qlfr, lang, element.getText());
						}else{
							continue;
						}
					}else{
						item.addMetadata(schema, elmnt, qlfr, lang, element.getText());
					}
				}
			}else{
				item.addMetadata("dc", element.getName(), null, lang, element.getText());
			}
        }
        
	}
	
	private boolean isValidHandle(String uri){
		boolean isValid= false;
		if(StringUtils.containsIgnoreCase(uri, "/handle/") || StringUtils.containsIgnoreCase(uri, "hdl.handle.net/")){
			isValid= true;
		}
		return isValid;
	}
	
	private boolean isDateIssued(String issued){
		boolean isValid= false;
		if(StringUtils.length(issued)<=10 &&
				( issued.matches("\\d{4}-[01]\\d-[0-3]\\d") || issued.matches("\\d{4}-[01]\\d") ||issued.matches("\\d{4}") )){
			isValid= true;
		}
		return isValid;
		
	}
	
}
