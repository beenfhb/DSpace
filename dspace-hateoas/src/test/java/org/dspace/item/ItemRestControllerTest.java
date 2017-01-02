package org.dspace.item;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.DSpace;
import org.dspace.core.DSpaceTest;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.repository.ItemRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DSpaceTest.class, DSpace.class})
@WebAppConfiguration
public class ItemRestControllerTest {

	/** log4j category */
	private static final Logger log = Logger.getLogger(ItemRestControllerTest.class);

	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	private MockMvc mockMvc;

	private HttpMessageConverter mappingJackson2HttpMessageConverter;

	protected DSpaceObject dspaceObject;
	private Item item;
	private Collection collection;
	private Community owningCommunity;

	private Context context;

	protected CommunityService communityService;
	protected CollectionService collectionService;
	protected ItemService itemService;
	protected WorkspaceItemService workspaceItemService;
	protected InstallItemService installItemService;

	/**
	 * Test properties. These configure our general test environment
	 */
	protected static Properties testProps;

	private EPerson eperson;
	
	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	void setConverters(HttpMessageConverter<?>[] converters) {

		this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
				.filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().orElse(null);

		assertNotNull("the JSON message converter must not be null", this.mappingJackson2HttpMessageConverter);
	}

	@Before
	public void setup() throws Exception {
		this.mockMvc = webAppContextSetup(webApplicationContext).build();

		CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
		CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
		ItemService itemService = ContentServiceFactory.getInstance().getItemService();
		WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
		InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
		
		this.context = new Context();
		context.turnOffAuthorisationSystem();

        //Find our global test EPerson account. If it doesn't exist, create it.
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        eperson = ePersonService.findByEmail(context, "test@email.com");
        if(eperson == null)
        {
            // This EPerson creation should only happen once (i.e. for first test run)
            log.info("Creating initial EPerson (email=test@email.com) for Unit Tests");
            eperson = ePersonService.create(context);
            eperson.setFirstName(context, "first");
            eperson.setLastName(context, "last");
            eperson.setEmail("test@email.com");
            eperson.setCanLogIn(true);
            eperson.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
            // actually save the eperson to unit testing DB
            ePersonService.update(context, eperson);
        }
        // Set our global test EPerson as the current user in DSpace
        context.setCurrentUser(eperson);

        // If our Anonymous/Administrator groups aren't initialized, initialize them as well
        EPersonServiceFactory.getInstance().getGroupService().initDefaultGroupNames(context);
		
		this.owningCommunity = communityService.create(null, context);
		this.collection = collectionService.create(context, owningCommunity);
		WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
		this.item = installItemService.installItem(context, workspaceItem);

		item.setSubmitter(context.getCurrentUser());
		itemService.update(context, item);
		this.dspaceObject = item;
		// we need to commit the changes so we don't block the table for testing
		context.restoreAuthSystemState();
	}

	@Test
	public void readItem() throws Exception {
		mockMvc.perform(get("/api/core/item/1")).andExpect(status().isOk());
		
		mockMvc.perform(get("/api/core/item/" + this.dspaceObject.getID())).andExpect(status().isOk())
				.andExpect(content().contentType(contentType)).andExpect(jsonPath("$.id", is(dspaceObject.getID())));
	}

	public void readItems() throws Exception {
	}

	public void createItem() throws Exception {
	}

	protected String json(Object o) throws IOException {
		MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
		this.mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
		return mockHttpOutputMessage.getBodyAsString();
	}
}
