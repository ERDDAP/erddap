package gov.noaa.pfel.erddap;

import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import gov.noaa.pfel.erddap.util.EDStatic;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.nio.file.Path;

@WebListener
public class MqttBrokerContextListener implements ServletContextListener {

  private final boolean enableMqttBroker = EDStatic.config.enableMqttBroker;
  private EmbeddedHiveMQ hiveMQ;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    if (enableMqttBroker) {
      try {
        EmbeddedHiveMQBuilder embeddedHiveMQBuilder = EmbeddedHiveMQ.builder();

        if (EDStatic.config.mqttConfigFolder != null
            && !EDStatic.config.mqttConfigFolder.isEmpty()) {
          embeddedHiveMQBuilder =
              embeddedHiveMQBuilder.withConfigurationFolder(
                  Path.of(EDStatic.config.mqttConfigFolder));
        }
        if (EDStatic.config.mqttDataFolder != null && !EDStatic.config.mqttDataFolder.isEmpty()) {
          embeddedHiveMQBuilder =
              embeddedHiveMQBuilder.withDataFolder(Path.of(EDStatic.config.mqttDataFolder));
        }
        if (EDStatic.config.mqttExtensionsFolder != null
            && !EDStatic.config.mqttExtensionsFolder.isEmpty()) {
          embeddedHiveMQBuilder =
              embeddedHiveMQBuilder.withExtensionsFolder(
                  Path.of(EDStatic.config.mqttExtensionsFolder));
        }

        embeddedHiveMQBuilder = embeddedHiveMQBuilder.withoutLoggingBootstrap();

        this.hiveMQ = embeddedHiveMQBuilder.build();
        this.hiveMQ.start().join();

      } catch (final Exception ex) {
        System.out.println("Error starting MQTT broker: ");
        ex.printStackTrace();
      }
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    if (enableMqttBroker && this.hiveMQ != null) {
      try {
        this.hiveMQ.stop().join();
      } catch (final Exception ex) {
        System.out.println("Error stopping MQTT broker");
        ex.printStackTrace();
      }
    }
  }
}
