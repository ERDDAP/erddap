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
        final EmbeddedHiveMQBuilder embeddedHiveMQBuilder =
            EmbeddedHiveMQ.builder()
                .withConfigurationFolder(Path.of("WEB-INF/classes/gov/noaa/pfel/erddap/config"))
                .withDataFolder(Path.of("target/data"));

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
        // 4. Stop the broker only when the application context is destroyed
        this.hiveMQ.stop().join();
      } catch (final Exception ex) {
        System.out.println("Error stopping MQTT broker");
        ex.printStackTrace();
      }
    }
  }
}
