package com.lyncode.xoai.dataprovider.xml.xoaiconfig.parse;

import com.lyncode.xoai.dataprovider.xml.read.XmlReader;
import com.lyncode.xoai.dataprovider.xml.read.XmlReaderException;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.CustomFormatConfiguration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.FormatConfiguration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.FormatConfigurationSuper;

public class FormatConfigurationParser extends Parser<FormatConfigurationSuper> {
    @Override
    public FormatConfigurationSuper parse(XmlReader reader) throws ParseException {
        try {
//        	FormatConfigurationSuper formatConfiguration;
        	String id= reader.getAttribute("id");
        	String prefix = reader.getNextElementText("Prefix");
        	reader.proceedToNextElement();
        	if (reader.isStart() && reader.getName().equals("XSLT")) {
        		FormatConfiguration formatConfiguration = new FormatConfiguration(id)            
                        .withPrefix(prefix)
                        .withXslt(reader.getText())
                        .withNamespace(reader.getNextElementText("Namespace"))
                        .withSchemaLocation(reader.getNextElementText("SchemaLocation"));
                reader.proceedToNextElement();
                if (reader.isStart() && reader.getName().equals("Filter")) {
                	formatConfiguration.withFilter(reader.getAttribute("ref"));
                    reader.proceedToNextElement();
                    reader.proceedToNextElement();
                }
                reader.proceedToNextElement();
                return formatConfiguration;                
        		
        	}
        	else {
        		CustomFormatConfiguration formatConfiguration = new CustomFormatConfiguration(id)
                        .withPrefix(prefix)
                        .withClazz(reader.getText())
                        .withNamespace(reader.getNextElementText("Namespace"))
                        .withSchemaLocation(reader.getNextElementText("SchemaLocation"));;
                reader.proceedToNextElement();
                
                //VSTODO mergiare questa parte con il caso di prima
                if (reader.isStart() && reader.getName().equals("Filter")) {
                	formatConfiguration.withFilter(reader.getAttribute("ref"));
                    reader.proceedToNextElement();
                    reader.proceedToNextElement();
                }
                reader.proceedToNextElement();
                return formatConfiguration;                
        		        		
        	}
//            FormatConfigurationSuper formatConfiguration = new FormatConfiguration(reader.getAttribute("id"))            
//                    .withPrefix(prefix)
//                    .withXslt(reader.getNextElementText("XSLT"))
//                    .withNamespace(reader.getNextElementText("Namespace"))
//                    .withSchemaLocation(reader.getNextElementText("SchemaLocation"));        	



        } catch (XmlReaderException e) {
            throw new ParseException(e);
        }
    }
}
