/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.lang.ArrayUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.DateDisplayStrategy;
import org.dspace.app.webui.util.DefaultDisplayStrategy;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.LinkDisplayStrategy;
import org.dspace.app.webui.util.ResolverDisplayStrategy;
import org.dspace.app.webui.util.ThumbDisplayStrategy;
import org.dspace.app.webui.util.TitleDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.CrossLinks;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Thumbnail;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortOption;

/**
 * Tag for display a list of items
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class ItemListTag extends TagSupport {
    private static final Logger log = Logger.getLogger(ItemListTag.class);

    /** Items to display */
    private List<Item> items;

    /** Row to highlight, -1 for no row */
    private int highlightRow = -1;

    /** Column to emphasise - null, "title" or "date" */
    private String emphColumn;

    /** Config value of thumbnail view toggle */
    private static boolean showThumbs;

    /** Config browse/search width and height */
    private static int thumbItemListMaxWidth;
    
	/**
	 * Specify if the user can select one or more items (checkbox or radio
	 * button). The html input element is included only if the inputName
	 * attribute is used
	 */
	private boolean radioButton = false;

	/**
	 * The name of the checkbox/radio html input to include in any row for
	 * select the item
	 */
	private String inputName;

    /** Config to include an edit link */
    private boolean linkToEdit = false;

    /** Config to disable cross links */
    private boolean disableCrossLinks = false;

    /** The default fields to be displayed when listing items */
    private static final String[] DEFAULT_LIST_FIELDS;

    /** The default widths for the columns */
    private static final String[] DEFAULT_LIST_WIDTHS;

    /** The default field which is bound to the browse by date */
    private static String dateField = "dc.date.issued";

    /** The default field which is bound to the browse by title */
    private static String titleField = "dc.title";

    private static String authorField = "dc.contributor.*";

    private int authorLimit = -1;
    
	/**
	 * regex pattern to capture the style of a field, ie
	 * <code>schema.element.qualifier(style)</code>
	 */
    private Pattern fieldStylePatter = Pattern.compile(".*\\((.*)\\)");    

    private transient SortOption sortOption = null;
    
    private final transient ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();

	private boolean isDesc = false;

	private static int itemStart = 1;

    private static final long serialVersionUID = 348762897199116432L;
    
    private final transient ConfigurationService configurationService
             = DSpaceServicesFactory.getInstance().getConfigurationService();

    protected PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    
    /** Config to use a specific configuration */
    private String config = null;

    static
    {

        showThumbs = DSpaceServicesFactory.getInstance().getConfigurationService().
                .getBooleanProperty("webui.browse.thumbnail.show");
        
        thumbItemListMaxWidth = DSpaceServicesFactory.getInstance().getConfigurationService().
                .getIntProperty("webui.browse.thumbnail.maxwidth");
        
        if (showThumbs)
        {
            DEFAULT_LIST_FIELDS = new String[] {"thumbnail", "dc.date.issued(date)", "dc.title", "dc.contributor.*"};
            DEFAULT_LIST_WIDTHS = new String[] {"*", "130", "60%", "40%"};
        } 
        else
        {
            DEFAULT_LIST_FIELDS = new String[] {"dc.date.issued(date)", "dc.title", "dc.contributor.*"};
            DEFAULT_LIST_WIDTHS = new String[] {"130", "60%", "40%"};
        }

        // get the date and title fields
        String dateLine = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.index.date");
        if (dateLine != null)
        {
            dateField = dateLine;
        }

        String titleLine = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.index.title");
        if (titleLine != null)
        {
            titleField = titleLine;
        }

        String authorLine = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.author-field");
        if (authorLine != null)
        {
            authorField = authorLine;
        }
    }

    public ItemListTag()
    {
        super();
    }

    @Override
    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();

        boolean emphasiseDate = false;
        boolean emphasiseTitle = false;

        if (emphColumn != null)
        {
            emphasiseDate = emphColumn.equalsIgnoreCase("date");
            emphasiseTitle = emphColumn.equalsIgnoreCase("title");
        }

        // get the elements to display
        String[] browseFields  = null;
        String[] browseWidths = null;

        if (sortOption != null)
        {
            if (ArrayUtils.isEmpty(browseFields))
            {
                browseFields = configurationService.getArrayProperty("webui.itemlist.sort." + sortOption.getName() + ".columns");
                browseWidths  = configurationService.getArrayProperty("webui.itemlist.sort." + sortOption.getName() + ".widths");
            }

            if (ArrayUtils.isEmpty(browseFields))
            {
                browseFields = configurationService.getArrayProperty("webui.itemlist." + sortOption.getName() + ".columns");
                browseWidths  = configurationService.getArrayProperty("webui.itemlist." + sortOption.getName() + ".widths");
            }
        }

        if (config!=null)
        {
            browseFields = configurationService.getArrayProperty("webui.itemlist." + config + ".columns");
            browseWidths  = configurationService.getArrayProperty("webui.itemlist." + config + ".widths");
        }
        

        if (ArrayUtils.isEmpty(browseFields))
        {
            browseFields = configurationService.getArrayProperty("webui.itemlist.columns");
            browseWidths  = configurationService.getArrayProperty("webui.itemlist.widths");


        }

        // Have we read a field configration from dspace.cfg?
        if (ArrayUtils.isNotEmpty(browseFields))
        {
            // If thumbnails are disabled, strip out any thumbnail column from the configuration
            if (!showThumbs)
            {
                // check if it contains a thumbnail entry
                // If so, remove it, and the width associated with it
                int thumbnailIndex = ArrayUtils.indexOf(browseFields, "thumbnail");
                if(thumbnailIndex>=0)
                {
                    browseFields = (String[]) ArrayUtils.remove(browseFields, thumbnailIndex);
                    if(ArrayUtils.isNotEmpty(browseWidths))
                    {
                       browseWidths = (String[]) ArrayUtils.remove(browseWidths, thumbnailIndex);
                    }
                }
            }
		} else {
            browseFields = DEFAULT_LIST_FIELDS;
            browseWidths = DEFAULT_LIST_WIDTHS;
        }

        // Arrays used to hold the information we will require when outputting each row
        boolean isDate[]   = new boolean[browseFields.length];
        boolean emph[]     = new boolean[browseFields.length];
        boolean isAuthor[] = new boolean[browseFields.length];
        boolean viewFull[] = new boolean[browseFields.length];
        String[] browseType = new String[browseFields.length];
        String[] cOddOrEven = new String[browseFields.length];

        try
        {
            // Get the interlinking configuration too
            CrossLinks cl = new CrossLinks();

            // Get a width for the table
            String tablewidth = configurationService.getProperty("webui.itemlist.tablewidth");

            // If we have column widths, output a fixed layout table - faster for browsers to render
            // but not if we have to add an 'edit item' button - we can't know how big it will be
            if (ArrayUtils.isNotEmpty(browseWidths) && browseWidths.length == browseFields.length && !linkToEdit)
            {
                // If the table width has been specified, we can make this a fixed layout
                if (!StringUtils.isEmpty(tablewidth))
                {
                    out.println("<table style=\"width: " + tablewidth + "; table-layout: fixed;\" align=\"center\" class=\"table\" summary=\"This table browses all dspace content\">");
                }
                else
                {
                    // Otherwise, don't constrain the width
					out.println("<table border=\"0\" align=\"center\" class=\"table table-hover\" summary=\"This table browses all dspace content\">");
                }

                // Output the known column widths
                out.print("<colgroup>");

                for (int w = 0; w < browseWidths.length; w++)
                {
                    out.print("<col width=\"");

                    // For a thumbnail column of width '*', use the configured max width for thumbnails
                    if (browseFields[w].equals("thumbnail") && browseWidths[w].equals("*"))
                    {
                        out.print(thumbItemListMaxWidth);
					}
                    else
                    {
                        out.print(StringUtils.isEmpty(browseWidths[w]) ? "*" : browseWidths[w]);
                    }

                    out.print("\" />");
                }

                out.println("</colgroup>");
            }
            else if (!StringUtils.isEmpty(tablewidth))
            {
                out.println("<table width=\"" + tablewidth + "\" align=\"center\" class=\"table table-hover\" summary=\"This table browses all dspace content\">");
			}
            else
            {
                out.println("<table align=\"center\" class=\"table\" summary=\"This table browses all dspace content\">");
			}

            // Output the table headers
            out.println("<tr>");

            for (int colIdx = 0; colIdx < browseFields.length; colIdx++)
            {
                String field = browseFields[colIdx].toLowerCase().trim();
                cOddOrEven[colIdx] = (((colIdx + 1) % 2) == 0 ? "Odd" : "Even");

                String style = null;
                
                // backward compatibility, special fields
				if (field.equals("thumbnail")) {
                    style = "thumbnail";
				} else if (field.equals(titleField)) {
                    style = "title";
				} else if (field.equals(dateField)) {
                    style = "date";
                }
                
                Matcher fieldStyleMatcher = fieldStylePatter.matcher(field);
				if (fieldStyleMatcher.matches()) {
                    style = fieldStyleMatcher.group(1);
                }
                
				if (style != null) {
					field = field.replaceAll("\\(" + style + "\\)", "");
                    useRender[colIdx] = style;
				} else {
                    useRender[colIdx] = "default";
                }

                // Cache any modifications to field
                browseFields[colIdx] = field;

                // find out if this is the author column
                if (field.equals(authorField))
                {
                    isAuthor[colIdx] = true;
                }

                // find out if this field needs to link out to other browse views
                if (cl.hasLink(field))
                {
                    browseType[colIdx] = cl.getLinkType(field);
                    viewFull[colIdx] = BrowseIndex.getBrowseIndex(browseType[colIdx]).isItemIndex();
                }

                // find out if we are emphasising this field
                if (field.equals(emphColumn))
                {
                    emph[colIdx] = true;
                }
                else if ((field.equals(dateField) && emphasiseDate) ||
                        (field.equals(titleField) && emphasiseTitle))
                {
                    emph[colIdx] = true;
                }

                // prepare the strings for the header
                String id = "t" + Integer.toString(colIdx + 1);

				// String css = "oddRow" + cOddOrEven[colIdx] + "Col";
				String css = "oddRow";
				String csssort = ""; 
                String message = "itemlist." + field;
                
				String thJs = null;

				for (SortOption tmpSo : SortOption.getSortOptions()) {
					if (field.equalsIgnoreCase(tmpSo.getMetadata())) {
					    css += " sortable sort_" + tmpSo.getNumber();
						thJs = " onclick=\"sortBy(" + tmpSo.getNumber() + ",";
						if (sortOption != null
								&& sortOption.getNumber() == tmpSo.getNumber()) {
							if (isDesc) {
								thJs += " 'ASC'";
								css += " sorted_desc";
								csssort += "fa fa-sort-desc pull-right";
							} else {
								thJs += " 'DESC'";
								css += " sorted_asc";								
								csssort += "fa fa-sort-asc pull-right";
							}
						} else {
							thJs += " 'ASC'";
							css += " sortable";
							csssort += "fa fa-sort pull-right";
						}
						thJs += ")\"";
						break;
					}
				}

                // output the header
				out.print("<th id=\""
						+ id
						+ "\" class=\""
						+ css
						+ "\">"
                        + (emph[colIdx] ? "<strong>" : "")
						+ (thJs != null ? "<a " + thJs + " href=\"#\">" : "")
						+ LocaleSupport.getLocalizedMessage(pageContext,
								message) + (thJs != null ? "<i class=\""+ csssort +"\"></i></a>" : "")
                        + (emph[colIdx] ? "</strong>" : "") + "</th>");
            }

            if (linkToEdit)
            {
                String id = "t" + Integer.toString(cOddOrEven.length + 1);
                String css = "oddRow" + cOddOrEven[cOddOrEven.length - 2] + "Col";

                // output the header
                out.print("<th id=\"" + id +  "\" class=\"" + css + "\">"
                        + (emph[emph.length - 2] ? "<strong>" : "")
                        + "&nbsp;" //LocaleSupport.getLocalizedMessage(pageContext, message)
                        + (emph[emph.length - 2] ? "</strong>" : "") + "</th>");
            }

            out.print("</tr>");

            // now output each item row
            for (Item item : items)
            {
                // now prepare the XHTML frag for this division
            	out.print("<tr>"); 
                String rOddOrEven;
				if (i == highlightRow) {
                    rOddOrEven = "highlight";
				} else {
					rOddOrEven = ((i % 2) == 1 ? "odd" : "even");
                }

				if (inputName != null) {
					out.print("<td align=\"right\" class=\"oddRowOddCol\">");
					out.print("<input type=\""
							+ (radioButton ? "radio" : "checkbox")
							+ "\" name=\"");
					out.print(inputName + "\" value=\"" + items[i].getID()
							+ "\" />");
					out.print("</td>");
                }
				out.print("<td align=\"right\" class=\"oddRowOddCol\">"
						+ (itemStart + i) + "</td>");

                for (int colIdx = 0; colIdx < browseFields.length; colIdx++)
                {
                    String field = browseFields[colIdx];

                    // get the schema and the element qualifier pair
                    // (Note, the schema is not used for anything yet)
                    // (second note, I hate this bit of code.  There must be
                    // a much more elegant way of doing this.  Tomcat has
                    // some weird problems with variations on this code that
                    // I tried, which is why it has ended up the way it is)
                    StringTokenizer eq = new StringTokenizer(field, ".");

                    String[] tokens = { "", "", "" };
                    int k = 0;
                    while(eq.hasMoreTokens())
                    {
                        tokens[k] = eq.nextToken().toLowerCase().trim();
                        k++;
                    }
                    String schema = tokens[0];
                    String element = tokens[1];
                    String qualifier = tokens[2];
                    
                    // first get hold of the relevant metadata for this column
                    List<MetadataValue> metadataArray;
                    
                    if (schema.equalsIgnoreCase("extra")) {
                    	
                    	String val = null;
                    	Object obj = items[i].extraInfo.get(element);
						if (obj != null) {
							val = String.valueOf(obj);
						}
                    	if (StringUtils.isNotBlank(val)) {
                    		metadataArray = new ArrayList<>();
                    		MetadataValue metadataValue = new MetadataValue();
                    		metadataValue.setValue(val);
                    		metadataValue.setSchema("extra");
                    		metadataValue.setElement(element);
                    		metadataArray.add(metadataValue);
                    	}
                    }
                    else {
	                    
                        if (qualifier.equals("*"))
                        {
                            metadataArray = itemService.getMetadata(item, schema, element, Item.ANY, Item.ANY);
                        }
                        else if (qualifier.equals(""))
                        {
                            metadataArray = itemService.getMetadata(item, schema, element, null, Item.ANY);
                        }
                        else
                        {
                            metadataArray = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
                        }
                    }
                    // save on a null check which would make the code untidy
					if (metadataArray == null) {
                        metadataArray = new ArrayList<>();
                    }

                    // now prepare the content of the table division
                    int limit = -1;
					if (isAuthor[colIdx]) {
                        limit = (authorLimit <= 0 ? metadataArray.length
                                : authorLimit);
                    }
                    
                    IDisplayMetadataValueStrategy strategy = getMetadataDisplayStrategy(useRender, colIdx);
                            
					String metadata = getMetadataDisplayByStrategy(hrq, emph, viewFull, browseType, i, colIdx, field,
							metadataArray, limit, strategy);

                    // prepare extra special layout requirements for dates
                    String extras = strategy.getExtraCssDisplay(hrq, limit,
							viewFull[colIdx], browseType[colIdx], colIdx,
							field, metadataArray, items[i], disableCrossLinks,
							emph[colIdx]);

                    String markClass = "";
                    if (field.startsWith("mark_"))
                    {
                    	markClass = " "+field+"_tr";
                    }

                    
                    String id = "t" + Integer.toString(colIdx + 1);
                    out.print("<td headers=\"" + id + "\" class=\""
							+ rOddOrEven + "Row" + cOddOrEven[colIdx]
							+ "Col\" " + (extras != null ? extras : "") + ">"
							+ metadata + "</td>");
                }

                // Add column for 'edit item' links
                if (linkToEdit)
                {
                    String id = "t" + Integer.toString(cOddOrEven.length + 1);

					if (inputName != null) { // subform is not allowed in html
												// so we need to use a
												// javascript on the onclick...
						out.print("<td headers=\""
								+ id
								+ "\" class=\""
								+ rOddOrEven
								+ "Row"
								+ cOddOrEven[cOddOrEven.length - 2]
								+ "Col\" nowrap>"
								+ "<input class=\"btn btn-default\" type=\"button\" value=\"Edit Item\" onclick=\"javascript:self.location='"
								+ hrq.getContextPath()
								+ "/tools/edit-item?handle="
								+ items[i].getHandle() + "'\"" + "/>" + "</td>");
					} else {
						out.print("<td headers=\""
								+ id
								+ "\" class=\""
								+ rOddOrEven
								+ "Row"
								+ cOddOrEven[cOddOrEven.length - 2]
								+ "Col\" nowrap>"
								+ "<form method=\"get\" action=\""
								+ hrq.getContextPath()
								+ "/tools/edit-item\">"
								+ "<input type=\"hidden\" name=\"handle\" value=\""
								+ items[i].getHandle()
								+ "\" />"
                            + "<input type=\"submit\" value=\"Edit Item\" /></form>"
                            + "</td>");
                    }
				}

                out.println("</tr>");
            }

            // close the table
            out.println("</table>");
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        catch (BrowseException e)
        {
            throw new JspException(e);
        } catch (SortException e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    public int getAuthorLimit()
    {
        return authorLimit;
    }

    public void setAuthorLimit(int al)
    {
        authorLimit = al;
    }

    public boolean getLinkToEdit()
    {
        return linkToEdit;
    }

    public void setLinkToEdit(boolean edit)
    {
        this.linkToEdit = edit;
    }

    public boolean getDisableCrossLinks()
    {
        return disableCrossLinks;
    }

    public void setDisableCrossLinks(boolean links)
    {
        this.disableCrossLinks = links;
    }

    public SortOption getSortOption()
    {
        return sortOption;
    }

    public void setSortOption(SortOption so)
    {
        sortOption = so;
    }

    /**
     * Get the items to list
     *
     * @return the items
     */
    public List<Item> getItems()
    {
        return items;
    }

    /**
     * Set the items to list
     *
     * @param itemsIn
     *            the items
     */
    public void setItems(List<Item> itemsIn)
    {
        items = itemsIn;
    }

    /**
     * Get the row to highlight - null or -1 for no row
     *
     * @return the row to highlight
     */
    public String getHighlightrow()
    {
        return String.valueOf(highlightRow);
    }

    /**
     * Set the row to highlight
     *
     * @param highlightRowIn
     *            the row to highlight or -1 for no highlight
     */
    public void setHighlightrow(String highlightRowIn)
    {
        if ((highlightRowIn == null) || highlightRowIn.equals(""))
        {
            highlightRow = -1;
        }
        else
        {
            try
            {
                highlightRow = Integer.parseInt(highlightRowIn);
            }
            catch (NumberFormatException nfe)
            {
                highlightRow = -1;
            }
        }
    }

    /**
     * Get the column to emphasise - "title", "date" or null
     *
     * @return the column to emphasise
     */
    public String getEmphcolumn()
    {
        return emphColumn;
    }

    /**
     * Set the column to emphasise - "title", "date" or null
     *
     * @param emphColumnIn
     *            column to emphasise
     */
    public void setEmphcolumn(String emphColumnIn)
    {
        emphColumn = emphColumnIn;
    }

    @Override
    public void release()
    {
        highlightRow = -1;
        emphColumn = null;
        items = null;
		inputName = null;
		radioButton = false;
		isDesc = false;
	}


	// get-set methods for custom attribute
	public int getItemStart() {
		return itemStart;
	}

	public void setItemStart(int iStart) {
		itemStart = iStart;
	}

	public void setInputName(String inputName) {
		this.inputName = inputName;
    }

	public void setRadioButton(boolean radioButton) {
		this.radioButton = radioButton;
	}

	public void setOrder(String order) {
			if (SortOption.DESCENDING.equalsIgnoreCase(order))
				isDesc = true;
			else
				isDesc = false;
		}

	public void setConfig(String config)
    {
		this.config = config;
	}

    private String getMetadataDisplayByStrategy(HttpServletRequest hrq,
            boolean[] emph, boolean[] viewFull, String[] browseType, int i,
            int colIdx, String field, Metadatum[] metadataArray, int limit,
            IDisplayMetadataValueStrategy strategy)
    {
        String metadata = "";
        try
        {
            metadata = strategy.getMetadataDisplay(hrq, limit, viewFull[colIdx],
                    browseType[colIdx], colIdx, field, metadataArray, items[i],
                    disableCrossLinks, emph[colIdx]);
        }
        catch (Exception e)
        {
            log.error("Error getMetadataDisplay on " + items[i].getHandle());
        }
        return metadata;
    }

    private IDisplayMetadataValueStrategy getMetadataDisplayStrategy(
            String[] useRender, int colIdx)
    {
        IDisplayMetadataValueStrategy strategy = (IDisplayMetadataValueStrategy) pluginService
                .getNamedPlugin(IDisplayMetadataValueStrategy.class,
                        useRender[colIdx]);

        // fallback compatibility
        if (strategy == null)
        {
            if (useRender[colIdx].equalsIgnoreCase("title"))
            {
                strategy = new TitleDisplayStrategy();
            }
            else if (useRender[colIdx].equalsIgnoreCase("date"))
            {
                strategy = new DateDisplayStrategy();
            }
            else if (useRender[colIdx].equalsIgnoreCase("thumbnail"))
            {
                strategy = new ThumbDisplayStrategy();
            }
            else if (useRender[colIdx].equalsIgnoreCase("link"))
            {
                strategy = new LinkDisplayStrategy();
            }
            else if (useRender[colIdx].equalsIgnoreCase("default"))
            {
                strategy = new DefaultDisplayStrategy();
            }
            else if (useRender[colIdx].startsWith("mark_"))
            {
                strategy = new MarkingDisplayStrategy();
            }
            else
            {
                // if the plugin instantiation fails try to use the
                // resolver catch all strategy
                strategy = new ResolverDisplayStrategy();
            }
        }
        return strategy;
    }
}
