package um.tesoreria.aud.rest.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicsConfigurationTest {

    private final KafkaTopicsConfiguration configuration = new KafkaTopicsConfiguration();

    @Test
    void declaraLaDltDeAuditoriaConLasParticionesDelOrigen() {
        NewTopic topic = configuration.auditDltTopic("tesoreria-audit-evento.DLT", 3, 1);

        assertThat(topic.name()).isEqualTo("tesoreria-audit-evento.DLT");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
        assertThat(topic.configs())
                .containsEntry("retention.ms", "1209600000")
                .containsEntry("cleanup.policy", "delete")
                .containsEntry("min.insync.replicas", "1");
    }

    @Test
    void ajustaMinInsyncReplicasAlReplicationFactor() {
        assertThat(configuration.auditDltTopic("tesoreria-audit-evento.DLT", 3, 3).configs())
                .containsEntry("min.insync.replicas", "2");
    }
}
