package com.apollocurrency.aplwallet.apl.exec.webui;

import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import java.io.File;
import java.util.concurrent.Callable;

/**
 * Class for handling zip of WebUI
 *
 * @author alukin\@gmail.com
 */
public class WebUiExtractor implements Callable<Boolean>{
    private static String webUiZipFile = "web-wallet.zip";
    private DirProvider dirProvider;

    public WebUiExtractor(DirProvider dirProvider) {
        this.dirProvider = dirProvider;
    }
    
    private File findZip(){
      File res = null;
      File dir = dirProvider.getBinDirectory();
      File[] filesList = dir.listFiles();
        for(File f : filesList){
            if(f.isFile()){
                System.out.println(f.getName());
                if(f.getName().equalsIgnoreCase(webUiZipFile)){
                 System.out.println("Found: "+f.getName());   
                }
            }
        }  
      return res;
    }
    
    private File findDest(){
        File res = null;
        return res;
    }
    
    public boolean checkInstalled() {
        boolean res;
        File zip = findZip();
        File dest = findDest();
        if(dest==null){
            res=false;
        }else{
            long lmd = dest.lastModified();
            long lmz =zip.lastModified();
            res=lmd<lmz;
        }
        return res;
    }

    public boolean install() {
        boolean res = true;
        return res;
    }

    @Override
    public Boolean call() throws Exception {
       return install(); 
    }
}
