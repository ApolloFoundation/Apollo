package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            
    private final Map<BigInteger,PeerInfoGroup> sorted = new HashMap<>();
    
    public void add(HasHashSum pi){
        BigInteger hash = pi.getHash();
        PeerInfoGroup pg = sorted.get(hash);
        if(pg==null){
            pg = new PeerInfoGroup(hash);
            sorted.put(hash,pg);
        }
        pg.add(pi);
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
    
    public Map<BigInteger,ProbabInfo> getFrequences(){
        Map<BigInteger,ProbabInfo> res = new HashMap();
        long all=0;
        for(BigInteger hash: sorted.keySet()){
            all=all+sorted.get(hash).count();
        }
        double t = StudentsT(BETA, all);
        for(BigInteger hash: sorted.keySet()){
            ProbabInfo pri = new ProbabInfo();
            PeerInfoGroup pg = sorted.get(hash);
            pri.frequency = pg.count()*1.0D/all;
            double sigma = Math.sqrt(((1.0D-pri.frequency)*pri.frequency)/all);
            pri.confidenceEpsilon = t*(sigma/Math.sqrt(all-1));
            res.put(hash, pri);
        }
        return res;
    }
    
    boolean isAlreadyCounted(HasHashSum pi){
        boolean res = false;
        for(BigInteger hash: sorted.keySet()){
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
    
    public List<HasHashSum> getByHash(BigInteger hash){
        List<HasHashSum> res = new ArrayList<>();
        PeerInfoGroup pg = sorted.get(hash);
        if(pg!=null){
            res.addAll(pg.pl);
        }
        return res;
    }

    List<HasHashSum> getAllExceptHash(BigInteger key) {
        List<HasHashSum> res = new ArrayList<>();
        for(BigInteger h: sorted.keySet()){
            PeerInfoGroup pg = sorted.get(h);
            if(h!=key){
                res.addAll(pg.pl);
            }
        }
        return res;
    }
    
    public void crlear(){
        sorted.clear();
    }
}
