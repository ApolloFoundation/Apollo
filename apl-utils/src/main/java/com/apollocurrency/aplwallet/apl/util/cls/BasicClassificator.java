package com.apollocurrency.aplwallet.apl.util.cls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 *
 * @author al
 */
public class BasicClassificator {
    ClsItem root;
    private final ItemValueComparator compS = new StringValueComparator();
    private final ItemValueComparator compN = new NumbericValueComparator();
    private ItemValueComparator comp = compS;
    
    public BasicClassificator() {
        root = new ClsItem();
    }
    
    public static BasicClassificator fromJson(String json) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        BasicClassificator res = new BasicClassificator();
        res.root = mapper.readValue(json, ClsItem.class);
        return res;
    }
    
    public static BasicClassificator fromJsonFile(String jsonFilePath) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        BasicClassificator res = new BasicClassificator();
        FileInputStream jsonIs = new FileInputStream(jsonFilePath);
        res.root = mapper.readValue(jsonIs, ClsItem.class);
        return res;
    }
    
    public static BasicClassificator fromJsonResource(String jsonFilePath) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        BasicClassificator res = new BasicClassificator();
        ClassLoader classLoader = BasicClassificator.class.getClassLoader();
        InputStream jsonIs = classLoader.getResourceAsStream(jsonFilePath);
        res.root = mapper.readValue(jsonIs, ClsItem.class);
        return res;
    }  
    public String toJson() throws JsonProcessingException{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(root);     
    }
    
    public ClsItem getItem(String ... names){
        ClsItem res = new ClsItem();
        res.empty = true;
        int sz = names.length;
        if(names.length==0 || (names.length==1 &&(names[0]==null || names[0].isEmpty()))){
            return root;
        }
        ClsItem cur = root;
        boolean found1 = false;
        for(int lvl=0; lvl<names.length; lvl++){
            boolean found2 = false;
            String n = names[lvl];
            for(ClsItem item: cur.chlds){
                if(item.name.equalsIgnoreCase(n)){
                    cur=item;
                    if(lvl==sz-1){//last level
                        res = item;
                        found1=true;
                    }
                    found2=true;
                    break;
                }
            }
            if(!found2 || found1) { 
                break;
            };
        }
        return res;       
    }
    
    public String getValue(String ... names){
        ClsItem cur = getItem(names);
        return cur.value;
    }
    
    public String getDescription(String ... names){
        ClsItem cur = getItem(names);
        return cur.value;
    }
    
    public List<ClsItem> getAllChilds(String ... names){
        ClsItem cur = getItem(names);
        return cur.chlds;
    }
    
    private ClsItem findByValueInternal(String value, String ... names){
        ClsItem res = new ClsItem();
        res.empty = true;
        ClsItem cur = getItem(names);
        for(ClsItem i: cur.chlds){
            if(comp.compare(i.value,value)==0){
                res=i;
                break;
            }
        }
        return res;
    }
    public ClsItem findByValue(String value, String ... names){
         comp = compS;
         return findByValueInternal(value, names);
    }
    public ClsItem findByValue(Long value, String ... names){
        comp=compN;
        return findByValueInternal(value.toString(), names);
    }
    
    public ClsItem findByValue(Integer value, String ... names){
        comp=compN;
        return findByValueInternal(value.toString(), names);
    }
       
    public BasicClassificator setValueComparator(ItemValueComparator comp){
        this.comp=comp;
        return this;
    }
}
