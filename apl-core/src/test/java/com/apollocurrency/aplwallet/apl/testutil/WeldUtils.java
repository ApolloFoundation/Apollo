package com.apollocurrency.aplwallet.apl.testutil;

import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.WeldInitiator;

import java.util.List;

import static org.mockito.Mockito.mock;

public class WeldUtils {
    public static WeldInitiator.Builder from(List<Class> beanClasses, List<Class> mockClasses) {

        WeldInitiator.Builder builder = WeldInitiator.from(beanClasses.toArray(new Class[0]));
        for (Class mockClass : mockClasses) {
            builder.addBeans(MockBean.of(mock(mockClass), mockClass));
        }
        return builder;
    }
}
