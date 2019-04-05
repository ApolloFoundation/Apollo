package com.apollocurrency.aplwallet.apl.util;

import javax.enterprise.inject.Vetoed;

/**
 *
 * @author al
 */
@Vetoed
public class AppStatus {
    private static AppStatusUpdater updater;
    
    private AppStatus() {
        updater = new AppStatusUpdater() {
            @Override
            public void updateStatus(String status) {
               System.out.println(status);
            }

            @Override
            public void alert(String message) {
               System.err.println(message);
            }

            @Override
            public void error(String message) {
               System.err.println(message);
            }
        };
    }
    
    public static AppStatus getInstance() {
        return AppStatusHolder.INSTANCE;
    }
    
    public static void setUpdater(AppStatusUpdater u){
        updater=u;
    }
    @Vetoed
    private static class AppStatusHolder {
        private static final AppStatus INSTANCE = new AppStatus();
    }
    
    public void update(String status){
        updater.updateStatus(status);
    }
    public void error(String status){
        updater.error(status);
    }
    public void alert(String status){
        updater.alert(status);
    }
}
