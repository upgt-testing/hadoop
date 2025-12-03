package org.restarttest.adapter.yarn;

import org.restarttest.core.AdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to register the YARN adapter with the Restart Testing Framework.
 *
 * The adapter is automatically registered via the static initializer block,
 * which is invoked when this class is loaded. The adapter is also registered
 * via Java's ServiceLoader mechanism (see META-INF/services).
 */
public class YarnAdapterRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(YarnAdapterRegistration.class);
    private static volatile boolean registered = false;

    /**
     * Manually register the YARN adapter.
     * This method is idempotent - multiple calls are safe.
     */
    public static synchronized void register() {
        if (!registered) {
            YarnClusterAdapter adapter = new YarnClusterAdapter();
            AdapterRegistry.getInstance().register(adapter);
            LOG.info("YARN adapter registered successfully");
            registered = true;
        }
    }

    // Automatically register on class load
    static {
        register();
    }

    // Prevent instantiation
    private YarnAdapterRegistration() {
    }
}
