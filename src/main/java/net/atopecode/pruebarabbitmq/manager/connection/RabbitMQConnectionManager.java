package net.atopecode.pruebarabbitmq.manager.connection;


import org.apache.log4j.Logger;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class RabbitMQConnectionManager extends RabbitMQConnectionManagerBase {
    private final static Logger logger = Logger.getLogger(RabbitMQConnectionManager.class);
    private final static String exchangeEventPublishName = "events"; //'Exchange' donde se publican los mensajes.
    private final static String exchangeEventName = "event"; //'Exchange' que filtra los mensajes recibidos.
    private final static String exchangeEventErrorName = "event.error"; //'Exchange' que recoge los mensajes rechazados de la cola que escucha al 'eventExchage'.
    private final static String headerBindingEventExchangeWithPublishExchange = "action"; //Nombre de la cabecera para filtrar los mensajes que se envían desde el 'Exchange' que publica los mensajes.
    private final static String valueHeaderBindingEventExchangeWithPublishExchange = "update"; //Valor de la cabecera para filtrar los mensajes que se envían desde el 'Exchange' que publica los mensajes.
    private final static String queueEventName = "event"; //Queue donde se envían los mensajes que se reciben en el 'Exchange' 'event'.
    private final static String queueEventErrorName = "event.error"; //Queue donde se envían los mensajes procesados incorrectamente recibidos desde la cola 'event'.

    private boolean localConnection = true;
    private ConnectionFactory connectionFactory;

    @PostConstruct
    public void postConstruct(){
        ConnectionValues connectionValues;
        if (!localConnection){
            connectionValues = getConnectionValues();
        }
        else{
            connectionValues = getConnectionValuesTest();
        }

        this.connectionFactory = getConnectionFactory(connectionValues);
        initialize();
    }
    
    //TODO... Completar este método para que cuando se cierre el 'ServicioWeb' se desconecte del server de RabbitMQ, sino queda la conexión
    //TODO... del Listener abierta.
    /*@PreDestory
    private void preDestory(){
        if(this.connectionFactory != null){
            this.connectionFactory.close();
        }
    }*/

    /**
     * Este método crea los 'Exchange', 'Queues' y 'Bindings' necesarios si no existen.
     */
    private void initialize(){
        if(connectionFactory != null){
            //TODO... Los VirtualHost deben crearse a mano en el server de RabbitMQ.
            configureExchangeEvent(connectionFactory, exchangeEventPublishName, headerBindingEventExchangeWithPublishExchange, valueHeaderBindingEventExchangeWithPublishExchange);
            configureExchangeEventError(connectionFactory);
            configureListeners(connectionFactory);
        }
    }

    @Override
    protected ConnectionFactory getConnectionFactory(ConnectionValues connectionValues){
        CachingConnectionFactory ccf = (CachingConnectionFactory)super.getConnectionFactory(connectionValues);
        ccf.setPublisherReturns(true); //Para poder saber si se envío un mensaje y se enrutó correctamente.
        //ccf.setPublisherConfirms(true);//Para poder saber si el mensaje se consumió correctamente.

        return ccf;
    }
    public static ConnectionValues getConnectionValues(){
        String host = "127.0.0.1";
        String userName = "rabbitmq";
        String password = "rabbitmq";
        String virtualHost = "virtualhost-produccion";
        return new ConnectionValues(host, userName, password, virtualHost);
    }

    public static ConnectionValues getConnectionValuesTest(){
        String host = "127.0.0.1";
        String userName = "rabbitmq";
        String password = "rabbitmq";
        String virtualHost = "virtualhost-test";
        return new ConnectionValues(host, userName, password, virtualHost);
    }

    private void configureExchangeEvent(ConnectionFactory connectionFactory, String exchangeToBinding,
                                        String headerExchangeToBinding, String valueHeaderExchangeToBinding){
        try{
            AmqpAdmin admin = getAmqpAdmin(connectionFactory);
            HeadersExchange eventExchange = new HeadersExchange(exchangeEventName, true, false);
            admin.declareExchange(eventExchange);

            //Se bindea el exchange que recibe los mensajes contra el 'Exchange' que los envía para filtrar los mensajes (se filtra por un campo del header del mensaje.).
            //NOTA.- El 'Exchange' contra el que se quiere bindear el 'eventExchange' debe estar creado en el server de RabbitMQ, lo crea el servicio emisor.
            Binding bindingToExchage = BindingBuilder.bind(eventExchange).to(new HeadersExchange(exchangeToBinding))
                    .where(headerExchangeToBinding).matches(valueHeaderExchangeToBinding); //.exists(); (Para recibir todos lo eventos).
            admin.declareBinding(bindingToExchage);

            //'Queue' donde se reciben los mensajes del 'Exchange' 'eventExchange'. Si se rechaza un mensaje se envía al 'Exchange' 'eventError'.
            Queue eventQueue = QueueBuilder.durable(queueEventName).withArgument("x-dead-letter-exchange", exchangeEventErrorName).build();

            admin.declareQueue(eventQueue);

            //Por defecto todos los mensajes enviados por 'eventExchange' que tenga cabecera 'action' (sin importar su valor) se envían a la cola 'event'.
            Binding bindingToEventQueue = BindingBuilder.bind(eventQueue).to(eventExchange).where(headerExchangeToBinding).exists();
            admin.declareBinding(bindingToEventQueue);
        }
        catch (Exception ex){
            ex.printStackTrace();
            logger.error("RabbitMQ: No se ha podido configurar el 'ExchangeEvent'." + ex.getMessage());
        }
    }

    private void configureExchangeEventError(ConnectionFactory connectionFactory){
        try{
            AmqpAdmin admin = getAmqpAdmin(connectionFactory);
            HeadersExchange errorExchange = new HeadersExchange(exchangeEventErrorName, true, false);
            admin.declareExchange(errorExchange);

            Queue errorQueue = QueueBuilder.durable(queueEventErrorName).build();
            admin.declareQueue(errorQueue);

            Binding bindingErrorQueue = BindingBuilder.bind(errorQueue).to(errorExchange).where(headerBindingEventExchangeWithPublishExchange).exists();
            admin.declareBinding(bindingErrorQueue);
        }
        catch (Exception ex){
            ex.printStackTrace();
            logger.error("RabbitMQ: No se ha podido configurar el 'ExchangeEventError': " + ex.getMessage());
        }
    }

    private void configureListeners(ConnectionFactory connectionFactory){
        try{
            SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
            simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
            simpleMessageListenerContainer.setQueueNames(queueEventName);
            simpleMessageListenerContainer.setMessageListener((Message message) -> {
                String payload = new String(message.getBody());
                Map<String, Object> headers = message.getMessageProperties().getHeaders();
                logger.info("Mensaje recibido en la cola '" + queueEventName + "': " + payload);
                //throw new RuntimeException("Mensaje rechazado y enviado a la cola de errores.");
            });

            //La cola de este 'Listener' tiene atributos 'deadletter' para que se reenvie a otra cola en caso de error.
            //Hay que utilizar 'setDefaultRequeueRejected(false);' para que el mensaje se envíe a la otra cola (atributos 'deadletter')
            //tanto si se lanza una 'RuntimeException' como una 'AmqpRejectAndDontRequeueException', sino solo funciona el 'deadletter' con una 'AmqpRejectAndDontRequeueException'.
            simpleMessageListenerContainer.setDefaultRequeueRejected(false);
            simpleMessageListenerContainer.setAcknowledgeMode(AcknowledgeMode.AUTO);
            // Should be restricted to '1' to maintain data consistency.
            simpleMessageListenerContainer.setConcurrentConsumers(1);
            simpleMessageListenerContainer.start();
        }
        catch(Exception ex){
            ex.printStackTrace();;
            logger.error("RabbitMQ: No se han podido configurar los 'Listeners'.");
        }
    }

    public void sendMessage(Message message, String exchangeName, Map<String, Object> headers){
        RabbitTemplate.ReturnCallback returnCallback = (Message messageSended, int replyCode, String replyText, String exchange, String routingKey) -> {
            logger.info("Mensaje no enrutado con id: " + getMessageXDocumentId(message));
            //TODO... Guardar mensaje no enrutado.
        };

        try{
            super.sendMessage(getRabbitTemplate(connectionFactory), exchangeName, null, message,true, 500, returnCallback, headers);
        }
        catch(AmqpConnectException ex){
            logger.error("No se ha podido conectar con el servidor de RabbitMQ para enviar el mensaje con id: " + getMessageXDocumentId(message));
            //TODO... Guardar mensaje no enrutado.
        }
        catch(Exception ex){
            String payload = new String(message.getBody());
            logger.error("No se ha podido enviar al server de RabbitMQ  el mensaje con id: " + getMessageXDocumentId(message));
            //TODO... Guardar mensaje no enrutado.
        }
    }

    public String getMessageXDocumentId(Message message){
        String idXDoc = null;
        if(message == null){
            return idXDoc;
        }

        try{
            idXDoc = (String)message.getMessageProperties().getHeaders().get("id");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        return idXDoc;
    }
}
