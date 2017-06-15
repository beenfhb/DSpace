/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.batch;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.hibernate.Session;
import org.xml.sax.InputSource;

public class ScriptCrossrefChecker
{

    private static final int PLACEHOLDER_SENDEDTOCROSSREF_SUCCESSFULLY = 999;

    private static XPathFactory factory = XPathFactory.newInstance();

    private static final String HOST = ConfigurationManager
            .getProperty("crossref.host");

    private static final int PORT = ConfigurationManager
            .getIntProperty("crossref.port");

    private static final String PASSWORD = ConfigurationManager
            .getProperty("crossref.password");

    private static final String USERNAME = ConfigurationManager
            .getProperty("crossref.username");

    public static String TABLE_NAME_DOI2ITEM = "doi2item";

    private static String dbName = ConfigurationManager.getProperty("db.name");

    private static MultiThreadedHttpConnectionManager m_cManager;

    private static String servicePOST = ConfigurationManager
            .getProperty("crossref.path.infodeposit");

    /** Seconds to wait before a connection is established. */
    public static int TIMEOUT_SECONDS = 60;

    /** Seconds to wait while waiting for data over the socket (SO_TIMEOUT). */
    public static int SOCKET_TIMEOUT_SECONDS = 1800; // 30 minutes

    /** Maximum http connections per host (for REST calls only). */
    public static int MAX_CONNECTIONS_PER_HOST = 15;

    /** Maximum total http connections (for REST calls only). */
    public static int MAX_TOTAL_CONNECTIONS = 30;

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptCrossrefChecker.class);

    public static void main(String[] args) throws ParseException
    {
        log.info("#### START Script crossref sender: -----" + new Date()
                + " ----- ####");
        Map<UUID, String> result = new HashMap<UUID, String>();

        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("a", "all", false,
                "Work on new inserted row, with placeholder metadata");
        options.addOption("s", "single", true, "Work on single item, , with");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptCrossrefSender \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptCrossrefSender -a|-s <item_id>] \n");

            System.exit(0);
        }

        if (line.hasOption('s') && line.hasOption('a'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptCrossrefSender -a|-s <item_id>] \n");
            System.out.println("Insert either a or s like parameters");
            log.error("Either a or s like parameters");
            System.exit(1);
        }

        m_cManager = new MultiThreadedHttpConnectionManager();

        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            if (line.hasOption('a'))
            {

                int limit = 100;

                List<Object[]> rows = null;

                if ("oracle".equals(dbName))
                {
                    rows = getHibernateSession(context).createSQLQuery(
                            "select item_id, filename, identifier_doi from " + TABLE_NAME_DOI2ITEM
                                    + " d2i where d2i.response_code = '200'"
                                    + " AND ROWNUM <= " + limit).list();
                }
                else
                {
                	rows = getHibernateSession(context).createSQLQuery(
                            "select item_id, filename, identifier_doi from " + TABLE_NAME_DOI2ITEM
                                    + " d2i where d2i.response_code = '200'"
                                    + " LIMIT " + limit).list();
                }
                int offset = 0;

                while (!rows.isEmpty())
                {
                    if (offset > 0)
                    {
                        if ("oracle".equals(dbName))
                        {
                            rows = getHibernateSession(context).createSQLQuery(
                                            "select item_id, filename, identifier_doi from "
                                                    + TABLE_NAME_DOI2ITEM
                                                    + " d2i where d2i.response_code = '200'"
                                                    + " AND ROWNUM > " + limit
                                                    + " AND ROWNUM <= " + (limit + offset))
                                    .list();
                        }
                        else
                        {
                            rows = getHibernateSession(context).createSQLQuery(
                                            "select item_id, filename, identifier_doi from "
                                                    + TABLE_NAME_DOI2ITEM
                                                    + " d2i where d2i.response_code = '200'"
                                                    + " LIMIT " + limit
                                                    + " OFFSET " + offset)
                                    .list();
                        }
                    }
                    offset = limit + offset;
                    for (Object[] row : rows)
                    {
                        Item item = ContentServiceFactory.getInstance().getItemService().find(context, 
                        		(UUID)row[0]);//.getIntColumn("item_id"));
                        String filename = (String)row[1];//.getStringColumn("filename");
                        String doi = (String)row[2];//.getStringColumn("identifier_doi");

                        result.putAll(check(context, item, doi, filename));

                    }
                    context.commit();
                }
            }
            else
            {
                if (line.hasOption('s'))
                {
                    UUID id = UUID.fromString(line.getOptionValue("s"));
                    List<Object[]> rows = getHibernateSession(context).createSQLQuery(
                                    "SELECT item_id, filename, identifier_doi FROM "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i where d2i.response_code = '200' and d2i.item_id = :item_id").setParameter("item_id", id).list();
                    for (Object[] row : rows)
                    {
	                    String filename = (String)row[1];//.getStringColumn("filename");
	                    String doi = (String)row[2];//.getStringColumn("identifier_doi");
	                    Item item = ContentServiceFactory.getInstance().getItemService().find(context, 
                                (UUID)row[0]);//.getIntColumn("item_id"));
	                    result.putAll(check(context, item, doi, filename));
                    }


                }
                else
                {
                    System.out
                            .println("\n\nUSAGE:\n ScriptCrossrefSender -a|-s <item_id>] \n");
                    System.out.println("Option n or s is needed");
                    log.error("Option n or s is needed");
                    System.exit(1);
                }

            }
        }
        catch (SQLException e1)
        {
            log.error(e1.getMessage(), e1);
            if (context.isValid())
            {
                context.abort();
            }
        }

        log.info("#### Import details ####");

        for (UUID key : result.keySet())
        {
            log.info("ITEM: " + key + " RESULT: " + result.get(key));
        }

        log.info("#### ########################### ####");
        log.info("#### END: -----" + new Date() + " ----- ####");

        System.exit(0);
    }

    protected static Map<UUID, String> check(Context context, Item target,
            String doi, String filename) throws SQLException
    {
        GetMethod get = null;
        Map<UUID, String> result = new HashMap<UUID, String>();

        int responseCode = 0;
        try
        {

            // prepare the get method
            get = new GetMethod(servicePOST + "?usr=" + USERNAME + "&pwd="
                    + PASSWORD + "&doi_batch_id=" + target.getID()
                    + "&file_name=" + filename + "&type=result");

            HostConfiguration nss = new HostConfiguration();
            nss.setHost(HOST, PORT);

            responseCode = getHttpClient().executeMethod(nss, get);
            result.put(target.getID(), get.getResponseBodyAsString());

        }
        catch (Exception e)
        {
            result.put(target.getID(), responseCode + " - " + e.getMessage());
            log.error(
                    "FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
        }
        finally
        {

            if (get != null)
            {
                get.releaseConnection();
            }

            log.info("FOR item: " + target.getID() + " -> RESPONSECODE: "
                    + responseCode + " MESSAGE:" + result.get(target.getID()));
            if (responseCode == 200)
            {
                int responseCodeToInsert = responseCode;
                XPathExpression xpathexp = null;
                try
                {
                    xpathexp = factory.newXPath().compile(
                            "//record_diagnostic[@status='Success']");
                }
                catch (XPathExpressionException e)
                {
                    log.error("FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
                }

                String resultSuccess = "";
                try
                {
                    InputSource source = new InputSource(
                            get.getResponseBodyAsStream());
                    resultSuccess = xpathexp.evaluate(source);
                }
                catch (XPathExpressionException e)
                {
                    log.error("FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
                }
                catch (IOException e)
                {
                    log.error("FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
                }

                if (resultSuccess != null && !resultSuccess.isEmpty())
                {
                    target.getItemService().addMetadata(context, target, "dc", "identifier", "doi", null, doi);
                    target.getItemService().clearMetadata(context, target, "dc", "utils", "processdoi", Item.ANY);
                    responseCodeToInsert = PLACEHOLDER_SENDEDTOCROSSREF_SUCCESSFULLY;
                    try
                    {
                        target.getItemService().update(context, target);
                        context.commit();
                    }
                    catch (AuthorizeException e)
                    {
                        log.error("FOR item: " + target.getID()
                                + " ERRORMESSAGE: " + e.getMessage(), e);
                    }
                }

              
                    getHibernateSession(context).createSQLQuery(
                                    "UPDATE "
                                            + TABLE_NAME_DOI2ITEM
                                            + " SET LAST_MODIFIED = :last_modified, RESPONSE_CODE = :response_code, NOTE = :note WHERE ITEM_ID = :item_id").setParameter("last_modified",
                                    new Date()).setParameter("response_code", responseCodeToInsert).setParameter("note",
                                    result.get(target.getID())).setParameter("item_id", target.getID()).executeUpdate();
              
            }
            else
            {
            	getHibernateSession(context).createSQLQuery("UPDATE "
                        + TABLE_NAME_DOI2ITEM
                        + " SET NOTE = :note WHERE ITEM_ID = :item_id").setParameter("note", responseCode + "-"
                        + result.get(target.getID())).setParameter("item_id", target.getID()).executeUpdate();
            }

            context.commit();
        }

        return result;
    }

    public static HttpClient getHttpClient()
    {

        m_cManager.getParams().setDefaultMaxConnectionsPerHost(
                MAX_CONNECTIONS_PER_HOST);
        m_cManager.getParams().setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
        m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);
        m_cManager.getParams().setSoTimeout(SOCKET_TIMEOUT_SECONDS * 1000);

        HttpClient client = new HttpClient(m_cManager);

        return client;
    }

    protected static Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}
