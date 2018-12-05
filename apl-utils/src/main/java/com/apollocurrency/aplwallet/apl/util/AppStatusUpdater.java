
package com.apollocurrency.aplwallet.apl.util;

/**
 * Interface to be implemented to pass application status updated from
 * different parts of program
 * @author alukin@gmail
 */
public interface AppStatusUpdater {
    public void updateStatus(String status);
}
