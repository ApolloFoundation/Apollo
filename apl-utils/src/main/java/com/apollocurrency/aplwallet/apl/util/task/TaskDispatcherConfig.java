package com.apollocurrency.aplwallet.apl.util.task;

import java.util.Enumeration;

public interface TaskDispatcherConfig {

    String getName();

    String getInitParameter(String key);

    Enumeration<String> getInitParameterNames();
}
