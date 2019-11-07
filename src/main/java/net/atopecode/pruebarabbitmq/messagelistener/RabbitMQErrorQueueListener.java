package net.atopecode.pruebarabbitmq.messagelistener;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
 
public class RabbitMQErrorQueueListener implements MessageListener {
    private static Logger logger = Logger.getLogger(RabbitMQErrorQueueListener.class);
    public static boolean deleteErrorMessage = false;
 
    //Este mensaje se ejecuta cuando se recibe un mensaje del server de 'RabbitMQ'.
    @Override
    public void onMessage(Message message) {
        logger.info("Recibido message de la cola de ERRORES: " + new String(message.getBody()));
 
        if(!deleteErrorMessage){
            throw new RuntimeException("Mensaje de ERROR devuelto a la cola de ERRORES: " + message.toString());
        }
    }
}