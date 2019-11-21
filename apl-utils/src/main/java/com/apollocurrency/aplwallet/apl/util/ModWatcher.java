/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

/**
 * This class replaces % operation when counter may change for more then 1.
 * We can see modulos operations every day looking at our own watches.
 * Imagine situation. You have to go from home at 7:00 to be in time at train station
 * and take your train  that goes every hour. You look at your watch and there is already 7:05. It does not mean 
 * "Holly caps! I am late! I'll go to next train!". It just means "No time for coffee, pal! Go, go,go!"
 * So this class takes modulos to form your watch scale and 
 * tooLate number that defines how much we can be late after round modulos. 
 * You also can get how many full circles your watch did.
 * Note: you are responsible for calculation of number of hole circles and missed "hours"
 * @author alukin@gmail.com
 */
public class ModWatcher {
    long modulos_m;
    long late_m;
/**
 * Constructs modWatcher instance. Thik of it as it is watch with 2 arrows? minutes and hours.
 * Only minutes matter for us.
 * @param modulos full scale is "modulos" minutes
 * @param tooLate we're trying not to miss  x % modulos == 0 event. How late we can be to
 * still be in time?
 */    
    ModWatcher(int modulos, long tooLate) {
        this.modulos_m=modulos;
        this.late_m = tooLate % modulos; // just to be sure it less
    }
  /**
   * How late we are after x % modulos == 0 event?
   * @param minutesTotal count of minutes
   * @return how late in this round ("hour")
   */  
    public Long howLate(long minutesTotal){
        long m = minutesTotal % modulos_m;
        return m;
    }
   /**
    * Get full circles count of minute arrow 
    * @param minutesTotal count of "minutes"
    * @return count of "hours"
    */ 
    public Long fullCircles(long minutesTotal){
       return minutesTotal /modulos_m;
    }
    /**
     * is it to late according to constructed parameters
     * @param minutesTotal
     * @return 
     */
    public boolean isTooLate(long minutesTotal){
       return howLate(minutesTotal)>late_m;
    }
}
