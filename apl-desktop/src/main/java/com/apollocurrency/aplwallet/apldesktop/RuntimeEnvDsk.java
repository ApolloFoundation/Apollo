
package com.apollocurrency.aplwallet.apldesktop;

import com.apollocurrency.aplwallet.apldesktop.DesktopMode;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import static java.awt.GraphicsEnvironment.isHeadless;

/**
 *
 * @author al
 */
public class RuntimeEnvDsk extends RuntimeEnvironment{


    
    public static RuntimeMode getRuntimeMode() {
        System.out.println("isHeadless=" + isHeadless());
        if (isDesktopEnabled()) {
            return new DesktopMode();
        }else{
            return RuntimeEnvironment.getRuntimeMode();
        } 
        
    }    
}
