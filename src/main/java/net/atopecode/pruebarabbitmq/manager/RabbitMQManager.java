package net.atopecode.pruebarabbitmq.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
 
import java.io.Serializable;
 
@Component
public class RabbitMQManager {
    private final static Logger logger = Logger.getLogger(RabbitMQManager.class);
 
    private RabbitTemplate rabbitTemplate;
 
    @Autowired
    public RabbitMQManager(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }
 
    /**
     * Este método envía un 'mensaje de texto' a un 'Exchange' de tipo 'direct' bindeado a una 'cola' con el tipo 'direct' recibido
     * como parámetro.
     * @param exchange
     * @param routingKey
     * @param message
     */
    public void sendDirect(String exchange, String routingKey, String message){
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
 
    public void sendDirectJsonObject(String exchange, String routingKey, JsonObject jsonObject){
        String message = jsonObject.toString();
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
 
    public void sendDirectJsonObject(String exchange, String routingKey, Object objectToSend){
        Gson gson = new Gson();
        JsonObject jsonObject = gson.toJsonTree(objectToSend).getAsJsonObject();
        String message = jsonObject.toString();
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
 
    /**
     * Este método envía un 'Objeto serializado en binario' a un 'Exchange' de tipo 'direct' bindeado a una 'cola' con el tipo 'direct' recibido
     * como parámetro.
     * Se utiliza para envíar 'docs, archivos de música...'.
     * @param exchange
     * @param routingKey
     * @param serializableObject
     */
    public void sendDirectBinary(String exchange, String routingKey, Serializable serializableObject){
        rabbitTemplate.convertAndSend(exchange, routingKey, serializableObject);
    }

    /**
     * Este método envía un mensaje dejando escoger si se debe borrar o no cuando se para el 'Servicio de RabbitMQ'.
     * @param exchange
     * @param routingKey
     * @param message
     * @param persistent
     */
    public void sendMessageDirect(String exchange, String routingKey, Message message, boolean persistent, int delay){
        if(message == null){
            return;
        }

        MessageDeliveryMode deliveryMode = (persistent) ? MessageDeliveryMode.PERSISTENT : MessageDeliveryMode.NON_PERSISTENT;
        message.getMessageProperties().setDeliveryMode(deliveryMode);
        message.getMessageProperties().setDelay(delay);
        rabbitTemplate.send(exchange, routingKey, message);
    }
}