package net.atopecode.pruebarabbitmq.config;


import net.atopecode.pruebarabbitmq.manager.RabbitMQManager;
import net.atopecode.pruebarabbitmq.messagelistener.ErrorMessageRecover;
import net.atopecode.pruebarabbitmq.messagelistener.RabbitMQDeadLetterQueueListener;
import net.atopecode.pruebarabbitmq.messagelistener.RabbitMQErrorQueueListener;
import net.atopecode.pruebarabbitmq.messagelistener.RabbitMQMessageListener;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;


@Configuration
public class RabbitMQConfig {

    //Los mensajes se envían al 'Exchange', que debe estar 'Bindeado' contra una 'Queue'. Según
    //la configuración del 'Binding'
    //se envían los mensajes a unas colas u otras. Por defecto cuando se lee un mensaje de una
    //'Queue' se elimina de ésta excepto cuando el 'Listener' lanza una 'Exception' (RuntimeException) (si se lanza
    //una 'AmqpRejectAndDontRequeueException' en vez de cualquier otra 'Exception', se ordena al
    //server de RabbitMQ que elimine el mensaje aunque su recepción
    //haya fallado).
    //Para que los los Exchanges y las Colas (con sus Bindings) no se eliminen si se para el servicio de RabbitMQ
    //deben crearse como 'durables'.
    //Para que los mensajes de las 'Colas' no se eliminen cuando se para el servicio de RabbitMQ, la cola debe ser de tipo
    //durable y cuando se envió el mensaje (al exchange que lo asignó a dicha cola) el envío debe ser de tipo 'persistent'
    //para que guarde el mensaje en disco hasta que se reciba correctamente (por defecto el método 'rabbitTemplate.convertAndSend()'
    //ya lo envía como 'persistent').


    //Si no existe el 'Exchange' en el server de 'RabbitMQ', se crea uno.
    @Bean
    public Exchange myExchange(){
        return ExchangeBuilder.directExchange(ExchangeName.MYEXCHANGE)
                .durable(true)
                .build();
    }
    @Bean
    public Exchange errorExchange(){
        return ExchangeBuilder.directExchange(ExchangeName.ERROREXCHANGE)
                .durable(true)
                .build();
    }
    @Bean
    public Exchange deadLetterExchange(){
        return ExchangeBuilder.directExchange(ExchangeName.DEADLETTEREXCHANGE)
                .durable(true)
                .build();
    }


    //Si la 'Queue' no existe en el server de 'RabbitMQ', se crea una.
    @Bean
    public Queue myQueue(){
        return new Queue(QueueName.MYQUEUE, true);
    }
    @Bean
    public Queue directQueue() { return new Queue(QueueName.DIRECTQUEUE, true); }
    @Bean
    public Queue errorQueue() { return new Queue(QueueName.ERRORQUEUE, true); }
    @Bean
    public Queue deadLetterQueue() {
        //Se crea una cola con atributos que indican que cuando se rechaza un mensaje se envíe directamente a otra cola sin necesidad
        //de utilizar una clase 'Recover' en el 'Listener'.
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("x-dead-letter-exchange", ExchangeName.ERROREXCHANGE);
        attributes.put("x-dead-letter-routing-key", RoutingKey.ERRORMYROUTINGKEY);
        return new Queue(QueueName.DEADLETTERQUEUE, true, false, false, attributes);
    }

    //Si no existe el 'Binding' en el server de 'RabbitMQ', se crea uno.
    @Bean
    public Binding myBinding(){
        //return new Binding(QueueName.MYQUEUE, Binding.DestinationType.QUEUE, ExchangeName.MYEXCHANGE, RoutingKey.MYROUTINGKEY, null);
        return BindingBuilder
                .bind(myQueue())
                .to(myExchange())
                .with(RoutingKey.MYROUTINGKEY)
                .noargs();
    }

    //Si no existe el 'Binding' en el server de 'RabbitMQ', se crea uno.
    @Bean
    public Binding errorBinding(){
        return BindingBuilder
                .bind(errorQueue())
                .to(errorExchange())
                .with(RoutingKey.ERRORMYROUTINGKEY)
                .noargs();
    }

    @Bean
    public Binding deadLetterBinding(){
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(RoutingKey.DEADLETTERROUTINGKEY)
                .noargs();
    }

    //Se configura la conexión de 'RabbitMQ.messageListenerContainer()'.
    @Bean
    @Primary //Si existe otra conexión al server de 'RabbitMQ', se inyecta este Bean por defecto a no ser que se utilice '@Quialifie("nombreBean")' en la inyección.
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(ConnectionInfo.SERVER);
        cachingConnectionFactory.setUsername(ConnectionInfo.USER_NAME);
        cachingConnectionFactory.setPassword(ConnectionInfo.PASSWORD);
        //cachingConnectionFactory.setVirtualHost("/"); //Por defecto.
        return cachingConnectionFactory;
    }

    //Se configura la clase que recibe los mensajes y de que cola. Se pueden declarar tantos @Bean 'Listener' como sean necesarios.
    //Todas las colas de este 'Listener' comparten el mismo 'canal', para que sea mas efectiva la comunicación es mejor declarar un 'Listener'
    //por 'cola' y cada una tendrá su propio 'canal'.
    @Bean
    public MessageListenerContainer messageListenerContainer() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
        simpleMessageListenerContainer.setQueues(myQueue(), directQueue()); //Todas estas colas comparten el mismo canal, para que cada una tenga un canal distinto debe declararse un @Bean Listener por cada una.
        simpleMessageListenerContainer.setMessageListener(new RabbitMQMessageListener());

        //Por defecto 'DefaultRequeueRejected' vale 'true' (no hace falta asignarlo), en el 'Listener' cuando se recibe un mensaje si se lanza 'Excepcion'
        //no se elimina el mensaje de la cola en el server. Si no se lanza 'Excepcion' quiere decir que se recibió correctamente
        //y el 'server de RabbitMQ' elimina el mensaje de la cola.
        simpleMessageListenerContainer.setDefaultRequeueRejected(true);
        //simpleMessageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        //simpleMessageListenerContainer.setDefaultRequeueRejected(false); //Los mensajes siempre ser borran del server RabbitMQ (incluso cuando se lanza Exception).

        //Cuando se rechaza un mensaje se configura un retardo para volver a procesarlo y un nº máximo de reintentos (luego se borra del server RabbitMQ).
        //Sino se añade un 'RetryInterceptor' y se tiene 'setDefaultRequeueRejected(true)' cada vez que se lanza una 'RuntimeException' en el 'Listener',
        //vuelve al server de RabbitMQ y lo volvemos a recibir en el 'Listener' de manera infinita hasta que se consigue procesar (puede ser un bucle infinito).
        //Con un 'RetryInterceptor' y un 'MessageRecover' se puede envíar el mensaje después de 'n' intentos a otra cola para que el servicio
        //que publica mensajes pueda comprobar los que fallaron en la recepción.
        simpleMessageListenerContainer.setAdviceChain(new Advice[]{
                RetryInterceptorBuilder.StatefulRetryInterceptorBuilder.stateless()
                        .maxAttempts(5)
                        .backOffOptions(3000, 2, 10000)
                        .recoverer(errorMessageRecover()) //Se ejecuta después del último intento para realizar una acción.
                        .build()
        });

//        // Set Exclusive Consumer 'ON'
//        simpleMessageListenerContainer.setExclusive(true);
//        // Should be restricted to '1' to maintain data consistency.
//        simpleMessageListenerContainer.setConcurrentConsumers(1);

        return simpleMessageListenerContainer;
    }

    //Otro Listener para la cola de errores (en teoría este listener debe estar en el servicio que envía el mensaje, porque será el responsable
    //de decidir que se hace con cada mensaje erróneo (se reenvía, se procesa manualmente...).
    @Bean
    public MessageListenerContainer messageListenerContainerErrorQueue() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
        simpleMessageListenerContainer.setQueues(errorQueue());
        simpleMessageListenerContainer.setMessageListener(new RabbitMQErrorQueueListener());

        //Por defecto 'DefaultRequeueRejected' vale 'true' (no hace falta asignarlo), en el 'Listener' cuando se recibe un mensaje si se lanza 'Excepcion'
        //no se elimina el mensaje de la cola en el server. Si no se lanza 'Excepcion' quiere decir que se recibió correctamente
        //y el 'server de RabbitMQ' elimina el mensaje de la cola.
        simpleMessageListenerContainer.setDefaultRequeueRejected(true);

        return simpleMessageListenerContainer;
    }
    //Otro Listener para la cola que está configurada con atributos 'dead-letter' para que cuando se rechaza un mensaje se envíe
    //directamente a otra cola sin tener que utilizar una clase 'Recover' en este Listener (se configura el redireccionamiento/deadletter al crear la cola).
    @Bean
    public MessageListenerContainer messageListenerContainerDeadLetterQueue() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
        simpleMessageListenerContainer.setQueues(deadLetterQueue());
        simpleMessageListenerContainer.setMessageListener(new RabbitMQDeadLetterQueueListener());

        //La cola de este 'Listener' tiene atributos 'deadletter' para que se reenvie a otra cola en caso de error.
        //Hay que utilizar 'setDefaultRequeueRejected(false);' para que el mensaje se envíe a la otra cola (atributos deadletter)
        //tanto si se lanza una 'RuntimeException' como una 'AmqpRejectAndDontRequeueException', sino solo funciona el 'deadletter' con una 'AmqpRejectAndDontRequeueException'.
        simpleMessageListenerContainer.setDefaultRequeueRejected(false);

        return simpleMessageListenerContainer;
    }



    //Recuperación de mensajes cuando se lanza una 'Exception' desde el 'Listener':
    @Autowired
    private RabbitMQManager rabbitMQManager;
    @Bean
    public MessageRecoverer errorMessageRecover(){
        return new ErrorMessageRecover(rabbitMQManager);
    }



    public static class ExchangeName{
        public final static String MYEXCHANGE = "MyExchange";
        public final static String ERROREXCHANGE = "ErrorExchange";
        public final static String DEADLETTEREXCHANGE = "DeadLetterExchange";
    }

    public static class QueueName{
        public final static String MYQUEUE = "MyQueue";
        public final static String DIRECTQUEUE = "DirectQueue";
        public final static String ERRORQUEUE = "ErrorQueue";
        public final static String DEADLETTERQUEUE = "DeadLetterQueue";
    }

    public static class ConnectionInfo{
        public final static String SERVER = "localhost";
        public final static String USER_NAME = "rabbitmq";
        public final static String PASSWORD = "rabbitmq";
    }

    public static class RoutingKey{
        public final static String MYROUTINGKEY = "myroutingkey";
        public final static String ERRORMYROUTINGKEY = "error.myroutingkey";
        public final static String DEADLETTERROUTINGKEY = "deadletterkey";
    }
}