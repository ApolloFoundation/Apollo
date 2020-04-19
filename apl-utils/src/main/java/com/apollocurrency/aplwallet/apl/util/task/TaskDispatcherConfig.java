package com.apollocurrency.aplwallet.apl.util.task;

import java.util.Enumeration;

/**
 * The dispatcher configuration
 */
public interface TaskDispatcherConfig {

    /**
     * Name of the dispatcher
     *
     * @return the dispatcher name
     */
    String getName();

    /**
     * Return the init parameter value
     *
     * @param key the name of parameter
     * @return the parameter value or null if not present
     */
    String getInitParameter(String key);


    /**
     * Returns the names of the initialization parameters as an Enumeration of String objects
     *
     * @return an enumeration of String objects containing the names of the initialization parameters
     */
    Enumeration<String> getInitParameterNames();
}
