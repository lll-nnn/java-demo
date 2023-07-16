package cn.itcast.mq.helloworld;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PublisherTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //简单队列
    @Test
    public void testSendMessageSimpleQueue() {
        rabbitTemplate.convertAndSend("simple.queue", "hello world!");
    }

    @Test
    public void testSendMessageWorkQueue() throws InterruptedException {
        for (int i = 1; i <= 50; i++) {
            rabbitTemplate.convertAndSend("simple.queue", "hello world! + index = " + i);
            Thread.sleep(50);
        }
    }

    @Test
    public void testSendFanoutExchange() {
        rabbitTemplate.convertAndSend("lee.fanout", "", "hello fanout queue");
    }

    @Test
    public void testSendDirectExchange() {
        rabbitTemplate.convertAndSend("lee.direct", "yellow", "hello direct queue");
    }

    @Test
    public void testSendTopicExchange() {
        rabbitTemplate.convertAndSend("lee.topic", "china.news", "hello topic queue");
    }

    @Test
    public void testSendObject() {
        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        map.put("lee", "123");
        //发送对象默认使用jdk序列化
        //注入jackson的消息转化器用json
        rabbitTemplate.convertAndSend("object.queue", map);
    }
}
