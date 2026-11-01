package um.tesoreria.aud.rest.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

/**
 * Topics de los que {@code tesoreria-auditoria} es dueño.
 *
 * <p>El topic principal ({@code tesoreria-audit-evento}) lo declara su productor,
 * {@code tesoreria-mercadopago}. Acá sólo se declara la DLT, porque es este servicio el que
 * escribe en ella al derivar un evento inválido.</p>
 *
 * <p>Particiones: las mismas que el topic origen (3), porque el
 * {@code DeadLetterPublishingRecoverer} publica en el mismo número de partición del registro
 * original y falla si no existe. El réplication factor es ambiental
 * ({@code auditoria.kafka.topic-replicas}).</p>
 */
@Configuration
public class KafkaTopicsConfiguration {

    private static final String DLT_RETENTION_MS = "1209600000"; // 14 días

    @Bean
    NewTopic auditDltTopic(@Value("${app.kafka.dlt-topic:tesoreria-audit-evento.DLT}") String topic,
                           @Value("${app.kafka.topic-partitions:3}") int partitions,
                           @Value("${app.kafka.topics.replicas:1}") int replicas) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicas)
                .configs(Map.of(
                        "min.insync.replicas", String.valueOf(Math.min(2, replicas)),
                        "retention.ms", DLT_RETENTION_MS,
                        "cleanup.policy", "delete"))
                .build();
    }
}
