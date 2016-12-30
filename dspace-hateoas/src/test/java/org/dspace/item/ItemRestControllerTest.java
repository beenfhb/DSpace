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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

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
import org.dspace.repository.ItemRepository;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@SpringApplicationConfiguration(classes = DSpace.class)
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

	private List<Item> itemList = new ArrayList<Item>();

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

	/**
	 * DSpace Kernel. Must be started to initialize ConfigurationService and any
	 * other services.
	 */
	protected static DSpaceKernelImpl kernelImpl;

	private ItemRepository itemRepository;
	
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
		initKernel();
		this.mockMvc = webAppContextSetup(webApplicationContext).build();

		CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
		CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
		ItemService itemService = ContentServiceFactory.getInstance().getItemService();
		WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
		InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

		this.itemRepository.deleteAllInBatch();

		context.turnOffAuthorisationSystem();
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

	/**
	 * This method will be run before the first test as per @BeforeClass. It
	 * will initialize shared resources required for all tests of this class.
	 *
	 * This method loads our test properties to initialize our test environment,
	 * and then starts the DSpace Kernel (which allows access to services).
	 */
	public void initKernel() {
		// set a standard time zone for the tests
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Dublin"));

		// Initialise the service manager kernel
		kernelImpl = DSpaceKernelInit.getKernel(null);
		if (!kernelImpl.isRunning()) {
			// NOTE: the "dspace.dir" system property MUST be specified via
			// Maven
			kernelImpl.start(System.getProperty("dspace.dir")); // init the
																// kernel
		}
	}

	/**
	 * This method will be run after all tests finish as per @AfterClass. It
	 * will clean resources initialized by the @BeforeClass methods.
	 */
	@After
	public void destroyKernel() throws SQLException {

		// Also clear out the kernel & nullify (so JUnit will clean it up)
		if (kernelImpl != null) {
			kernelImpl.destroy();
		}
		kernelImpl = null;
	}
}
