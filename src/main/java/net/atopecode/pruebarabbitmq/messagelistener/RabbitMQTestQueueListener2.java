package net.atopecode.pruebarabbitmq.messagelistener;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class RabbitMQTestQueueListener2 implements MessageListener {
    private final static Logger logger = Logger.getLogger(RabbitMQTestQueueListener2.class);
    public static boolean deleteMessage = true;

    @Override
    public void onMessage(Message message) {
        logger.info("Recibido message de la cola de Test para Listener2: " + new String(message.getBody()));

        if(!deleteMessage){
            //Si en el Listener se configura 'simpleMessageListenerContainer.setDefaultRequeueRejected(false)' lanzando tanto
            //una 'RuntimeException' como una 'AmqpRejectAndDontRequeueException' se elimina el mensaje de su cola original y se
            //reenvia al exchange configurado en los atributos 'deadletter' de la cola.
            //Si en el Listener se configura 'simpleMessageListenerContainer.setDefaultRequeueRejected(true)' si se lanza una
            //'RuntimeException' el mensaje se vuelve a encolar en su cola original. Solo si se lanza una 'AmqpRejectAndDontRequeueException'
            //el mensaje se elimina de la cola original y se reenvia según los parámetro deadletter de la cola.
            //En ambos casos, sino se lanza Exception y se procesa correctamente el mensaje, se elimina de la cola original y no se reenvía a ninguna otra.

            throw new RuntimeException("Mensaje enviado a la cola de 'ERRORES: " + message.toString());
            //throw new AmqpRejectAndDontRequeueException("Mensaje eliminado a la fuerza de la cola.");
        }

        //El mensaje se procesa con normalidad, se elimina de su cola y no se envía a ninguna más.
    }
}
