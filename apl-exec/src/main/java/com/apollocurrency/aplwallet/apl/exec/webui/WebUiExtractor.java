package com.apollocurrency.aplwallet.apl.exec.webui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

/**
 * Class for handling zip of WebUI
 *
 * @author alukin\@gmail.com
 */
public class WebUiExtractor implements Callable<Boolean>{
    private static String webUiZipFile = "web-wallet.zip";
    private static String testUiZipFile = "apl-html";
    private DirProvider dirProvider;
    public final static String WEB_UI_DIR="webui";

    public WebUiExtractor(DirProvider dirProvider) {
        this.dirProvider = dirProvider;
    }
    
    private File findWebUiZip() throws FileNotFoundException{
      File res = null;
      File dir = new File(DirProvider.getBinDir().toAbsolutePath()+File.separator+"lib");
      if(!dir.exists()){//we are in dev. env or tests
          dir = new File(DirProvider.getBinDir().toAbsolutePath()+"/apl-exec/target/lib");
      }
      if(!dir.exists()){
          throw new FileNotFoundException(dir.getAbsolutePath());
      }
      File[] filesList = dir.listFiles(new FileFilter(){
          @Override
          public boolean accept(File pathname) {
              return pathname.getName().endsWith("zip");
          }
      });
        for(File f : filesList){
            if(f.isFile()){
                if(f.getName().equalsIgnoreCase(webUiZipFile)){
                  res = new File(dir.getAbsolutePath()+File.separator+f.getName());
                  break;
                }
            }
        }  
      return res;
    }
    
    private File findTestUiZip() throws FileNotFoundException{
      File res = null;
      File dir = new File(DirProvider.getBinDir().toAbsolutePath()+File.separator+"lib");
      if(!dir.exists()){//we are in dev. env or tests
          dir = new File(DirProvider.getBinDir().toAbsolutePath()+"/apl-exec/target/lib");
      }
      if(!dir.exists()){
          throw new FileNotFoundException(dir.getAbsolutePath());
      }
      File[] filesList = dir.listFiles(new FileFilter(){
          @Override
          public boolean accept(File pathname) {
              return pathname.getName().endsWith("zip");
          }
      });
        for(File f : filesList){
            if(f.isFile()){
                if(f.getName().startsWith(testUiZipFile)){
                  res = new File(dir.getAbsolutePath()+File.separator+f.getName());
                  break;
                }
            }
        }  
      return res;
    }
    
    private File findDest(){
        Path res = dirProvider.getAppBaseDir().resolve(WEB_UI_DIR);
        return res.toFile();
    }
    
    public boolean checkInstalled() throws FileNotFoundException {
        boolean res;
        File zip = findWebUiZip();
        File dest = findDest();
        if(!dest.exists()){
            res=false;
        }else{
            //TODO: check version of UI somehow, depends on zip name in our Artifactory
            long lmd = dest.lastModified();
            long lmz =zip.lastModified();
            res=lmd<lmz;
        }
        return res;
    }
    
    public void removeDir(String path){
        try {
            Files.walk(Paths.get(path))
                    .map(Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);
        } catch (IOException ex) {
        }
    }
    
    public boolean install() throws FileNotFoundException, IOException {
        boolean res=true;
        if(!checkInstalled()){
            File dest = findDest();
            if(dest.exists()){
              removeDir(dest.getAbsolutePath());
            }
            res=Zip.extract(findWebUiZip().getAbsolutePath(), dest.getAbsolutePath());
            res=res&Zip.extract(findTestUiZip().getAbsolutePath(), dest.getAbsolutePath());
        }
        return res;
    }

    @Override
    public Boolean call() throws Exception {
       return install(); 
    }
}
