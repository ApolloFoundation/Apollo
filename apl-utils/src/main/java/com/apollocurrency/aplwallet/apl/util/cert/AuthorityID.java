
package com.apollocurrency.aplwallet.apl.util.cert;

import com.apollocurrency.aplwallet.apl.util.cls.BasicClassificator;
import com.apollocurrency.aplwallet.apl.util.cls.ClsItem;
import com.apollocurrency.aplwallet.apl.util.cls.AplClassificators;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

/**
 * Apollo AUthority ID is set of bits that classifies Apollo crypto actors
 * and actor's capabilities
 * @author alukin@gmail.com
 */
public class AuthorityID {

    public static AuthorityID fromStrings(String actorType, String actorSubType, String businessCode, String regionCode, String opCode, String supCode) {
      AuthorityID res = new AuthorityID();
      BasicClassificator ac = AplClassificators.getCls(AplClassificators.ACTOR_CLS);
      BasicClassificator bc = AplClassificators.getCls(AplClassificators.BUSINESS_CLS);
      BasicClassificator rc = AplClassificators.getCls(AplClassificators.REGION_CLS);
      BasicClassificator oc = AplClassificators.getCls(AplClassificators.OPERATIONS_CLS);
      BasicClassificator sc = AplClassificators.getCls(AplClassificators.SUPL_CLS);
      ClsItem a = ac.getItem(actorType,actorSubType);
      if(a==null){
          throw new IllegalArgumentException("actorTupe and sub type is not in classificator");
      }
      ClsItem b = bc.getItem(businessCode);
      if(b==null){
          throw new IllegalArgumentException("bussinessCode type is not in classificator");
      } 
      ClsItem r = rc.getItem(regionCode);
      if(r==null){
          throw new IllegalArgumentException("regionCode type is not in classificator");
      }
      ClsItem o = bc.getItem(businessCode);
      if(o==null){
          throw new IllegalArgumentException("operationsCode type is not in classificator");
      } 
      ClsItem s = sc.getItem(businessCode);
      if(s==null){
          throw new IllegalArgumentException("spuplementalCode type is not in classificator");
      }
      int at = ac.getItem(actorType).getIntegerValue();
      int ast = ac.getItem(actorType,actorSubType).getIntegerValue();
      ActorType vat = new ActorType();
      vat.setType(at);
      vat.setSubType(ast);
      res.setActorType(vat.getValue());
      res.setBusinessCode(b.getIntegerValue());
      res.setRegionCode(r.getIntegerValue());
      res.setOperationCode(o.getIntegerValue());
      res.setSuplementalCode(s.getIntegerValue());
      return res;
    }
    
    private byte[] authorityID;
    
    private void setup(){
        authorityID = new byte[16];
        byte zero = 0;
        Arrays.fill(authorityID,zero);        
    }
    
    public AuthorityID() {
        setup();
    }
    
    public AuthorityID(BigInteger authID) {
        setup();
        byte[] a = authID.toByteArray();
        int idx_dst=authorityID.length-1;
        int idx_src=a.length-1;
        while(idx_dst>=0 && idx_src>=0){
            authorityID[idx_dst]=a[idx_src];
            idx_src--;
            idx_dst--;
        }
    }
    
    public AuthorityID(byte[] a) {
        setup();
        int idx_dst=authorityID.length-1;
        int idx_src=a.length-1;
        while(idx_dst>=0 && idx_src>=0){
            authorityID[idx_dst]=a[idx_src];
            idx_src--;
            idx_dst--;
        }
    }
    
    public BigInteger getAuthorityID(){
       return new BigInteger(authorityID);
    }
    
    /**
     * ActorType and ActorSubType are first 2 most significant bytes of 
     * AuthorityID respectively
     * @return 2 bytes of ActorType wrapped to 4 bytes of int
     */
    public int getActorType(){
        int res = authorityID[0]<<8 | authorityID[1];
        return res;
    }
   
    /**
     * Sets ActorType and ActorSubType as first 2 most significant bytes of 
     * AuthorityID respectively
     * @param at 2 bytes wrapped in 2 least significant bytes of int
     */
    public void setActorType(int at){
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(at);
        authorityID[0]=bb.get(2);
        authorityID[1]=bb.get(3);
    }
    
    /**
     * ActorType and ActorSubType are first 2 most significant bytes of 
     * AuthorityID respectively
     * @return ActorType and ActorSubType wrapped in ActorType class
     */
    public ActorType getApolloActorType(){
        return new ActorType(getActorType());
    }
    /**
     * Sets ActorType and ActorSubType as first 2 most significant bytes of 
     * uthorityID respectively
     * @param vat ActorType class hat wraps those 2 bytes
     */   
    public void setApolloActorType(ActorType vat){
        setActorType(vat.getValue());
    }
    
    /**
     * RegionCode is 2nd and 3rd most significant bytes of AuthorityID
     * @return 2 bytes of RegionCode wrapped to 2 least significant bytes of int
     */
    public Integer getRegionCode(){
        int res = authorityID[2]<<8 | authorityID[3];
        return res;
    }
    
    /**
     * RegionCode is 2nd and 3rd most significant bytes of AuthorityID
     * @param rc 2 bytes of RegionCode wrapped to 2 least significant bytes of int
     */
    public void setRegionCode(int rc){
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(rc);
        authorityID[2]=bb.get(2);
        authorityID[3]=bb.get(3);
    }
    
    /**
     * BusinessCode is 4th and 5th most significant bytes of AuthorityID
     * @return 2 bytes of BusinessCode wrapped to 2 least significant bytes of int
     */
    public Integer getBusinessCode(){
        int res = authorityID[4]<<8 | authorityID[5];
        return res;
    }
    
    /**
     * BusinessCode is 4th and 5th most significant bytes of AuthorityID
     * @param bc 2 bytes of BusinessCode wrapped to 2 least significant bytes of int
     */
    public void setBusinessCode(int bc){
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bc);
        authorityID[4]=bb.get(2);
        authorityID[5]=bb.get(3);
    }
    
    /**
     * AuthorityCode is 6th and 7th most significant bytes of AuthorityID
     * @return 2 bytes of AuthorityCode wrapped to 2 least significant bytes of int
     */
    public Integer getAuthorityCode(){
        int res = authorityID[6]<<8 | authorityID[7];
        return res;
    }
    
    /**
     * AuthorityCode is 6th and 7th most significant bytes of AuthorityID 
     * @param bc 2 bytes of AuthorityCode wrapped to 2 least significant bytes of int
     */
    public void setAuthorityCode(int bc){
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bc);
        authorityID[6]=bb.get(2);
        authorityID[7]=bb.get(3);
    }
    
    public long getOperationCode(){
      long res = authorityID[8]<<24|authorityID[9]<<16|authorityID[10]<<8|authorityID[11];
      return res;
    }
    
    public void setOperationCode(long oc){
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(oc);
        authorityID[8]=bb.get(4);
        authorityID[9]=bb.get(5);          
        authorityID[10]=bb.get(6);
        authorityID[11]=bb.get(7);          
    }
    
    public Long getSuplementalCode(){
      long res = authorityID[12]<<24|authorityID[13]<<16|authorityID[14]<<8|authorityID[15];
      return res;
    }
    
    public void setSuplementalCode(long oc){
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(oc);
        authorityID[12]=bb.get(4);
        authorityID[13]=bb.get(5);          
        authorityID[14]=bb.get(6);
        authorityID[15]=bb.get(7);          
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!AuthorityID.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final AuthorityID other = (AuthorityID) obj;
        if ((this.authorityID == null) ? (other.authorityID != null) : !Arrays.equals(this.authorityID,other.authorityID)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return authorityID.hashCode();
    }

    @Override
    public String toString() {
        return "AuthorityID{" + "authorityID=" + Hex.toHexString(authorityID) + '}';
    }
    
}
