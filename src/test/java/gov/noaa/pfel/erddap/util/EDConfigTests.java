package gov.noaa.pfel.erddap.util;

import com.cohort.util.ResourceBundle2;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

public class EDConfigTests {

  @BeforeAll
  static void beforeAll() {
    Initialization.edStatic();
  }

  @Test
  void testS3ConfigDefaults() {
    EDConfig config = EDStatic.config;
    com.cohort.util.Test.ensureNotNull(config, "EDStatic.config should not be null");

    // Default configuration values
    com.cohort.util.Test.ensureEqual(config.s3TargetThroughputInGbps, 20.0, "default s3TargetThroughputInGbps");
    com.cohort.util.Test.ensureTrue(config.s3MaxConcurrency == null, "default s3MaxConcurrency should be null");
  }

  @Test
  void testS3ConfigParsing() throws Exception {
    EDConfig config = EDStatic.config;
    com.cohort.util.Test.ensureNotNull(config, "EDStatic.config should not be null");

    Map<String, String> ev = new HashMap<>();
    ConcurrentHashMap<String, String> setupMap = new ConcurrentHashMap<>();
    ResourceBundle2 setup = new ResourceBundle2(setupMap);

    // 1. Test parsing valid positive values
    setupMap.put("s3TargetThroughputInGbps", "5.5");
    setupMap.put("s3MaxConcurrency", "100");

    double targetThroughput = config.getSetupEVDouble(setup, ev, "s3TargetThroughputInGbps", 20.0);
    int rawMaxConcurrency = config.getSetupEVInt(setup, ev, "s3MaxConcurrency", -1);
    Integer maxConcurrency = rawMaxConcurrency > 0 ? Integer.valueOf(rawMaxConcurrency) : null;

    com.cohort.util.Test.ensureEqual(targetThroughput, 5.5, "parsed valid targetThroughputInGbps");
    com.cohort.util.Test.ensureEqual(maxConcurrency, Integer.valueOf(100), "parsed valid maxConcurrency");

    // 2. Test missing values (fallbacks to defaults)
    setupMap.remove("s3TargetThroughputInGbps");
    setupMap.remove("s3MaxConcurrency");

    targetThroughput = config.getSetupEVDouble(setup, ev, "s3TargetThroughputInGbps", 20.0);
    rawMaxConcurrency = config.getSetupEVInt(setup, ev, "s3MaxConcurrency", -1);
    maxConcurrency = rawMaxConcurrency > 0 ? Integer.valueOf(rawMaxConcurrency) : null;

    com.cohort.util.Test.ensureEqual(targetThroughput, 20.0, "missing targetThroughputInGbps falls back to 20.0");
    com.cohort.util.Test.ensureTrue(maxConcurrency == null, "missing maxConcurrency falls back to null");

    // 3. Test non-positive / zero values (fallbacks to defaults)
    setupMap.put("s3TargetThroughputInGbps", "-10.0");
    setupMap.put("s3MaxConcurrency", "0");

    targetThroughput = config.getSetupEVDouble(setup, ev, "s3TargetThroughputInGbps", 20.0);
    rawMaxConcurrency = config.getSetupEVInt(setup, ev, "s3MaxConcurrency", -1);
    maxConcurrency = rawMaxConcurrency > 0 ? Integer.valueOf(rawMaxConcurrency) : null;

    com.cohort.util.Test.ensureEqual(targetThroughput, 20.0, "negative targetThroughputInGbps falls back to 20.0");
    com.cohort.util.Test.ensureTrue(maxConcurrency == null, "zero maxConcurrency falls back to null");

    // 4. Test malformed / non-numeric string values (fallbacks to defaults)
    setupMap.put("s3TargetThroughputInGbps", "not_a_number");
    setupMap.put("s3MaxConcurrency", "invalid_int");

    targetThroughput = config.getSetupEVDouble(setup, ev, "s3TargetThroughputInGbps", 20.0);
    rawMaxConcurrency = config.getSetupEVInt(setup, ev, "s3MaxConcurrency", -1);
    maxConcurrency = rawMaxConcurrency > 0 ? Integer.valueOf(rawMaxConcurrency) : null;

    com.cohort.util.Test.ensureEqual(targetThroughput, 20.0, "malformed targetThroughputInGbps falls back to 20.0");
    com.cohort.util.Test.ensureTrue(maxConcurrency == null, "malformed maxConcurrency falls back to null");

    // 5. Test environment variable overrides
    ev.put("ERDDAP_s3TargetThroughputInGbps", "12.5");
    ev.put("ERDDAP_s3MaxConcurrency", "64");

    targetThroughput = config.getSetupEVDouble(setup, ev, "s3TargetThroughputInGbps", 20.0);
    rawMaxConcurrency = config.getSetupEVInt(setup, ev, "s3MaxConcurrency", -1);
    maxConcurrency = rawMaxConcurrency > 0 ? Integer.valueOf(rawMaxConcurrency) : null;

    com.cohort.util.Test.ensureEqual(targetThroughput, 12.5, "EV override targetThroughputInGbps");
    com.cohort.util.Test.ensureEqual(maxConcurrency, Integer.valueOf(64), "EV override maxConcurrency");

    // 6. Test environment variable with malformed / negative value falls back safely
    ev.put("ERDDAP_s3TargetThroughputInGbps", "bad_ev");
    ev.put("ERDDAP_s3MaxConcurrency", "-5");

    targetThroughput = config.getSetupEVDouble(setup, ev, "s3TargetThroughputInGbps", 20.0);
    rawMaxConcurrency = config.getSetupEVInt(setup, ev, "s3MaxConcurrency", -1);
    maxConcurrency = rawMaxConcurrency > 0 ? Integer.valueOf(rawMaxConcurrency) : null;

    com.cohort.util.Test.ensureEqual(targetThroughput, 20.0, "malformed EV falls back to default 20.0");
    com.cohort.util.Test.ensureTrue(maxConcurrency == null, "negative EV falls back to null");
  }

  @Test
  void testBuildS3TransferManager() throws Exception {
    double oldThroughput = EDStatic.config.s3TargetThroughputInGbps;
    Integer oldConcurrency = EDStatic.config.s3MaxConcurrency;
    try {
      EDStatic.config.s3TargetThroughputInGbps = 10.0;
      EDStatic.config.s3MaxConcurrency = 32;

      software.amazon.awssdk.transfer.s3.S3TransferManager tm =
          EDStatic.buildS3TransferManager("us-east-1");
      com.cohort.util.Test.ensureNotNull(tm, "buildS3TransferManager should return S3TransferManager");
    } finally {
      EDStatic.config.s3TargetThroughputInGbps = oldThroughput;
      EDStatic.config.s3MaxConcurrency = oldConcurrency;
    }
  }
}
