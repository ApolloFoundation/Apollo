package com.apollocurrency.aplwallet.apl.util;

/**
 *
 * @author al
 */
public class AppStatus {
    private static AppStatusUpdater updater;
    
    private AppStatus() {
        updater = new AppStatusUpdater() {
            @Override
            public void updateStatus(String status) {
               System.out.println(status);
            }
        };
    }
    
    public static AppStatus getInstance() {
        return AppStatusHolder.INSTANCE;
    }
    
    public static void setUpdater(AppStatusUpdater u){
        updater=u;
    }
    
    private static class AppStatusHolder {
        private static final AppStatus INSTANCE = new AppStatus();
    }
    
    public void update(String status){
        updater.updateStatus(status);
    }
}
