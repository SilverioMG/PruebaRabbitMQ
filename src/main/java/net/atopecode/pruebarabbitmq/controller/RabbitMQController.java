package net.atopecode.pruebarabbitmq.controller;

import com.google.gson.JsonObject;
import net.atopecode.pruebarabbitmq.config.RabbitMQConfig;
import net.atopecode.pruebarabbitmq.dto.RabbitMQMessage;
import net.atopecode.pruebarabbitmq.manager.RabbitMQManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
 
@RestController
@RequestMapping("/rabbitmq")
public class RabbitMQController {
    private final Logger logger = Logger.getLogger(RabbitMQController.class);
 
    private RabbitMQManager rabbitMQManager;
 
    @Autowired
    public RabbitMQController(RabbitMQManager rabbitMQManager){
        this.rabbitMQManager = rabbitMQManager;
    }
 
    @RequestMapping(value = "/senddirect", method = RequestMethod.POST)
    public ResponseEntity<byte[]> sendDirect(HttpServletRequest request, @RequestBody JsonObject body){
        try{
            String exchange = body.get("exchange").getAsString();
            String routingKey = body.get("routingkey").getAsString();
            String message = body.get("message").getAsString();
            rabbitMQManager.sendDirect(exchange, routingKey, message);
 
            return new ResponseEntity<byte[]>("Mensaje enviado.".getBytes(), HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<byte[]>("No se ha podido enviar el mensaje.".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    @RequestMapping(value = "/senddirectjsonobject", method = RequestMethod.POST)
    public ResponseEntity<byte[]> sendDirectJsonObject(HttpServletRequest request, @RequestBody JsonObject body){
        try{
            String exchange = body.get("exchange").getAsString();
            String routingKey = body.get("routingkey").getAsString();
            JsonObject messageJsonObject = body.get("message").getAsJsonObject();
            RabbitMQMessage messageObject = RabbitMQMessage.fromJson(messageJsonObject);
            rabbitMQManager.sendDirectJsonObject(exchange, routingKey, messageObject);

            return new ResponseEntity<byte[]>("Mensaje enviado.".getBytes(), HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<byte[]>("No se ha podido enviar el mensaje.".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    @RequestMapping(value = "/senddirectbinaryobject", method = RequestMethod.POST)
    public ResponseEntity<byte[]> sendDirectBinaryObject(HttpServletRequest request, @RequestBody JsonObject body){
        try{
            String exchange = body.get("exchange").getAsString();
            String routingKey = body.get("routingkey").getAsString();
            JsonObject messageJsonObject = body.get("message").getAsJsonObject();
            RabbitMQMessage messageObject = RabbitMQMessage.fromJson(messageJsonObject);
            rabbitMQManager.sendDirectBinary(exchange, routingKey, messageObject);

            return new ResponseEntity<byte[]>("Mensaje enviado.".getBytes(), HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<byte[]>("No se ha podido enviar el mensaje.".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    @RequestMapping(value = "/sendmessage", method = RequestMethod.POST)
    public ResponseEntity<byte[]> sendMessage(HttpServletRequest reqeust, @RequestBody JsonObject body){
        try{
            String message = body.get("message").getAsString();
            rabbitMQManager.sendDirectBinary(RabbitMQConfig.ExchangeName.MYEXCHANGE, RabbitMQConfig.RoutingKey.MYROUTINGKEY, message);

            return new ResponseEntity<byte[]>("Mensaje enviado.".getBytes(), HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<byte[]>("No se ha podido enviar el mensaje.".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}