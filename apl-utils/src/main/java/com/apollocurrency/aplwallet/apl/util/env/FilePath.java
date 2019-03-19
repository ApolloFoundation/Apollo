package com.apollocurrency.aplwallet.apl.util.env;

import java.io.File;

/**
 *
 * @author alukin@gmail.com
 */
public class FilePath {

    private String path;
    File f;
    public FilePath(String path) {
        this.path = path;
        f= new File(path);
    }
    
    public FilePath(File f){
        this.f=f;
        path = f.getAbsolutePath();
    }
    
    public String getExtension() {
        int i = path.lastIndexOf('.');
        if (i >= 0) {
            return path.substring(i + 1);
        } else {
            return "";
        }
    }
    
    public String addExtension(String ext) {
        path=path+"."+ext;
        f=new File(path);
        return path;
    }    
    
    public String getName(){
        int i = path.lastIndexOf('.');
        if (i >= 0) {
            return path.substring(0,i);
        } else {
            return path;
        }
    }
    
    public String getPath(){
       return f.getParent();
    }
    
    public String getFileName(){
        return f.getName();
    }
    
    public String getAbsPath(){
       return f.getAbsolutePath();
    }
    
    public boolean exists(){
       return f.exists();
    }
    
    public boolean canRead(){
       return f.canRead();
    }
    
    public boolean canWrite(){
        return f.canWrite();
    }
}
