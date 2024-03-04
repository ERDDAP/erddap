package scripts;

import tags.TagIncompleteTest;

class ValidateDataSetPropertiesTests {

  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testDoIt() throws Exception {
    ValidateDataSetProperties.doIt();
  }

  @org.junit.jupiter.api.Test
  void testEnsureDatasetInDataSetProperties() throws Exception {
    ValidateDataSetProperties.ensureDatasetInDataSetProperties();
  }
}
