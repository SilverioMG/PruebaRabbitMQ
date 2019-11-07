package net.atopecode.pruebarabbitmq.messagelistener;

import net.atopecode.pruebarabbitmq.config.RabbitMQConfig;
import net.atopecode.pruebarabbitmq.manager.RabbitMQManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

    //@Component
    public class ErrorMessageRecover implements MessageRecoverer {
        public final static Logger logger = Logger.getLogger(ErrorMessageRecover.class);
        public final static String HEADER_ERROR = "error";

        private RabbitMQManager rabbitMQManager;

        public ErrorMessageRecover(RabbitMQManager rabbitMQManager) {
            this.rabbitMQManager = rabbitMQManager;
        }

        //Este método se ejecuta cuando se intentó recibir un mensaje de forma errónea un nº máximo de veces y se procederá a borrarlo
        //del server 'RabbitMQ'.
        @Override
        public void recover(Message message, Throwable throwable) {
            //Map headers = new HashMap<>();
            String error = (String) message.getMessageProperties().getHeaders().get(HEADER_ERROR);
            error = (StringUtils.isEmpty(error)) ? (throwable.getMessage()) : (error + "\n\r" + throwable.getMessage());
            message.getMessageProperties().setHeader(HEADER_ERROR, error);

            //Se envía el mensaje a la cola de errores con el error en la 'Cabecera'.
            sendMessageToErrorQueue(message);
        }

        private void sendMessageToErrorQueue(Message message) {
            rabbitMQManager.sendMessageDirect(RabbitMQConfig.ExchangeName.ERROREXCHANGE, RabbitMQConfig.RoutingKey.ERRORMYROUTINGKEY, message, true, 5000);
        }
    }
