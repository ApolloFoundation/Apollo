package com.apollocurrency.aplwallet.apl.core.files.statcheck;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.distribution.TDistribution;

/**
 * General statistics
 * @author alukin@gmail.com
 */
public class PeerInfoStatistics {
    /**
     * confidence probability
     */
    public static final double BETA=0.90;
            
    private final Map<String,PeerInfoGroup> sorted = new HashMap<>();
    
    public void add(PeerFileHashSum pi){
      if(pi!=null)  {
        String hash = Convert.toHexString(pi.getHash());
        PeerInfoGroup pg = sorted.get(hash);
        if(pg==null){
            pg = new PeerInfoGroup(hash);
            sorted.put(hash,pg);
        }
        pg.add(pi);
      }
    }
    
    /**
     * Student's t-distribution function
     * @param p desired confidentiality probability
     * @param n numver of samples, defree of freedom is v=n-1
     * @return t as in tables in all mathstat books
     */
    public static double StudentsT(double p, long n){
        TDistribution td = new TDistribution(n-1);
        double res = td.inverseCumulativeProbability(p);
        return res;
    }
    
    public Map<String,ProbabInfo> getFrequences() throws NotEnoughDataException{
        Map<String,ProbabInfo> res = new HashMap();
        long all=0;
        for(String hash: sorted.keySet()){
            all=all+sorted.get(hash).count();
        }
        if(all<2){
            throw new NotEnoughDataException("Count of samples is less then 2 and not enough for statistics");
        }
        double t = StudentsT(BETA, all);
        for(String hash: sorted.keySet()){
            ProbabInfo pri = new ProbabInfo();
            PeerInfoGroup pg = sorted.get(hash);
            pri.frequency = pg.count()*1.0D/all;
            double sigma = Math.sqrt(((1.0D-pri.frequency)*pri.frequency)/all);
            pri.confidenceEpsilon = t*(sigma/Math.sqrt(all-1));
            res.put(hash, pri);
        }
        return res;
    }
    
    boolean isAlreadyCounted(PeerFileHashSum pi){
        boolean res = false;
        for(String hash: sorted.keySet()){
            PeerInfoGroup pg = sorted.get(hash);
            if(pg!=null){
                res = pg.contains(pi);
            }
            if(res){
                break;
            }
        }
        return res;
    }
    
    public Set<PeerFileHashSum> getByHash(String hash){
        Set<PeerFileHashSum> res = new HashSet<>();
        PeerInfoGroup pg = sorted.get(hash);
        if(pg!=null){
            res.addAll(pg.pl);
        }
        return res;
    }

    public Set<PeerFileHashSum> getAllExceptHash(String key) {
        Set<PeerFileHashSum> res = new HashSet<>();
        for(String h: sorted.keySet()){
            PeerInfoGroup pg = sorted.get(h);
            if(h.equals(key)){
                res.addAll(pg.pl);
            }
        }
        return res;
    }
    
    public void crlear(){
        sorted.clear();
    }
}
