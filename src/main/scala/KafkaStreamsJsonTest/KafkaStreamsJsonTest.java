package KafkaStreamsJsonTest;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.util.concurrent.CountDownLatch;
import java.util.Properties;

public class KafkaStreamsJsonTest {

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-json");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        final Serde<JsonNode> jsonSerdes = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

        StreamsBuilder builder = new StreamsBuilder();

        final Consumed<String, JsonNode> consumed = Consumed.with(Serdes.String(), jsonSerdes);

        KStream<String, JsonNode> jsonKStream = builder.stream("streams-plaintext-input-json", consumed);
        KStream<String, JsonNode> filteredjsonKStream = jsonKStream.filter((key, value) -> Integer.parseInt(value.get("age").toString()) > 25);

        //viewing current jsonKStream
        jsonKStream.foreach(new ForeachAction<String, JsonNode>() {
            @Override
            public void apply(String key, JsonNode value) {
                if (Integer.parseInt(value.get("age").toString()) > 25)
                    System.out.println("Hello I am , " + value.get("name") + "  " + value.get("age"));
                else
                    System.out.println("Sorry age is not greater than 25");
            }
        });

        filteredjsonKStream.to("streams-plaintext-output-json", Produced.with(Serdes.String(), jsonSerdes));

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-json-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);

    }
}

