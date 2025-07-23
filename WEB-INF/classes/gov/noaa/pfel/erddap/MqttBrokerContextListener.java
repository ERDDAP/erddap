package gov.noaa.pfel.erddap;

import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import java.nio.file.Path;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class MqttBrokerContextListener implements ServletContextListener {
  final EmbeddedHiveMQBuilder embeddedHiveMQBuilder =
      EmbeddedHiveMQ.builder()
          .withConfigurationFolder(Path.of("/path/to/embedded-config-folder"))
          .withDataFolder(Path.of("/path/to/embedded-data-folder"))
          .withExtensionsFolder(Path.of("/path/to/embedded-extensions-folder"));

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try (final EmbeddedHiveMQ hiveMQ = embeddedHiveMQBuilder.build()) {
      hiveMQ.start().join();
    } catch (final Exception ex) {
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    try (final EmbeddedHiveMQ hiveMQ = embeddedHiveMQBuilder.build()) {
      hiveMQ.start().join();
    } catch (final Exception ex) {
    }
  }
}
