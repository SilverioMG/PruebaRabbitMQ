package net.atopecode.pruebarabbitmq.messagelistener;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
 
public class RabbitMQMessageListener implements MessageListener {
    private final static Logger logger = Logger.getLogger(RabbitMQMessageListener.class);
    @Override
    public void onMessage(Message message) {
        String payload = new String(message.getBody());
        logger.info("RabbitMQMessage: " + payload);
        if(StringUtils.equals(payload.toLowerCase(), "no eliminar")){
            //Al lanzar la 'Exception' el mensaje no se elimnina del server 'RabbitMQ'. El server vuelve a mandar el mensaje una y
            //otra vez hasta que se procesa correctamente o se lance una Exception del tipo 'AmqpRejectAndDontRequeueException'.
            throw new RuntimeException("No eliminar mensaje del server de RabbitMQ.");
            //throw new AmqpRejectAndDontRequeueException("Mensaje eliminado a la fuerza de la cola.");
        }
    }
 
    @Override
    public void containerAckMode(AcknowledgeMode mode) {

    }
}