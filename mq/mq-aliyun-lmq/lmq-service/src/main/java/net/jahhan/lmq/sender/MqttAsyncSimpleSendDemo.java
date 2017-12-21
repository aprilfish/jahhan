package net.jahhan.lmq.sender;

import net.jahhan.lmq.common.util.Tools;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.Properties;


/**
 * Created by alvin on 17-7-24. This is simple example for mqtt async java client sender mqtt msg
 */
public class MqttAsyncSimpleSendDemo {
    public static void main(String[] args) throws Exception {
        Properties properties = Tools.loadProperties();
        final String brokerUrl = properties.getProperty("brokerUrl");
        final String groupId = properties.getProperty("groupId");
        final String topic = properties.getProperty("topic");
        final int qosLevel = Integer.parseInt(properties.getProperty("qos"));
        final Boolean cleanSession = Boolean.parseBoolean(properties.getProperty("cleanSession"));
        String clientId = groupId + "@@@SEND0001";
        String accessKey = properties.getProperty("accessKey");
        String secretKey = properties.getProperty("secretKey");
        final MemoryPersistence memoryPersistence = new MemoryPersistence();
        final MqttAsyncClient mqttAsyncClient = new MqttAsyncClient(brokerUrl, clientId, memoryPersistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        //cal the sign as password,sign=BASE64(MAC.SHA1(groupId,secretKey))
        String sign = Tools.macSignature(clientId.split("@@@")[0], secretKey);
        connOpts.setUserName(accessKey);
        connOpts.setPassword(sign.toCharArray());
        connOpts.setCleanSession(cleanSession);
        connOpts.setKeepAliveInterval(90);
        connOpts.setAutomaticReconnect(true);
        mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("connect success");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println("receive msg from topic " + s + " , body is " + new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("sender msg succeed");
            }
        });
        //this is sync invoke wait complete
        mqttAsyncClient.connect(connOpts).waitForCompletion();
        while (true) {
            try {
                //async sender normal pub sub msg
                final String mqttSendTopic = topic + "/qos" + qosLevel;
                MqttMessage message = new MqttMessage("hello lmq pub sub msg".getBytes());
                message.setQos(qosLevel);
                //this is async invoke ,doesn't care the complete token
                IMqttDeliveryToken token = mqttAsyncClient.publish(mqttSendTopic, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }
}
