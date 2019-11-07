package net.atopecode.pruebarabbitmq.utils;

import com.amazonaws.util.IOUtils;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class ResourcesFile {
     
    private String pathResourceFile;
     
    public String getPathResourceFile(){ return pathResourceFile; }
    public void setPathResourceFile(String value){ pathResourceFile =value; }
     
    public ResourcesFile(String pathResourceFile){
        this.pathResourceFile = pathResourceFile;
    }
 
    public InputStream loadInputStreamFromResourceFile() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(pathResourceFile);
        return inputStream;
    }
     
    public byte[] loadResourceFileBytes() throws Exception{
        byte[] data = null;
        InputStream inputStream = null;
         
        try{
            inputStream = getClass().getClassLoader().getResourceAsStream(pathResourceFile);
            data = IOUtils.toByteArray(inputStream);
        }
        catch(Exception ex){
            throw new Exception(ex);
        }
        finally{
            if(inputStream != null){
                inputStream.close();
            }
        }
         
        return data;
    }
     
    public String loadResourceFileBase64() throws Exception{
        byte[] data = loadResourceFileBytes();
        String file = Base64.getEncoder().encodeToString(data);
         
        return file;
    }
     
    public JsonObject getJsonBodyLoadExcel(String fileName, boolean setDateFileName) throws Exception{
        String file = loadResourceFileBase64();
 
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yy_hh:mm:ss");
        if(setDateFileName){
            fileName += "_" + dateFormat.format(new Date());
        }
         
        JsonObject body = new JsonObject();
        body.addProperty("file", file);
        body.addProperty("filename", fileName);
         
        return body;
    }
     
}