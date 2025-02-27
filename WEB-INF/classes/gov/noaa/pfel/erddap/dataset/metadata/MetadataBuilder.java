package gov.noaa.pfel.erddap.dataset.metadata;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import gov.noaa.pfel.erddap.variable.EDVAltGridAxis;
import gov.noaa.pfel.erddap.variable.EDVDepth;
import gov.noaa.pfel.erddap.variable.EDVDepthGridAxis;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import gov.noaa.pfel.erddap.variable.EDVLat;
import gov.noaa.pfel.erddap.variable.EDVLatGridAxis;
import gov.noaa.pfel.erddap.variable.EDVLon;
import gov.noaa.pfel.erddap.variable.EDVLonGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTime;
import gov.noaa.pfel.erddap.variable.EDVTimeGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import gov.noaa.pfel.erddap.variable.EDVTimeStampGridAxis;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.content.DefaultRangeDimension;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.distribution.DefaultDistributor;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.identification.DefaultAssociatedResource;
import org.apache.sis.metadata.iso.identification.DefaultCoupledResource;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.identification.DefaultOperationMetadata;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.maintenance.DefaultMaintenanceInformation;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.spatial.DefaultDimension;
import org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation;
import org.apache.sis.pending.geoapi.evolution.UnsupportedCodeList;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.joda.time.DateTime;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.AssociationType;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.InitiativeType;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.InternationalString;

public class MetadataBuilder {
  public static Metadata buildMetadata(
      String datasetId,
      long creationDate,
      Attributes attributes,
      EDV[] variables,
      boolean accessibleViaWms,
      boolean accessibleViaSubset) {

    String minTime = ""; // iso string with Z, may be ""
    String maxTime = "";
    double minVert =
        Double.NaN; // in destination units (may be positive = up[I use] or down?! any units)
    double maxVert = Double.NaN;
    float latMin = Float.NaN;
    float latMax = Float.NaN;
    float lonMin = Float.NaN;
    float lonMax = Float.NaN;

    List<InternationalString> standardNames = new ArrayList<>();
    List<Dimension> dimensions = new ArrayList<>();
    List<RangeDimension> rangeDimensions = new ArrayList<>();
    for (EDV edv : variables) {
      dimensions.add(dimFromVariable(edv));
      String sn = edv.combinedAttributes().getString("standard_name");
      if (sn != null) standardNames.add(new EDDInternationalString(sn));
      DefaultRangeDimension attrDim = new DefaultRangeDimension();
      attrDim.setSequenceIdentifier(
          DefaultNameFactory.provider()
              .createMemberName(
                  null,
                  edv.destinationName(),
                  DefaultNameFactory.provider().createTypeName(null, edv.destinationDataType())));
      attrDim.setDescription(new EDDInternationalString(edv.longName()));
      rangeDimensions.add(attrDim);

      if (edv instanceof EDVTime || edv instanceof EDVTimeStamp) {
        minTime = edv.destinationMinString(); // differs from EDDGrid, may be ""
        maxTime = edv.destinationMaxString();
        if (minTime.startsWith("0000")) minTime = ""; // 0000 is invalid in ISO 19115
        if (maxTime.startsWith("0000")) maxTime = "";
      }

      if (edv instanceof EDVDepth) {
        minVert = -edv.destinationMaxDouble(); // make into alt
        maxVert = -edv.destinationMinDouble();
      }
      if (edv instanceof EDVAlt) {
        minVert = edv.destinationMinDouble();
        maxVert = edv.destinationMaxDouble();
      }

      if (edv instanceof EDVLat) {
        latMin = (float) edv.destinationMinDouble();
        latMax = (float) edv.destinationMaxDouble();
        if (!Float.isNaN(latMin)) latMin = (float) Math2.minMax(-90, 90, latMin);
        if (!Float.isNaN(latMax)) latMax = (float) Math2.minMax(-90, 90, latMax);
      }
      if (edv instanceof EDVLon) {
        // lon,lat Min/Max
        // ??? I'm not certain but I suspect ISO requires lon to be +/-180.
        //    I don't see guidance for 0 - 360 datasets.
        // so just deal with some of the options
        // and use (float) to avoid float->double bruising
        // default: just the lon part already in -180 to 180.
        // EDDGrid doesn't allow for NaN
        lonMin = (float) edv.destinationMinDouble();
        lonMax = (float) edv.destinationMaxDouble();
        if (!Float.isNaN(lonMin) && !Float.isNaN(lonMax)) {
          lonMin = (float) Math2.minMax(-180, 180, lonMin);
          lonMax = (float) Math2.minMax(-180, 180, lonMax);
          // 0 to 360  -> -180 to 180
          if (edv.destinationMinDouble() >= 0
              && edv.destinationMinDouble() <= 20
              && edv.destinationMaxDouble() >= 340) {
            lonMin = -180;
            lonMax = 180;
            // all lon >=180, so shift down 360
          } else if (edv.destinationMinDouble() >= 180) {
            lonMin = (float) Math2.minMax(-180, 180, edv.destinationMinDouble() - 360);
            lonMax = (float) Math2.minMax(-180, 180, edv.destinationMaxDouble() - 360);
          }
        }
      }
    }

    DefaultExtent extent = new DefaultExtent();
    extent.setDescription(new EDDInternationalString("boundingExtent"));
    extent.setGeographicElements(
        List.of(new DefaultGeographicBoundingBox(lonMin, lonMax, latMin, latMax)));
    if (!minTime.isEmpty() && !maxTime.isEmpty()) {
      DefaultTemporalExtent tempExtent = new DefaultTemporalExtent();
      tempExtent.setBounds(DateTime.parse(minTime).toDate(), DateTime.parse(maxTime).toDate());
      extent.setTemporalElements(List.of(tempExtent));
    }
    if (!Double.isNaN(minVert) && !Double.isNaN(maxVert)) {
      extent.setVerticalElements(List.of(new DefaultVerticalExtent(minVert, maxVert, null)));
    }
    return buildMetadata(
        datasetId,
        creationDate,
        attributes,
        dimensions,
        rangeDimensions,
        standardNames,
        extent,
        false,
        accessibleViaWms,
        accessibleViaSubset);
  }

  public static Metadata buildMetadata(
      String datasetId,
      long creationDate,
      Attributes attributes,
      EDV[] variables,
      EDVGridAxis[] axisVariables,
      boolean accessibleViaWms,
      boolean accessibleViaSubset) {

    String minTime = ""; // iso string with Z, may be ""
    String maxTime = "";
    double minVert =
        Double.NaN; // in destination units (may be positive = up[I use] or down?! any units)
    double maxVert = Double.NaN;
    float latMin = Float.NaN;
    float latMax = Float.NaN;
    float lonMin = Float.NaN;
    float lonMax = Float.NaN;

    List<InternationalString> standardNames = new ArrayList<>();
    List<Dimension> dimensions = new ArrayList<>();
    List<RangeDimension> rangeDimensions = new ArrayList<>();
    for (EDVGridAxis edv : axisVariables) {
      dimensions.add(dimFromAxis(edv));
      String sn = edv.combinedAttributes().getString("standard_name");
      if (sn != null) standardNames.add(new EDDInternationalString(sn));

      if (edv instanceof EDVTimeGridAxis || edv instanceof EDVTimeStampGridAxis) {
        minTime = edv.destinationMinString(); // differs from EDDGrid, may be ""
        maxTime = edv.destinationMaxString();
        if (minTime.startsWith("0000")) minTime = ""; // 0000 is invalid in ISO 19115
        if (maxTime.startsWith("0000")) maxTime = "";
      }

      if (edv instanceof EDVDepthGridAxis) {
        minVert = -edv.destinationMaxDouble(); // make into alt
        maxVert = -edv.destinationMinDouble();
      }
      if (edv instanceof EDVAltGridAxis) {
        minVert = edv.destinationMinDouble();
        maxVert = edv.destinationMaxDouble();
      }

      if (edv instanceof EDVLatGridAxis) {
        latMin = (float) edv.destinationMinDouble();
        latMax = (float) edv.destinationMaxDouble();
        if (!Float.isNaN(latMin)) latMin = (float) Math2.minMax(-90, 90, latMin);
        if (!Float.isNaN(latMax)) latMax = (float) Math2.minMax(-90, 90, latMax);
      }
      if (edv instanceof EDVLonGridAxis) {
        // lon,lat Min/Max
        // ??? I'm not certain but I suspect ISO requires lon to be +/-180.
        //    I don't see guidance for 0 - 360 datasets.
        // so just deal with some of the options
        // and use (float) to avoid float->double bruising
        // default: just the lon part already in -180 to 180.
        // EDDGrid doesn't allow for NaN
        lonMin = (float) edv.destinationMinDouble();
        lonMax = (float) edv.destinationMaxDouble();
        if (!Float.isNaN(lonMin) && !Float.isNaN(lonMax)) {
          lonMin = (float) Math2.minMax(-180, 180, lonMin);
          lonMax = (float) Math2.minMax(-180, 180, lonMax);
          // 0 to 360  -> -180 to 180
          if (edv.destinationMinDouble() >= 0
              && edv.destinationMinDouble() <= 20
              && edv.destinationMaxDouble() >= 340) {
            lonMin = -180;
            lonMax = 180;
            // all lon >=180, so shift down 360
          } else if (edv.destinationMinDouble() >= 180) {
            lonMin = (float) Math2.minMax(-180, 180, edv.destinationMinDouble() - 360);
            lonMax = (float) Math2.minMax(-180, 180, edv.destinationMaxDouble() - 360);
          }
        }
      }
    }
    for (EDV dataVariable : variables) {
      String sn = dataVariable.combinedAttributes().getString("standard_name");
      if (sn != null) standardNames.add(new EDDInternationalString(sn));
      DefaultRangeDimension attrDim = new DefaultRangeDimension();
      attrDim.setSequenceIdentifier(
          DefaultNameFactory.provider()
              .createMemberName(
                  null,
                  dataVariable.destinationName(),
                  DefaultNameFactory.provider()
                      .createTypeName(null, dataVariable.destinationDataType())));
      attrDim.setDescription(new EDDInternationalString(dataVariable.longName()));
      rangeDimensions.add(attrDim);
    }

    DefaultExtent extent = new DefaultExtent();
    extent.setDescription(new EDDInternationalString("boundingExtent"));
    extent.setGeographicElements(
        List.of(new DefaultGeographicBoundingBox(lonMin, lonMax, latMin, latMax)));
    if (!minTime.isEmpty() && !maxTime.isEmpty()) {
      DefaultTemporalExtent tempExtent = new DefaultTemporalExtent();
      tempExtent.setBounds(DateTime.parse(minTime).toDate(), DateTime.parse(maxTime).toDate());
      extent.setTemporalElements(List.of(tempExtent));
    }
    if (!Double.isNaN(minVert) && !Double.isNaN(maxVert)) {
      extent.setVerticalElements(List.of(new DefaultVerticalExtent(minVert, maxVert, null)));
    }

    return buildMetadata(
        datasetId,
        creationDate,
        attributes,
        dimensions,
        rangeDimensions,
        standardNames,
        extent,
        true,
        accessibleViaWms,
        accessibleViaSubset);
  }

  private static Metadata buildMetadata(
      String datasetId,
      long creationDate,
      Attributes attributes,
      List<Dimension> dimensions,
      List<RangeDimension> rangeDimensions,
      List<InternationalString> standardNames,
      Extent extent,
      boolean isGriddap,
      boolean accessibleViaWms,
      boolean accessibleViaSubset) {
    DefaultMetadata metadata = new DefaultMetadata();
    // metadata.setFileIdentifier(datasetId); // id info
    metadata.setLocalesAndCharsets(Map.of(Locale.ENGLISH, StandardCharsets.UTF_8));
    metadata.setMetadataScopes(
        List.of(
            new DefaultMetadataScope(ScopeCode.DATASET, ScopeCode.DATASET.identifier()),
            new DefaultMetadataScope(ScopeCode.SERVICE, ScopeCode.SERVICE.identifier())));
    metadata.setContacts(List.of(adminContact(Role.POINT_OF_CONTACT)));
    // metadata.setDateStamp(null); // id info

    DefaultGridSpatialRepresentation spatialRepresentation = new DefaultGridSpatialRepresentation();
    spatialRepresentation.setAxisDimensionProperties(dimensions);
    spatialRepresentation.setNumberOfDimensions(dimensions.size());
    metadata.setSpatialRepresentationInfo(List.of(spatialRepresentation));

    // identification info
    List<CitationDate> dates = new ArrayList<>();
    dates.add(new DefaultCitationDate(new Date(creationDate), DateType.CREATION));
    String dateIssued = Calendar2.tryToIsoString(attributes.getString("date_issued"));
    if (String2.isSomething(dateIssued)) {
      dates.add(new DefaultCitationDate(DateTime.parse(dateIssued).toDate(), DateType.PUBLICATION));
    }

    String domain = EDStatic.baseUrl;
    if (domain.startsWith("http://")) domain = domain.substring(7);
    else if (domain.startsWith("https://")) domain = domain.substring(8);
    String contributorName = attributes.getString("contributor_name");
    String contributorEmail = attributes.getString("contributor_email");
    String contributorRole = attributes.getString("contributor_role");
    String creatorName = attributes.getString("creator_name");
    String creatorEmail = attributes.getString("creator_email");
    String creatorType = attributes.getString("creator_type");
    String institution = attributes.getString("institution");
    String infoUrl = attributes.getString("infoUrl");
    String acknowledgement = attributes.getString("acknowledgement"); // acdd 1.3
    if (acknowledgement == null) {
      acknowledgement = attributes.getString("acknowledgment"); // acdd 1.0
    }
    String project = attributes.getString("project");
    String license = attributes.getString("license");
    creatorType = String2.validateAcddContactType(creatorType);
    if (!String2.isSomething2(creatorType) && String2.isSomething2(creatorName))
      creatorType = String2.guessAcddContactType(creatorName);
    if (creatorType == null) // creatorType will be something
    creatorType = "person"; // assume

    List<ResponsibleParty> citationContacts = new ArrayList<>();
    ResponsibleParty creatorParty =
        buildResponsibleParty(
            String2.isSomething2(creatorType) && creatorType.matches("(person|position)")
                ? creatorName
                : null,
            String2.isSomething2(creatorType) && creatorType.matches("(group|institution)")
                ? creatorName
                : institution,
            null,
            creatorEmail,
            infoUrl,
            Role.ORIGINATOR);
    citationContacts.add(creatorParty);
    if (String2.isSomething(contributorName) || String2.isSomething(contributorRole)) {
      citationContacts.add(
          buildResponsibleParty(
              contributorName, null, contributorRole, contributorEmail, null, Role.USER));
    }
    DefaultCitation dataCitation = new DefaultCitation();
    dataCitation.setTitle(new EDDInternationalString(attributes.getString("title")));
    dataCitation.setDates(dates);
    dataCitation.setCitedResponsibleParties(citationContacts);

    DefaultCitation idAuthority = new DefaultCitation();
    idAuthority.setTitle(new EDDInternationalString(domain));
    DefaultIdentifier dataIdentifier = new DefaultIdentifier();
    dataIdentifier.setCode(datasetId);
    dataIdentifier.setAuthority(idAuthority);
    dataCitation.setIdentifiers(List.of(dataIdentifier));

    DefaultDataIdentification dataId = new DefaultDataIdentification();
    dataId.setCitation(dataCitation);
    dataId.setAbstract(new EDDInternationalString(attributes.getString("summary")));
    dataId.setCredits(List.of(acknowledgement));
    dataId.setPointOfContacts(
        List.of(responsiblePartyWithRole(creatorParty, Role.POINT_OF_CONTACT)));
    dataId.setDescriptiveKeywords(getKeywords(attributes, standardNames));
    if (String2.isSomething(license)) {
      dataId.setResourceConstraints(List.of(new DefaultConstraints(license)));
    }
    List<DefaultAssociatedResource> resources = new ArrayList<>();
    if (String2.isSomething(project)) {
      DefaultAssociatedResource projectResource = new DefaultAssociatedResource();
      projectResource.setAssociationType(AssociationType.LARGER_WORD_CITATION);
      projectResource.setInitiativeType(InitiativeType.PROJECT);
      DefaultCitation projectName = new DefaultCitation();
      projectName.setTitle(new EDDInternationalString(project));
      projectResource.setName(projectName);
      resources.add(projectResource);
    }
    String cdmDataType = attributes.getString("cdm_data_type");
    if (!EDD.CDM_OTHER.equals(cdmDataType)) {
      DefaultAssociatedResource cdmResource = new DefaultAssociatedResource();
      cdmResource.setAssociationType(AssociationType.LARGER_WORD_CITATION);
      cdmResource.setInitiativeType(InitiativeType.PROJECT);
      DefaultCitation cdmSet = new DefaultCitation();
      cdmSet.setTitle(new EDDInternationalString(cdmDataType));
      cdmSet.setIdentifiers(
          List.of(
              new DefaultIdentifier(
                  new DefaultCitation(new EDDInternationalString("Unidata Common Data Model")),
                  cdmDataType)));
      cdmResource.setName(cdmSet);
      resources.add(cdmResource);
    }
    dataId.setAssociatedResources(resources);
    dataId.setTopicCategories(List.of(TopicCategory.GEOSCIENTIFIC_INFORMATION));
    dataId.setExtents(List.of(extent));

    String datasetUrl =
        EDStatic.preferredErddapUrl + (isGriddap ? "/griddap/" : "/tabledap/") + datasetId;

    List<Identification> identifications = new ArrayList<>();
    identifications.add(dataId);

    DefaultCitation serviceCitation = new DefaultCitation();
    serviceCitation.setDates(dates);
    serviceCitation.setTitle(new EDDInternationalString(attributes.getString("title")));
    serviceCitation.setCitedResponsibleParties(citationContacts);
    DefaultServiceIdentification erddapService = new DefaultServiceIdentification();
    erddapService.setCitation(serviceCitation);
    erddapService.setExtents(List.of(extent));
    // used to set couplingType, not in this api, instead using coupled resource
    if (isGriddap) {
      erddapService.setServiceType(
          DefaultNameFactory.provider().createLocalName(null, "ERDDAP griddap"));
      DefaultCoupledResource coupledResource = new DefaultCoupledResource();
      coupledResource.setResources(List.of(dataId));
      DefaultOperationMetadata operationMetadata = new DefaultOperationMetadata();
      operationMetadata.setOperationName("ERDDAPgriddapDatasetQueryAndAccess");
      DefaultOnlineResource connectPoint = new DefaultOnlineResource();
      connectPoint.setLinkage(URI.create(datasetUrl));
      connectPoint.setProtocol("ERDDAP:griddap");
      connectPoint.setName("ERDDAP-griddap");
      connectPoint.setDescription(
          new EDDInternationalString(
              "ERDDAP's griddap service (a flavor of OPeNDAP) "
                  + "for gridded data. Add different extensions (e.g., .html, .graph, .das, .dds) "
                  + "to the base URL for different purposes."));
      connectPoint.setFunction(OnLineFunction.DOWNLOAD);
      operationMetadata.setConnectPoints(List.of(connectPoint));
      coupledResource.setOperation(operationMetadata);
      erddapService.setCoupledResources(List.of(coupledResource));
    } else {
      erddapService.setServiceType(
          DefaultNameFactory.provider().createLocalName(null, "ERDDAP tabledap"));
      DefaultCoupledResource coupledResource = new DefaultCoupledResource();
      coupledResource.setResources(List.of(dataId));
      DefaultOperationMetadata operationMetadata = new DefaultOperationMetadata();
      operationMetadata.setOperationName("ERDDAPtabledapDatasetQueryAndAccess");
      DefaultOnlineResource connectPoint = new DefaultOnlineResource();
      connectPoint.setLinkage(URI.create(datasetUrl));
      connectPoint.setProtocol("ERDDAP:tabledap");
      connectPoint.setName("ERDDAP-tabledap");
      connectPoint.setDescription(
          new EDDInternationalString(
              "ERDDAP's tabledap service (a flavor of OPeNDAP) "
                  + "for tabular (sequence) data. Add different extensions "
                  + "(e.g., .html, .graph, .das, .dds) to the base URL for different purposes."));
      connectPoint.setFunction(OnLineFunction.DOWNLOAD);
      operationMetadata.setConnectPoints(List.of(connectPoint));
      coupledResource.setOperation(operationMetadata);
      erddapService.setCoupledResources(List.of(coupledResource));
    }
    identifications.add(erddapService);

    DefaultServiceIdentification opendapService = new DefaultServiceIdentification();
    opendapService.setCitation(serviceCitation);
    opendapService.setExtents(List.of(extent));
    opendapService.setServiceType(DefaultNameFactory.provider().createLocalName(null, "OPeNDAP"));
    DefaultCoupledResource openResource = new DefaultCoupledResource();
    openResource.setResources(List.of(dataId));
    DefaultOperationMetadata openOperationMetadata = new DefaultOperationMetadata();
    openOperationMetadata.setOperationName("OPeNDAPDatasetQueryAndAccess");
    DefaultOnlineResource openConnectPoint = new DefaultOnlineResource();
    openConnectPoint.setLinkage(URI.create(datasetUrl));
    openConnectPoint.setProtocol("OPeNDAP:OPeNDAP");
    openConnectPoint.setName("OPeNDAP");
    openConnectPoint.setDescription(
        new EDDInternationalString(
            "An OPeNDAP service for tabular (sequence) data. "
                + "Add different extensions (e.g., .html, .das, .dds) to the "
                + "base URL for different purposes"));
    openConnectPoint.setFunction(OnLineFunction.DOWNLOAD);
    openOperationMetadata.setConnectPoints(List.of(openConnectPoint));
    openResource.setOperation(openOperationMetadata);
    opendapService.setCoupledResources(List.of(openResource));
    identifications.add(opendapService);

    if (accessibleViaWms) {
      DefaultServiceIdentification wmsService = new DefaultServiceIdentification();
      wmsService.setCitation(serviceCitation);
      wmsService.setExtents(List.of(extent));
      wmsService.setServiceType(DefaultNameFactory.provider().createLocalName(null, "OPeNDAP"));
      DefaultCoupledResource wmsResource = new DefaultCoupledResource();
      wmsResource.setResources(List.of(dataId));
      DefaultOperationMetadata wmsOperationMetadata = new DefaultOperationMetadata();
      wmsOperationMetadata.setOperationName("GetCapabilities");
      DefaultOnlineResource wmsConnectPoint = new DefaultOnlineResource();
      wmsConnectPoint.setLinkage(
          URI.create(
              EDStatic.preferredErddapUrl
                  + "/wms/"
                  + datasetId
                  + "/"
                  + EDD.WMS_SERVER
                  + "?service=WMS&version=1.3.0&request=GetCapabilities"));
      wmsConnectPoint.setProtocol("OGC:WMS");
      wmsConnectPoint.setName("OGC-WMS");
      wmsConnectPoint.setDescription(
          new EDDInternationalString("Open Geospatial Consortium Web Map Service (WMS)"));
      wmsConnectPoint.setFunction(OnLineFunction.DOWNLOAD);
      wmsOperationMetadata.setConnectPoints(List.of(wmsConnectPoint));
      wmsResource.setOperation(wmsOperationMetadata);
      wmsService.setCoupledResources(List.of(wmsResource));
      identifications.add(wmsService);
    }

    if (accessibleViaSubset) {
      DefaultServiceIdentification subsetService = new DefaultServiceIdentification();
      subsetService.setCitation(serviceCitation);
      subsetService.setExtents(List.of(extent));
      subsetService.setServiceType(DefaultNameFactory.provider().createLocalName(null, "OPeNDAP"));
      DefaultCoupledResource subResource = new DefaultCoupledResource();
      subResource.setResources(List.of(dataId));
      DefaultOperationMetadata subOperationMetadata = new DefaultOperationMetadata();
      subOperationMetadata.setOperationName("ERDDAP_Subset");
      DefaultOnlineResource subConnectPoint = new DefaultOnlineResource();
      subConnectPoint.setLinkage(URI.create(datasetUrl + ".subset"));
      subConnectPoint.setProtocol("search");
      subConnectPoint.setName("Subset");
      subConnectPoint.setDescription(
          new EDDInternationalString("Web page to facilitate selecting subsets of the dataset"));
      subConnectPoint.setFunction(OnLineFunction.DOWNLOAD);
      subOperationMetadata.setConnectPoints(List.of(subConnectPoint));
      subResource.setOperation(subOperationMetadata);
      subsetService.setCoupledResources(List.of(subResource));
      identifications.add(subsetService);
    }
    metadata.setIdentificationInfo(identifications);

    DefaultDistribution distribution = new DefaultDistribution();
    DefaultDigitalTransferOptions transferOptions = new DefaultDigitalTransferOptions();
    DefaultOnlineResource subsetResource = new DefaultOnlineResource();
    subsetResource.setLinkage(URI.create(datasetUrl + ".html"));
    subsetResource.setProtocol("order");
    subsetResource.setName("Data Subset Form");
    subsetResource.setDescription(
        new EDDInternationalString(
            "ERDDAP's version of the OPeNDAP .html web page for this dataset. "
                + "Specify a subset of the dataset and download the data via OPeNDAP "
                + "or in many different file types."));
    subsetResource.setFunction(OnLineFunction.DOWNLOAD);
    DefaultOnlineResource graphResource = new DefaultOnlineResource();
    graphResource.setLinkage(URI.create(datasetUrl + ".graph"));
    graphResource.setProtocol("order");
    graphResource.setName("Make-A-Graph Form");
    graphResource.setDescription(
        new EDDInternationalString(
            "ERDDAP's Make-A-Graph .html web page for this dataset. "
                + "Create an image with a map or graph of a subset of the data."));
    graphResource.setFunction(OnLineFunction.DOWNLOAD);
    transferOptions.setOnLines(List.of(subsetResource, graphResource));
    DefaultFormat distributionFormat = new DefaultFormat();
    DefaultCitation openDapCitation = new DefaultCitation();
    openDapCitation.setTitle(new EDDInternationalString("OPeNDAP"));
    openDapCitation.setEdition(new EDDInternationalString(EDStatic.dapVersion));
    distributionFormat.setFormatSpecificationCitation(openDapCitation);

    DefaultDistributor distributor = new DefaultDistributor();
    distributor.setDistributorContact(adminContact(Role.DISTRIBUTOR));
    distributor.setDistributorFormats(List.of(distributionFormat));
    distributor.setDistributorTransferOptions(List.of(transferOptions));
    distribution.setDistributors(List.of(distributor));
    distribution.setDistributionFormats(List.of(distributionFormat));
    metadata.setDistributionInfo(distribution);

    if (String2.isSomething(attributes.getString("history"))) {
      DefaultDataQuality dataQual = new DefaultDataQuality();
      DefaultLineage dataLineage = new DefaultLineage();
      dataLineage.setStatement(new EDDInternationalString(attributes.getString("history")));
      dataQual.setLineage(dataLineage);
    }
    DefaultMaintenanceInformation maintenanceInfo = new DefaultMaintenanceInformation();
    maintenanceInfo.setMaintenanceNotes(
        List.of(
            new EDDInternationalString(
                "This record was created from dataset metadata by ERDDAP Version "
                    + EDStatic.erddapVersion)));
    metadata.setMetadataMaintenance(maintenanceInfo);

    String coverageType = attributes.getString("coverage_content_type"); // used by GOES-R
    CoverageContentType contentType = CoverageContentType.PHYSICAL_MEASUREMENT;
    if ("image".equals(coverageType)) {
      contentType = CoverageContentType.IMAGE;
    } else if ("thematicClassification".equals(coverageType)) {
      contentType = CoverageContentType.THEMATIC_CLASSIFICATION;
    }
    DefaultCoverageDescription coverageDescription = new DefaultCoverageDescription();
    DefaultAttributeGroup attributeGroup = new DefaultAttributeGroup();
    attributeGroup.setContentTypes(List.of(contentType));
    attributeGroup.setAttributes(rangeDimensions);
    coverageDescription.setAttributeGroups(List.of(attributeGroup));
    metadata.setContentInfo(List.of(coverageDescription));

    return metadata;
  }

  private static ResponsibleParty adminContact(Role role) {
    DefaultResponsibleParty admin = new DefaultResponsibleParty();
    admin.setRole(role);
    DefaultContact contact = new DefaultContact();
    DefaultTelephone phone = new DefaultTelephone();
    phone.setNumber(EDStatic.adminPhone);
    phone.setNumberType(UnsupportedCodeList.VOICE);
    contact.setPhones(List.of(phone));
    DefaultAddress address = new DefaultAddress();
    address.setDeliveryPoints(List.of(EDStatic.adminAddress));
    address.setCity(new EDDInternationalString(EDStatic.adminCity));
    address.setAdministrativeArea(new EDDInternationalString(EDStatic.adminStateOrProvince));
    address.setCountry(new EDDInternationalString(EDStatic.adminCountry));
    address.setPostalCode(EDStatic.adminPostalCode);
    address.setElectronicMailAddresses(List.of(EDStatic.adminEmail));
    contact.setAddresses(List.of(address));
    DefaultIndividual individual =
        new DefaultIndividual(EDStatic.adminIndividualName, EDStatic.adminPosition, contact);
    DefaultOrganisation institution =
        new DefaultOrganisation(EDStatic.adminInstitution, null, individual, contact);
    admin.setParties(List.of(individual, institution));
    return admin;
  }

  private static ResponsibleParty buildResponsibleParty(
      String name, String institution, String position, String email, String url, Role role) {
    DefaultResponsibleParty responsibleParty = new DefaultResponsibleParty();
    DefaultContact contact = new DefaultContact();
    DefaultAddress address = new DefaultAddress();
    address.setElectronicMailAddresses(List.of(email));
    contact.setAddresses(List.of(address));
    if (String2.isSomething(url)) {
      DefaultOnlineResource infoLink = new DefaultOnlineResource();
      infoLink.setLinkage(URI.create(url));
      infoLink.setProtocol("information");
      infoLink.setApplicationProfile("web browser");
      infoLink.setName("Background Information");
      infoLink.setDescription(new EDDInternationalString("Background information from the source"));
      infoLink.setFunction(OnLineFunction.INFORMATION);
      contact.setOnlineResources(List.of(infoLink));
    }
    List<AbstractParty> parties = new ArrayList<>();
    DefaultIndividual individual = null;
    if (String2.isSomething(name) || String2.isSomething(position)) {
      individual = new DefaultIndividual(name, position, contact);
      parties.add(individual);
    }
    if (String2.isSomething(institution)) {
      parties.add(new DefaultOrganisation(institution, null, individual, contact));
    }
    responsibleParty.setParties(parties);
    return responsibleParty;
  }

  private static ResponsibleParty responsiblePartyWithRole(ResponsibleParty party, Role role) {
    DefaultResponsibleParty newParty = new DefaultResponsibleParty(party);
    newParty.setRole(role);
    return newParty;
  }

  private static Dimension dimFromVariable(EDV dataVariable) {
    // TODO size and resolution aren't currently available. Can we add them to EDV?
    DefaultDimension dim = new DefaultDimension();
    if (dataVariable instanceof EDVLon) {
      dim.setDimensionName(DimensionNameType.COLUMN);
    } else if (dataVariable instanceof EDVLat) {
      dim.setDimensionName(DimensionNameType.ROW);
    } else if (dataVariable instanceof EDVAlt || dataVariable instanceof EDVDepth) {
      dim.setDimensionName(DimensionNameType.VERTICAL);
    } else if (dataVariable instanceof EDVTime || dataVariable instanceof EDVTimeStamp) {
      dim.setDimensionName(DimensionNameType.TIME);
    } else {
      // TODO this might not be right, old code just didn't include it
      dim.setDimensionName(DimensionNameType.SAMPLE);
    }
    return dim;
  }

  private static Dimension dimFromAxis(EDVGridAxis axis) {
    DefaultDimension dim = new DefaultDimension();
    dim.setDimensionSize(axis.sourceValues().size());
    dim.setResolution(axis.averageSpacing());
    if (axis instanceof EDVLonGridAxis) {
      dim.setDimensionName(DimensionNameType.COLUMN);
    } else if (axis instanceof EDVLatGridAxis) {
      dim.setDimensionName(DimensionNameType.ROW);
    } else if (axis instanceof EDVAltGridAxis || axis instanceof EDVDepthGridAxis) {
      dim.setDimensionName(DimensionNameType.VERTICAL);
    } else if (axis instanceof EDVTimeGridAxis || axis instanceof EDVTimeStampGridAxis) {
      dim.setDimensionName(DimensionNameType.TIME);
    } else {
      // TODO this might not be right, old code just said UNKNOWN
      dim.setDimensionName(DimensionNameType.SAMPLE);
    }
    return dim;
  }

  private static Collection<? extends Keywords> getKeywords(
      Attributes attributes, List<InternationalString> standardNames) {
    List<Keywords> keywordsList = new ArrayList<>();
    String keywords = attributes.getString("keywords");
    if (keywords == null) { // use the crude, ERDDAP keywords
      keywords = EDStatic.keywords;
    }
    String project = attributes.getString("project");
    if (project == null) project = attributes.getString("institution");
    ;
    String standardNameVocabulary = attributes.getString("standard_name_vocabulary");
    if (keywords != null) {
      StringArray kar = StringArray.fromCSVNoBlanks(keywords);

      // segregate gcmdKeywords and remove cf standard_names
      List<EDDInternationalString> gcmdKeywords = new ArrayList<>();
      for (int i = kar.size() - 1; i >= 0; i--) {
        String kari = kar.get(i);
        if (kari.indexOf(" > ") > 0) {
          gcmdKeywords.add(new EDDInternationalString(kari));
          kar.remove(i);

        } else if (kari.indexOf('_') > 0) {
          kar.remove(i); // a multi-word CF Standard Name

        } else if (kari.length() <= EDD.LONGEST_ONE_WORD_CF_STANDARD_NAMES
            && // quick accept if short enough
            EDD.ONE_WORD_CF_STANDARD_NAMES.indexOf(kari) >= 0) { // few, so linear search is quick
          kar.remove(i); // a one word CF Standard Name
        }
      }

      List<InternationalString> themeKeywords = new ArrayList<>();
      // keywords not from vocabulary
      if (kar.size() > 0) {
        for (int i = 0; i < kar.size(); i++) {
          themeKeywords.add(new EDDInternationalString(kar.get(i)));
        }
        DefaultKeywords keywordsObj = new DefaultKeywords();
        keywordsObj.setKeywords(themeKeywords);
        keywordsObj.setType(KeywordType.THEME);
        keywordsList.add(keywordsObj);
      }

      if (gcmdKeywords.size() > 0) {
        DefaultKeywords gcmdWords = new DefaultKeywords();
        gcmdWords.setKeywords(gcmdKeywords);
        gcmdWords.setType(KeywordType.THEME);
        DefaultCitation citation = new DefaultCitation();
        citation.setTitle(new EDDInternationalString("GCMD Science Keywords"));
        gcmdWords.setThesaurusName(citation);
        keywordsList.add(gcmdWords);
      }
    }

    if (project != null) {
      DefaultKeywords projectWords = new DefaultKeywords();
      projectWords.setKeywords(List.of(new EDDInternationalString(project)));
      projectWords.setType(KeywordType.THEME);
      keywordsList.add(projectWords);
    }

    // keywords - variable (CF) standard_names
    if (standardNames.size() > 0) {
      DefaultKeywords standardWords = new DefaultKeywords();
      standardWords.setKeywords(standardNames);
      standardWords.setType(KeywordType.THEME);
      DefaultCitation standardCitation = new DefaultCitation();
      standardCitation.setTitle(
          new EDDInternationalString(
              standardNameVocabulary == null ? "CF" : standardNameVocabulary));
      standardWords.setThesaurusName(standardCitation);
      keywordsList.add(standardWords);
    }
    return keywordsList;
  }
}
