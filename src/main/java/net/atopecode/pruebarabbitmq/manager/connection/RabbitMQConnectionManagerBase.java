package net.atopecode.pruebarabbitmq.manager.connection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.Serializable;
import java.util.Map;

public class RabbitMQConnectionManagerBase {

    protected ConnectionFactory getConnectionFactory(ConnectionValues connectionValues) {
        CachingConnectionFactory ccf = new CachingConnectionFactory(connectionValues.getHost(), connectionValues.getPort());
        ccf.setUsername(connectionValues.getUserName());
        ccf.setPassword(connectionValues.getPassword());

        if(StringUtils.isNotEmpty(connectionValues.getVirtualHost())){
            ccf.setVirtualHost(connectionValues.getVirtualHost());
        }

        return ccf;
    }

    public AmqpAdmin getAmqpAdmin(ConnectionFactory connectionFactory){
        return new RabbitAdmin(connectionFactory);
    }

    public RabbitTemplate getRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        RabbitTemplate rabbitTemplate = admin.getRabbitTemplate();
        return rabbitTemplate;
    }

    /**
     * Este método envía un 'mensaje de texto' a un 'Exchange' de tipo 'direct' bindeado a una 'cola' con el tipo 'direct' recibido
     * como parámetro.
     * @param exchange
     * @param routingKey
     * @param message
     */
    public void send(RabbitTemplate rabbitTemplate, String exchange, String routingKey, String message){
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void send(RabbitTemplate rabbitTemplate, String exchange, String routingKey, JsonObject jsonObject){
        String message = jsonObject.toString();
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void sendJsonObject(RabbitTemplate rabbitTemplate, String exchange, String routingKey, Object objectToSend){
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
    public void sendBinary(RabbitTemplate rabbitTemplate, String exchange, String routingKey, Serializable serializableObject){
        rabbitTemplate.convertAndSend(exchange, routingKey, serializableObject);
    }

    /**
     * Este método envía un mensaje dejando escoger si se debe borrar o no cuando se para el 'Servicio de RabbitMQ'.
     * @param exchange
     * @param routingKey
     * @param message
     * @param persistent
     */
    public void sendMessage(RabbitTemplate rabbitTemplate, String exchange, String routingKey, Message message, boolean persistent, int delay){
        if(message == null){
            return;
        }

        MessageDeliveryMode deliveryMode = (persistent) ? MessageDeliveryMode.PERSISTENT : MessageDeliveryMode.NON_PERSISTENT;
        message.getMessageProperties().setDeliveryMode(deliveryMode);
        message.getMessageProperties().setDelay(delay);
        rabbitTemplate.send(exchange, routingKey, message);
    }

    /**
     * Este método envía un mensaje dejando escoger si se debe borrar o no cuando se para el 'Servicio de RabbitMQ'.
     * @param exchange
     * @param routingKey
     * @param message
     * @param persistent
     */
    public void sendMessage(RabbitTemplate rabbitTemplate, String exchange, String routingKey, Message message, boolean persistent, int delay,
                            RabbitTemplate.ReturnCallback returnCallback, Map<String, Object> headers){
        if(message == null){
            return;
        }

        MessageDeliveryMode deliveryMode = (persistent) ? MessageDeliveryMode.PERSISTENT : MessageDeliveryMode.NON_PERSISTENT;
        message.getMessageProperties().setDeliveryMode(deliveryMode);
        message.getMessageProperties().setDelay(delay);
        if(returnCallback != null){
            rabbitTemplate.setReturnCallback(returnCallback);
            rabbitTemplate.setMandatory(true);
        }
        if(headers != null){
            for(Map.Entry<String, Object> entry: headers.entrySet()){
                message.getMessageProperties().setHeader(entry.getKey(), entry.getValue());
            }
        }

        rabbitTemplate.send(exchange, routingKey, message);
    }
}