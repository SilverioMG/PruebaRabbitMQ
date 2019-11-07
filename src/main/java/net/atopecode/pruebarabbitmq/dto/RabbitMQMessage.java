package net.atopecode.pruebarabbitmq.dto;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
 
import java.io.Serializable;
 
public class RabbitMQMessage implements Serializable {
    private String user;
    private String type;
    private String message;
 
    public RabbitMQMessage(){
 
    }
 
    public RabbitMQMessage(String user, String type, String message) {
        this.user = user;
        this.type = type;
        this.message = message;
    }
 
    public String getUser() {
        return user;
    }
 
    public String getType() {
        return type;
    }
 
    public String getMessage() {
        return message;
    }
 
    public void setUser(String user) {
        this.user = user;
    }
 
    public void setType(String type) {
        this.type = type;
    }
 
    public void setMessage(String message) {
        this.message = message;
    }
 
    public JsonObject toJson(){
        Gson gson = new Gson();
        return gson.toJsonTree(this).getAsJsonObject();
    }
 
    public static RabbitMQMessage fromJson(JsonObject jsonObject){
        RabbitMQMessage result = null;
        if(jsonObject == null){
            return result;
        }
 
        Gson gson = new Gson();
        result = gson.fromJson(jsonObject, RabbitMQMessage.class);
        return result;
    }
}