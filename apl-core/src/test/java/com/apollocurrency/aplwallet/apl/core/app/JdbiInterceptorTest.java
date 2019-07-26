/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.mockito.Mockito.spy;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalInterceptor;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit.AbstractWeldInitiator;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JdbiInterceptorTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    JdbiHandleFactory factory = spy(new JdbiHandleFactory());
    private Weld weld = AbstractWeldInitiator.createWeld();
    {
        weld.addInterceptor(JdbiTransactionalInterceptor.class);

        weld.addBeanClasses(JdbiTransactionalInterceptor.class);
    }

    @WeldSetup
    public WeldInitiator weldInitiator = WeldInitiator.from(weld)
            .addBeans(MockBean.of(factory, JdbiHandleFactory.class)).build();
}
