package org.dspace.core;

import java.sql.SQLException;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.After;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class DSpaceTest implements CommandLineRunner {

	private static final Logger log = Logger.getLogger(DSpaceTest.class);
	
	/**
	 * DSpace Kernel. Must be started to initialize ConfigurationService and any
	 * other services.
	 */
	protected static DSpaceKernelImpl kernelImpl;
	
    public static void main(String[] args) {
        SpringApplication.run(DSpaceTest.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
       initKernel();
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
