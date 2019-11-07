package net.atopecode.pruebarabbitmq.config;


import com.rabbitmq.client.Channel;
import net.atopecode.pruebarabbitmq.messagelistener.*;
import org.aopalliance.aop.Advice;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class RabbitMQConfigTestVirtualHost {
    private Logger logger = Logger.getLogger(RabbitMQConfigTestVirtualHost.class);
    public final static String virtualHost = "test";

    //Se crea el 'Exchange' utilizando el 'ConnectionFactory' de esta clase para que lo cree dentro del 'VirtualHost'.
    public void createTestExchange(ConnectionFactory connectionFactory) {
        Connection connection = connectionFactory.createConnection();
        try{
            Channel channel = connection.createChannel(true);
            channel.exchangeDeclare(ExchangeName.TESTEXCHANGE, "direct", true);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally {
            connection.close();
        }
    }

    //Se crea el 'Exchange' utilizando el 'ConnectionFactory' de esta clase para que lo cree dentro del 'VirtualHost'.
    public void createErrorExchange(ConnectionFactory connectionFactory) {
        Connection connection = connectionFactory.createConnection();
        try{
            Channel channel = connection.createChannel(true);
            channel.exchangeDeclare(ExchangeName.ERROREXCHANGE, "direct", true);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally {
            connection.close();
        }
    }

    //Se crea la 'Queue' utilizando el 'ConnectionFactory' de esta clase para que lo cree dentro del 'VirtualHost'.
    public void createTestQueue(ConnectionFactory connectionFactory) {
        //Se crea una cola con atributos que indican que cuando se rechaza un mensaje se envíe directamente a otra cola sin necesidad
        //de utilizar una clase 'Recover' en el 'Listener'.
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("x-dead-letter-exchange", RabbitMQConfigTestVirtualHost.ExchangeName.ERROREXCHANGE);
        attributes.put("x-dead-letter-routing-key", RabbitMQConfigTestVirtualHost.RoutingKey.ERRORMYROUTINGKEY);
        Connection connection = connectionFactory.createConnection();
        try {
            Channel channel = connection.createChannel(true);
            channel.queueDeclare(QueueName.TESTQUEUE, true, false, false, attributes);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            connection.close();
        }
    }

    //Se crea la 'Queue' utilizando el 'ConnectionFactory' de esta clase para que lo cree dentro del 'VirtualHost'.
    public void createErrorQueue(ConnectionFactory connectionFactory) {
        Connection connection = connectionFactory.createConnection();
        try {
            Channel channel = connection.createChannel(true);
            channel.queueDeclare(QueueName.ERRORQUEUE, true, false, false, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            connection.close();
        }
    }

    public void createTestBinding(ConnectionFactory connectionFactory){
        Connection connection = connectionFactory.createConnection();
        try {
            Channel channel = connection.createChannel(true);
            channel.queueBind(QueueName.TESTQUEUE, ExchangeName.TESTEXCHANGE, RoutingKey.TESTROUTINGKEY);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            connection.close();
        }
    }

    public void createErrorBinding(ConnectionFactory connectionFactory){
        Connection connection = connectionFactory.createConnection();
        try {
            Channel channel = connection.createChannel(true);
            channel.queueBind(QueueName.ERRORQUEUE, ExchangeName.ERROREXCHANGE, RoutingKey.ERRORMYROUTINGKEY);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            connection.close();
        }
    }


    //Se configura la conexión de 'RabbitMQ.messageListenerContainer()'.
    @Bean("connectionFactoryTest") //Para inyectar este 'Bean' hay que usar '@Qualifier("connectionFactorytest"), sino se pone nada se inyecta el de 'RabbitMQConfig' porque tiene '@Primary'.
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(RabbitMQConfig.ConnectionInfo.SERVER);
        cachingConnectionFactory.setUsername(RabbitMQConfigTestVirtualHost.ConnectionInfo.USER_NAME);
        cachingConnectionFactory.setPassword(RabbitMQConfigTestVirtualHost.ConnectionInfo.PASSWORD);
        cachingConnectionFactory.setVirtualHost("test"); //Se conecta a un 'VirtualHost'.

        createTestExchange(cachingConnectionFactory);
        createTestQueue(cachingConnectionFactory);
        createTestBinding(cachingConnectionFactory);

        createErrorExchange(cachingConnectionFactory);
        createErrorQueue(cachingConnectionFactory);
        createErrorBinding(cachingConnectionFactory);

        return cachingConnectionFactory;
    }

    @Bean
    public MessageListenerContainer messageListenerContainerTestQueue() {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
        simpleMessageListenerContainer.setQueueNames(QueueName.TESTQUEUE);
        simpleMessageListenerContainer.setMessageListener(new RabbitMQTestQueueListener());

        //La cola de este 'Listener' tiene atributos 'deadletter' para que se reenvie a otra cola en caso de error.
        //Hay que utilizar 'setDefaultRequeueRejected(false);' para que el mensaje se envíe a la otra cola (atributos deadletter)
        //tanto si se lanza una 'RuntimeException' como una 'AmqpRejectAndDontRequeueException', sino solo funciona el 'deadletter' con una 'AmqpRejectAndDontRequeueException'.
        simpleMessageListenerContainer.setDefaultRequeueRejected(false);

        // Should be restricted to '1' to maintain data consistency.
        //simpleMessageListenerContainer.setConcurrentConsumers(1);

        return simpleMessageListenerContainer;
    }

//    @Bean
//    public MessageListenerContainer messageListenerContainerTestQueue2() {
//        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
//        simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
//        simpleMessageListenerContainer.setQueueNames(QueueName.TESTQUEUE);
//        simpleMessageListenerContainer.setMessageListener(new RabbitMQTestQueueListener2());
//
//        //La cola de este 'Listener' tiene atributos 'deadletter' para que se reenvie a otra cola en caso de error.
//        //Hay que utilizar 'setDefaultRequeueRejected(false);' para que el mensaje se envíe a la otra cola (atributos deadletter)
//        //tanto si se lanza una 'RuntimeException' como una 'AmqpRejectAndDontRequeueException', sino solo funciona el 'deadletter' con una 'AmqpRejectAndDontRequeueException'.
//        simpleMessageListenerContainer.setDefaultRequeueRejected(false);
//
//        // Should be restricted to '1' to maintain data consistency.
//        //simpleMessageListenerContainer.setConcurrentConsumers(1);
//
//        return simpleMessageListenerContainer;
//    }

    //El 'VirtualHost' solo afecta al nombre de los 'Exchange' (no de las colas).
    public static class ExchangeName{
        public final static String TESTEXCHANGE = "TestExchange";
        public final static String ERROREXCHANGE = "ErrorExchange";
    }

    public static class QueueName{
        public final static String TESTQUEUE = "TestQueue";
        public final static String ERRORQUEUE = "ErrorQueue";
    }

    public static class ConnectionInfo{
        public final static String SERVER = "localhost";
        public final static String USER_NAME = "rabbitmq";
        public final static String PASSWORD = "rabbitmq";
    }

    public static class RoutingKey{
        public final static String TESTROUTINGKEY = "testroutingkey";
        public final static String ERRORMYROUTINGKEY = "error.myroutingkey";
    }
}
