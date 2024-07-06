package gov.noaa.pfel.erddap.handler;

import gov.noaa.pfel.erddap.handlers.SaxHandler;
import gov.noaa.pfel.erddap.handlers.TopLevelHandler;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import testDataset.Initialization;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopLevelHandlerTests {

    private static TopLevelHandler topLevelHandler;
    private static SAXParserFactory factory;
    private static SAXParser saxParser;
    private static InputStream inputStream;
    private static SaxHandler saxHandler;

    @BeforeAll
    static void initAll() throws ParserConfigurationException, SAXException {
        Initialization.edStatic();

        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        saxParser = factory.newSAXParser();
        saxHandler = new SaxHandler();
        saxHandler.setState(topLevelHandler);
    }

    @BeforeEach
    void init() {
        topLevelHandler = new TopLevelHandler(saxHandler, new StringBuilder(), new HashMap<>());
        inputStream = TopLevelHandlerTests.class.getResourceAsStream("/datasets/topLevelHandlerTest.xml");
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found: /datasets/topLevelHandlerTest.xml");
        }
    }

    @Test
    void convertToPublicSourceUrlTest() throws SAXException, IOException {
        saxParser.parse(inputStream, saxHandler);
        assertEquals(EDStatic.convertToPublicSourceUrl.get("http://example.com/"), "http://public.example.com/");
    }

    @Test
    void angularDegreeUnitsTest() throws SAXException, IOException {
        saxParser.parse(inputStream, saxHandler);
        assertEquals(EDStatic.angularDegreeUnitsSet.toString(), "[angular, for, degree, units, content]");
    }

    @Test
    void unusualActivityTest() throws IOException, SAXException {
        saxParser.parse(inputStream, saxHandler);
        assertEquals(EDStatic.unusualActivity, 25);
    }

    @Test
    void userTest() throws IOException, SAXException {
        var tUserHashMap = new HashMap();
        topLevelHandler = new TopLevelHandler(saxHandler, new StringBuilder(), tUserHashMap);
        saxHandler.setState(topLevelHandler);
        saxParser.parse(inputStream, saxHandler);
        Object[] user1Data = (Object[]) tUserHashMap.get("user1");
        assertEquals("pass1", user1Data[0]);
    }

}
