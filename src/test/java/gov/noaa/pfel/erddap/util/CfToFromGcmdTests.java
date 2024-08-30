package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;
import com.cohort.util.Test;

class CfToFromGcmdTests {
  /**
   * This tests CF to GCMD.
   *
   * @throws RuntimeException if trouble
   */
  @org.junit.jupiter.api.Test
  void testCfToGcmd() {

    String2.log("\n*** CfToFromGcmd.testCfToGcmd");
    // reallyReallyVerbose = true;

    // first
    Test.ensureEqual(CfToFromGcmd.cfNames[0], "", "test1");
    Test.ensureEqual(CfToFromGcmd.cfToGcmd(""), new String[0], "test2");

    // second
    Test.ensureEqual(
        String2.toCSSVString(CfToFromGcmd.cfToGcmd("aerosol_angstrom_exponent")),
        "Earth Science > Atmosphere > Aerosols > Aerosol Optical Depth/Thickness > Angstrom Exponent, "
            + "Earth Science > Atmosphere > Aerosols > Aerosol Particle Properties, "
            + "Earth Science > Atmosphere > Aerosols > Particulate Matter",
        "test3");

    // in middle (with lead/trail spaces)
    Test.ensureEqual(
        String2.toCSSVString(CfToFromGcmd.cfToGcmd("  sea_water_temperature  ")),
        "Earth Science > Oceans > Ocean Temperature > Water Temperature",
        "test4");

    // no translation
    Test.ensureEqual(
        String2.toCSSVString(CfToFromGcmd.cfToGcmd("model_level_number")), "", "test5");

    // last
    Test.ensureEqual(
        String2.toCSSVString(CfToFromGcmd.cfToGcmd("zenith_angle")),
        "Earth Science > Atmosphere > Atmospheric Radiation > Incoming Solar Radiation, "
            + "Earth Science > Atmosphere > Atmospheric Radiation > Solar Irradiance, "
            + "Earth Science > Atmosphere > Atmospheric Radiation > Solar Radiation",
        "test6");

    String2.log("\n*** CfToFromGcmd.testCfToGcmd finished successfully.");
  }

  /**
   * This tests GCMD to CF.
   *
   * @throws RuntimeException if trouble
   */
  @org.junit.jupiter.api.Test
  void testGcmdToCf() {

    String2.log("\n*** CfToFromGcmd.testGcmdToCf");
    // reallyReallyVerbose = true;

    // nothing
    Test.ensureEqual(CfToFromGcmd.gcmdToCf(""), new String[0], "test20");

    // invalid/unknown name
    Test.ensureEqual(CfToFromGcmd.gcmdToCf("bob"), new String[0], "test21");

    // (with lead/trail spaces)
    Test.ensureEqual(
        String2.toCSSVString(
            CfToFromGcmd.gcmdToCf(
                "  Earth Science > Atmosphere > Atmospheric Chemistry > Carbon and Hydrocarbon Compounds > Carbon Dioxide  ")),
        "atmosphere_mass_content_of_carbon_dioxide, "
            + "atmosphere_mass_of_carbon_dioxide, "
            + "atmosphere_moles_of_carbon_dioxide, "
            + "mass_concentration_of_carbon_dioxide_in_air, "
            + "mass_fraction_of_carbon_dioxide_in_air, "
            + "mole_concentration_of_carbon_dioxide_in_air, "
            + "mole_fraction_of_carbon_dioxide_in_air, "
            + "surface_carbon_dioxide_mole_flux, "
            + "surface_carbon_dioxide_partial_pressure_difference_between_air_and_sea_water, "
            + "surface_carbon_dioxide_partial_pressure_difference_between_sea_water_and_air, "
            + "surface_downward_mass_flux_of_carbon_dioxide_expressed_as_carbon, "
            + "surface_downward_mole_flux_of_carbon_dioxide, "
            + "surface_frozen_carbon_dioxide_amount, "
            + "surface_net_downward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_all_land_processes, "
            + "surface_net_downward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_all_land_processes_excluding_anthropogenic_land_use_change, "
            + "surface_net_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_anthropogenic_land_use_change, "
            + "surface_partial_pressure_of_carbon_dioxide_in_air, "
            + "surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_crop_harvesting, "
            + "surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_fires_excluding_anthropogenic_land_use_change, "
            + "surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_grazing, "
            + "surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_natural_sources, "
            + "surface_upward_mole_flux_of_carbon_dioxide, "
            + "tendency_of_atmosphere_mass_content_of_carbon_dioxide_due_to_emission, "
            + "tendency_of_atmosphere_mass_content_of_carbon_dioxide_expressed_as_carbon_due_to_anthropogenic_emission, "
            + "tendency_of_atmosphere_mass_content_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_fossil_fuel_combustion, "
            + "tendency_of_atmosphere_moles_of_carbon_dioxide",
        "test22");

    // doesn't exist
    Test.ensureEqual(String2.toCSSVString(CfToFromGcmd.gcmdToCf("bob")), "", "test23");

    String2.log("\n*** CfToFromGcmd.testGcmdToCf finished successfully.");
  }
}
