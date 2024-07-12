/*
 * $Id: GeoDateDialog.java,v 1.2 2001/02/08 00:29:39 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.swing.prop;

import gov.noaa.pmel.util.GeoDate;

import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.swing.event.ListSelectionListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * <code>GeoDateDialog</code> is a calendar, plus optionally, time, chooser 
 * that produces a date.  It allows the invoker to set the 
 * allowable range of dates by specifying an earliest and latest 
 * allowable date.  The user can select a date to within 
 * 5 minutes.  If the hour and minutes aren't needed, a flag 
 * allows the exclusion of the display of these fields.  
 *
 * @author Chris Windsor
 * @version $Revision: 1.2 $, $Date: 2001/02/08 00:29:39 $
 * @since 2.0
 */
public class GeoDateDialog extends JDialog
        implements ItemListener,
                  PropertyChangeListener,
                   ActionListener {
  // Get-able, Set-able variables
  private GeoDate initialDate = new GeoDate();
  private GeoDate earliestDateAllowed = new GeoDate();
  private GeoDate latestDateAllowed = new GeoDate();
  private boolean earliestCheckingEnabled = false;
  private boolean latestCheckingEnabled = false;
  private Font regularFont, boldFont;
  private Color panelBackground, calBackground, selectedButnBackground;

  private final static String[] months = {"Jan","Feb","Mar",
                                          "Apr","May","Jun",
                                          "Jul","Aug","Sep",
                                          "Oct", "Nov","Dec"};
  private final static String[] daysOfMonth = {" 1"," 2"," 3"," 4"," 5",
                                               " 6"," 7"," 8"," 9","10",
                                               "11","12","13","14","15",
                                               "16","17","18","19","20",
                                               "21","22","23","24","25",
                                               "26","27","28","29","30",
                                               "31"};
  private final static String[] hours = {"0","1","2","3","4","5","6","7",
                                         "8","9","10","11","12","13","14",
                                         "15","16","17","18","19","20","21",
                                         "22","23"};
  private final static String[] minutes =  {"0","5","10","15","20","25",
                                            "30","35","40","45","50","55"};
  private final static int numWeeks = 6;

  private GregorianCalendar cal;
  private SimpleDateFormat dateFormatter;
  private SimpleDateFormat sdf;
  private SimpleDateFormat tsdf = new SimpleDateFormat("dd MMM yyyy ");
  private TimeZone tz = TimeZone.getTimeZone("GMT"); //2015-09-02 was static

  private int result_;
  public static int OK_RESPONSE = 1;
  public static int CANCEL_RESPONSE = 2;

  private JPanel mainPanel_;
  private JPanel calPanel, okPanel, selectionPanel;
  private javax.swing.Box box, theBox, timeBox, monthYearBox, okBox, labelBox,
    hrMinBox;
  private JButton subYearButn, subMonthButn, addMonthButn, addYearButn,
    addHourButn, subHourButn, addMinButn, subMinButn;
  private JButton cancelButn, okButn;
  private JToggleButton calButtons[ ];
  private JLabel yearLabel, hourLabel, minLabel, hourMinLabel;
  private JLabel selectionLabel;
  private Choice monthChoice, yearChoice;
  private JComboBox monthList, minList;
  private JTextField hourText, yearText;
  private Component caller;

  private ButtonGroup calButtonGroup;
   
  private GeoDate liquidDate;
  private int lastDaySel, lastButnSel = 1;
  private int xloc, yloc;
  private String title;
  private boolean hideTime;
  //  private JFrame aJFrame = null;
  static private CompoundBorder cp = new CompoundBorder( 
                        new BevelBorder( BevelBorder.RAISED ),
                        new EmptyBorder( 2,2,2,2));

  public static final int DATE = Calendar.DATE;
  public static final int YEAR = Calendar.YEAR;
  public static final int MONTH = Calendar.MONTH;
  public static final int MINUTE = Calendar.MINUTE;
  public static final int HOUR_OF_DAY = Calendar.HOUR_OF_DAY;

  private boolean TRACE = false;
  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  // Get/Set Routines ------------------------------------------------------
  public void setEarliestDateAllowed( GeoDate earliestDateAllowed ) {
    this.earliestDateAllowed = earliestDateAllowed;
    earliestCheckingEnabled = true;
    System.out.println("Earliest date allowed: " + earliestDateAllowed);
  }
  public void setLatestDateAllowed( GeoDate latestDateAllowed ) {
    this.latestDateAllowed = latestDateAllowed;
    latestCheckingEnabled = true;
    System.out.println("Latest date allowed: " + latestDateAllowed);
  }
  public GeoDate getEarliestDateAllowed() {
    return( earliestDateAllowed );
  }
  public GeoDate getLatestDateAllowed() {
    return( latestDateAllowed );
  }
  //  public void setTimeZone( TimeZone tz ) {
  //    this.tz = tz;
  //    setTimeZoneForTimeObjects( tz );
  //  }

  //  public TimeZone getTimeZone( ) {
  //    return this.tz;
  //  }

    public void setOutputDateFormatter( SimpleDateFormat sdf) {
      this.sdf = sdf;
    }

  public void setInitialDate( GeoDate initialDateIn) {
    initialDate = initialDateIn;
    liquidDate = new GeoDate( initialDate );
    this.setTitle( composeTimeLabel( liquidDate));
  }
  public void setGeoDate( GeoDate dt) {
    if (TRACE) System.out.println( "setDate entered");
    liquidDate = new GeoDate( dt );
    if (liquidDateWithinBounds()) {
      updateGUIAfterLiquidDateChange();
      this.setTitle( composeTimeLabel( liquidDate));
    }
  }
  public void setTitle( String title ) {
    this.title = title;
  }
  public String getTitle( ) {
    return this.title;
  }
  public GeoDate getInitialDate() {
    return( initialDate );
  }
  public void setRegularFont( Font regularFontIn ) {
    regularFont = regularFontIn;
  }
  public Font getRegularFont() {
    return( regularFont );
  }
  public void setBoldFont( Font boldFontIn ) {
    boldFont = boldFontIn;
  }
  public Font getBoldFont( ) {
    return( boldFont );
  }
  public void setPanelBackground( Color color ) {
    panelBackground = color;
  }
  public Color getPanelBackground( ) {
    return( panelBackground );
  }
  public void setCalBackground( Color color ) {
    calBackground = color;
  }
  public Color getCalBackground( ) {
    return( calBackground );
  }
  public void setSelectedButnBackground( Color color ) {
    selectedButnBackground = color;
  }
  public Color getSelectedButnBackground( ) {
    return( selectedButnBackground );
  }
  public void setHideTime( boolean ans) {
    hideTime = ans;
    if (ans) {
      removeTime();
    }
  }
  public boolean getHideTime() {
    return hideTime;
  }
  //  public JButton getOkButn() {
  //    return okButn;
  //  }
  //  public JButton getCancelButn() {
  //    return cancelButn;
  //  }
  //----------------------------------------------------------------
  // Constructor
  public GeoDateDialog( GeoDate inDate ) {

    super();
    //    tz = TimeZone.getDefault();
    cal = new GregorianCalendar();
    cal.setTimeZone(tz);
    createDateFormatter();
    setTimeZoneForTimeObjects( );
    setInitialDate( inDate ); 
    finishConstruction();
  }

  //----------------------------------------------------------------
  // Constructor
  public GeoDateDialog( GeoDate inDate, GeoDate earliestDateAllowedIn,
                          GeoDate latestDateAllowedIn, int xlocIn, int ylocIn) {

    super();
    //    tz = TimeZone.getDefault();
    cal = new GregorianCalendar();
    cal.setTimeZone(tz);
    createDateFormatter();
    setTimeZoneForTimeObjects( );
    xloc = xlocIn;
    yloc = ylocIn;
    setInitialDate( inDate ); 
    setEarliestDateAllowed( earliestDateAllowedIn );
    setLatestDateAllowed( latestDateAllowedIn );
    finishConstruction();
  }

  //----------------------------------------------------------------
  // Constructor
  public GeoDateDialog( ) {

    super();
    //    tz = TimeZone.getDefault();
    cal = new GregorianCalendar();
    cal.setTimeZone(tz);
    createDateFormatter();
    setTimeZoneForTimeObjects( );
    // Initialize date to current, but round hours and minutes to 0
    GeoDate dt = new GeoDate();
    cal.setTime( dt );
    computeFields( cal );
    cal.set(HOUR_OF_DAY,0); cal.set(MINUTE,0); 
    cal.set( cal.SECOND,0);
    dt = new GeoDate(cal.getTime());
    setInitialDate( dt ); 

    finishConstruction();
  }
  //----------------------------------------------------------------
  // Finish Construction
  // 
  void finishConstruction( ) {

    mainPanel_ = new JPanel();

    this.addPropertyChangeListener( this );
    //    mainPanel_.setSize(220 ,323);

    regularFont = new Font( "Dialog", Font.PLAIN, 10);
    boldFont = new Font( "Dialog", Font.BOLD, 10);
    setSelectedButnBackground( new Color(240, 240, 240 ));
    setCalBackground( new Color(200,200,200 ));

    createCalendarPanel();
    createMonthYearPanel();
    createTimePanel();
    createOkPanel();
    createSelectionPanel();

    theBox = box.createVerticalBox();
    theBox.add( monthYearBox );
    theBox.add( calPanel);
    theBox.add( timeBox);
    theBox.add( box.createVerticalStrut( 1 ));
    theBox.add( selectionPanel);
    theBox.add( okPanel);
    theBox.add( box.createVerticalGlue()); 

    theBox.validate();
    theBox.setBackground( panelBackground );
    theBox.setBackground( Color.white );
    theBox.repaint();
    //this.getContentPane().add( theBox );
    mainPanel_.add( theBox );
    theBox.setLocation( 10, 10 );
    mainPanel_.setLocation( xloc, yloc);
    this.getContentPane().add(mainPanel_);
    validate();
    repaint();

    cal.setTime( getInitialDate());
    computeFields( cal );
    lastDaySel = cal.get( DATE );

    resetCalendarPanel( initialDate );
    softwareDayOfMonthClick( initialDate );

    minList.setSelectedIndex( (cal.get(MINUTE)/5));
    hourText.setText( new String(String.valueOf(cal.get(HOUR_OF_DAY))));
    monthList.setSelectedIndex( cal.get( MONTH));

  }
  //----------------------------------------------------------------
  // showInJFrame
  //
  //  show this DateTimeGetter in a JFrame; handles all window closing
  //
  public int showDialog(GeoDate date, int x, int y) {
    this.setSize(220 ,323);
    this.setLocation(x, y);
//      aJFrame.addWindowListener( new WindowAdapter() {
//      public void windowClosing( WindowEvent e) {
//        closeDown();
//      }
//        });
    setInitialDate(date);
    setGeoDate(date);
    result_ = CANCEL_RESPONSE;
    this.setModal(true);
    this.setVisible( true );
    return result_;
  }

  //----------------------------------------------------------------
  // Removes the time-related selectors
  //
  void removeTime() {
    theBox.remove(timeBox); 
    createDateFormatter();
    updateDateLabel();
    this.setSize( 220 ,260);
  }

  //----------------------------------------------------------------
  //
  // for all objects that use timezone.
  // 
  void setTimeZoneForTimeObjects() {
      if (TRACE) System.out.println( "setTimeZoneForTimeObjects entered");
      if (dateFormatter != null) dateFormatter.setTimeZone( tz );
      if (cal != null) cal.setTimeZone( tz );
    }


  //----------------------------------------------------------------
  // Create date formatter; need to create this only once for use
  // many times.
  void createDateFormatter() {

    if (TRACE) System.out.println( "createDateFormatter entered");
    dateFormatter = new SimpleDateFormat(" dd MMM yyyy HH:mm ");
    if (hideTime) {
      dateFormatter = new SimpleDateFormat("dd MMM yyyy ");
    }
    dateFormatter.setTimeZone( tz );
  }

  //----------------------------------------------------------------
  // create Calendar panel
  //----------------------------------------------------------------
  // The calendar panel has the grid layout of 7 rows, 7 columns,
  // (One label row; 6 week rows)
  // 

  void createCalendarPanel( ) {

    if (TRACE) System.out.println( "createCalendarPanel entered");
    int dayOfWeek, weekOfMonth;

    JLabel jl;
    // Create a panel for the calendar
    calPanel = new JPanel();
    calPanel.setLayout( new GridLayout( numWeeks+1, 7, 2, 1 ));
    jl = new JLabel("Sun", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    jl = new JLabel("Mon", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    jl = new JLabel("Tue", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    jl = new JLabel("Wed", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    jl = new JLabel("Thu", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    jl = new JLabel("Fri", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    jl = new JLabel("Sat", JLabel.CENTER); jl.setFont(boldFont);
    calPanel.add( jl);
    calPanel.setBorder( new EmptyBorder( 2,2,2,2));

    calButtonGroup = new ButtonGroup();
    calButtons = new JToggleButton[ numWeeks * 7 ];
    for (int i = 0; i < (7*numWeeks); i++ ) {
      calButtons[ i ] = new JToggleButton("  ");
      calButtons[ i ].setBorder( cp );
      calButtons[ i ].setContentAreaFilled( true );
      calButtons[ i ].addActionListener( this );
      calButtons[ i ].setFont(regularFont);
      calPanel.add( calButtons[ i ] );
      calButtonGroup.add( calButtons[ i ] );
      calButtons[ i ].setMinimumSize( new Dimension( 17, 21 ));
      calButtons[ i ].setMaximumSize( new Dimension( 17, 21 ));
      calButtons[ i ].setSize( new Dimension( 17, 21 ));
    }

  }  // createCalendarPanel

  //----------------------------------------------------------------
  // reset Calendar Panel when the date changes to reflect new month.
  //----------------------------------------------------------------
  void resetCalendarPanel( GeoDate newDate ) {

    if (TRACE) System.out.println( "resetCalendarPanel entered");
    int dayOfWeek, weekOfMonth;
    String[][] gridLabels = new String[7][numWeeks];
    GregorianCalendar cal1 = new GregorianCalendar();
    cal1.setTimeZone( tz );

    // Set labels in grid to "  " 
    for (dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {   // monday -> sunday
      for (weekOfMonth = 0; weekOfMonth < numWeeks; weekOfMonth++) {
        gridLabels[ dayOfWeek][ weekOfMonth ] = "  ";
      }
    }

    // Get first day of this month
    cal1.setTime( newDate );
    computeFields( cal1 );
    cal1.set( cal1.DATE, 1 );
    Date firstThisMonth = cal1.getTime();

    //System.out.println(" firstThisMonth: " + tsdf.format( firstThisMonth));
    // Get first of next month's date
    cal1.setTime( firstThisMonth );
    computeFields( cal1 );
    dayOfWeek = cal1.get( cal1.DAY_OF_WEEK ) - 1;       // added the subtract 12/99
    //System.out.println(" day of week is: " + dayOfWeek);

    // Now set calendar to next month
    if ((cal1.get(cal1.MONTH)+1) == 13) {  // crossing over to the next year
      cal1.set(cal1.MONTH, 0 );
      cal1.set(cal1.YEAR, cal1.get( cal1.YEAR ) +1 );
    }
    else
      cal1.set(cal1.MONTH, cal1.get( cal1.MONTH )+1 );


    Date firstNextMonth = cal1.getTime();
    //System.out.println(" firstNextMonth: " + tsdf.format( firstNextMonth));

    long curTime = firstThisMonth.getTime();
    long endTime = firstNextMonth.getTime();
    cal1.setTime( firstThisMonth);
    computeFields( cal1 );

    weekOfMonth = 0;
    int dayOfMonth = 1;
    while (curTime < endTime) {
      // Set labels in grid
      if (dayOfMonth != 32) // October; daylight saving time causes bug
        gridLabels[ dayOfWeek][ weekOfMonth ] = daysOfMonth[ dayOfMonth-1 ];

      dayOfWeek++;
      dayOfMonth++;
      if (dayOfWeek > 6) {
        dayOfWeek = 0;
        weekOfMonth++;
      }
      curTime+= (24 * 60 * 60 * 1000);  // add 24 hours
    }

    int ithButn = 0;
    for (weekOfMonth = 0; weekOfMonth < numWeeks; weekOfMonth++) {
      for (dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {// monday -> sunday
        if (gridLabels[ dayOfWeek][weekOfMonth].equals("  ")) {
          calButtons[ ithButn ].setText( "  ");
          calButtons[ ithButn ].setVisible(false);
        }
        else {
          calButtons[ ithButn ].setText( 
                                        gridLabels[ dayOfWeek][weekOfMonth]);
          calButtons[ ithButn ].setVisible(true);
        }
        ithButn++;
      }
    }
  }
  //----------------------------------------------------------------
  // closeDown 
  //
  void closeDown() {
    this.setVisible( false );
  }

  public GeoDate getGeoDate() {
    return liquidDate;
  }

  //----------------------------------------------------------------
  void softwareDayOfMonthClick( GeoDate dt) {

    if (TRACE) System.out.println( "softwareDayOfMonthClick entered");
    cal.setTime( dt );
    computeFields( cal );
    int date = cal.get( DATE );
    int idx = 0;
    for (int i = 0; i < calButtons.length; i++) {
      if (calButtons[i].getText().equals( daysOfMonth[ date-1 ])) {
        calButtons[i].doClick(); 
      }
    }
  }  

  //----------------------------------------------------------------
  void selectDay( int date) {

    if (TRACE) System.out.println( "selectDay entered");
    if (calButtons != null) {
      for (int i = 0; i < calButtons.length; i++) {
        if (calButtons[i].getText().equals( daysOfMonth[ date-1 ])) {
          lastButnSel = i;
          lastDaySel = date;
          break;
        }
      }
    }
  }  // selectDay

  // --------------------------------------------------------------
  // Compose time label
  String composeTimeLabel( GeoDate dt ) {
    return( dateFormatter.format( dt ));
  }
  //----------------------------------------------------------------
  // computeFields
  //  forces the Calendar object to compute its fields.  (Unfortunately,
  //  the computeFields method of the object is protected, so I can't
  //   call it.)
  //----------------------------------------------------------------
  void computeFields( Calendar caln ) {
    {int yrr=caln.get(caln.YEAR);} // FORCE a call to computeFields 
  }

  //----------------------------------------------------------------
  // update Date Label
  //----------------------------------------------------------------
  void updateDateLabel( ) {

    //this.setTitle( composeTimeLabel( liquidDate ));
    selectionLabel.setText( composeTimeLabel( liquidDate ));
  }
  //----------------------------------------------------------------
  // create Month/Year panel
  //----------------------------------------------------------------
  // The Month/Year panel has the following grid bag layout structure:
  //
  //         x-0    x-1    x-2    x-3    x-4    x-5    x-6    
  //         ------ ------ ------ ------ ------ ------ ------ 
  //  y-0:   subYea subMon month  month  year   addMon addYea 

  void createMonthYearPanel() {

    if (TRACE) System.out.println( "createMonthYearPanel entered");
    subMonthButn = new JButton("<<");
    subMonthButn.setFont( regularFont );
    subMonthButn.setBorder( cp );
    subMonthButn.addActionListener( this );
    subMonthButn.setBackground( calBackground );
    subMonthButn.setAlignmentY( 0.5f );

    addMonthButn = new JButton(">>");
    addMonthButn.setFont( regularFont );
    addMonthButn.setBorder( cp );
    addMonthButn.addActionListener( this );
    addMonthButn.setBackground( calBackground );
    addMonthButn.setAlignmentY( 0.5f );

    monthList = new JComboBox( months );
    monthList.setFont( regularFont );
    monthList.addItemListener( this );
    monthList.setAlignmentY( 0.5f );

    yearText = new JTextField( new String( String.valueOf(cal.get( YEAR))));
    yearText.setBackground(calBackground);
    yearText.addActionListener( this );
    yearText.setFont( regularFont );
    int x = subMonthButn.getPreferredSize().width*3;
    int y = subMonthButn.getPreferredSize().height;
    yearText.setPreferredSize( new Dimension( x,y));
    yearText.setMaximumSize( new Dimension( x,y));
    yearText.setAlignmentY( .5f );

    subYearButn = new JButton("<<");
    subYearButn.setFont( regularFont );
    subYearButn.setBorder( cp );
    subYearButn.addActionListener( this );
    subYearButn.setBackground( calBackground );
    subYearButn.setAlignmentY( .5f );

    addYearButn = new JButton(">>");
    addYearButn.setFont( regularFont );
    addYearButn.setBorder( cp );
    addYearButn.addActionListener( this );
    addYearButn.setBackground( calBackground );
    addYearButn.setAlignmentY( .5f );

    monthYearBox = box.createHorizontalBox();


    monthYearBox.add( box.createHorizontalGlue());
    monthYearBox.add( subMonthButn );
    monthYearBox.add( addMonthButn );
    monthYearBox.add( monthList );
    monthYearBox.add( box.createHorizontalStrut(1));
    monthYearBox.add( yearText );
    monthYearBox.add( subYearButn );
    monthYearBox.add( addYearButn );
    monthYearBox.add( box.createHorizontalGlue());
  }

  //----------------------------------------------------------------
  // Reset MonthYear Panel
  public void resetMonthYearPanel( GeoDate newDate ) {
    if (TRACE) System.out.println( "resetMonthYearPanel entered");
    cal.setTime( newDate );
    computeFields( cal );
    int m = cal.get( MONTH );
    Integer yr = Integer.valueOf( cal.get( YEAR));

    monthList.setVisible(false );
    monthList.setSelectedIndex( m);
    monthList.setVisible( true);

    int yr1 = Integer.parseInt( yearText.getText().trim());
    if (yr.intValue() != yr1) {
      yearText.setText( yr.toString() );
    }
  }
  //----------------------------------------------------------------
  // Create Time Panel

  void createTimePanel () {

    if (TRACE) System.out.println( "createTimePanel entered");
    hourLabel = new JLabel("Hour(0-23)        ");
    hourLabel.setFont( boldFont );

    minLabel = new JLabel( "Minute    ");
    minLabel.setFont( boldFont );

    JLabel colonLabel = new JLabel( ":");
    Font f = new Font( "Dialog", Font.BOLD, 12);
    colonLabel.setFont( f );

    subHourButn = new JButton("<<");
    subHourButn.setFont( regularFont );
    subHourButn.setBorder( cp );
    subHourButn.addActionListener( this );
    subHourButn.setBackground( calBackground );
    subHourButn.setAlignmentY( 0.5f );

    addHourButn = new JButton(">>");
    addHourButn.setFont( regularFont );
    addHourButn.setBorder( cp );
    addHourButn.addActionListener( this );
    addHourButn.setBackground( calBackground );
    addHourButn.setAlignmentY( 0.5f );

    hourText = new JTextField(); 
    hourText.setText("     00");
    hourText.setFont( regularFont );
    hourText.setBackground(calBackground);
    hourText.addActionListener( this );
    hourText.setHorizontalAlignment( JTextField.RIGHT );

    int x = addHourButn.getPreferredSize().width * 2;
    int y = addHourButn.getPreferredSize().height;
    hourText.setPreferredSize( new Dimension( x, y) );
    hourText.setMaximumSize( hourText.getPreferredSize());
    hourText.setSize( hourText.getPreferredSize());
    hourText.setAlignmentY( .5f );


    minList = new JComboBox( minutes );
    minList.addItemListener( this );
    minList.setFont( regularFont );
    x = addHourButn.getPreferredSize().width * 2;
    y = addHourButn.getPreferredSize().height;
    minList.setPreferredSize( new Dimension( x,y ));
    minList.setMaximumSize( minList.getPreferredSize());
    minList.setSize( minList.getPreferredSize());
    minList.setAlignmentY( .5f );

    subMinButn = new JButton("<<");
    subMinButn.setFont( regularFont );
    subMinButn.setBorder( cp );
    subMinButn.addActionListener( this );
    subMinButn.setBackground( calBackground );
    subMinButn.setAlignmentY( .5f );

    addMinButn = new JButton(">>");
    addMinButn.setFont( regularFont );
    addMinButn.setBorder( cp );
    addMinButn.addActionListener( this );
    addMinButn.setBackground( calBackground );
    addMinButn.setAlignmentY( .5f );


    labelBox = box.createHorizontalBox();
    labelBox.add( box.createHorizontalGlue());
    labelBox.add( hourLabel );
    labelBox.add( box.createHorizontalStrut(1));
    labelBox.add( minLabel );
    labelBox.add( box.createHorizontalGlue());

    hrMinBox = box.createHorizontalBox();
    hrMinBox.add( box.createHorizontalGlue());
    hrMinBox.add( subHourButn );
    hrMinBox.add( addHourButn );
    hrMinBox.add( hourText );
    hrMinBox.add( box.createHorizontalStrut(5));
    //hrMinBox.add( colonLabel );
    hrMinBox.add( minList );
    hrMinBox.add( subMinButn );
    hrMinBox.add( addMinButn );
    hrMinBox.add( box.createHorizontalGlue());

    timeBox = box.createVerticalBox();
    timeBox.add( box.createVerticalGlue());
    timeBox.add( labelBox );
    timeBox.add( hrMinBox );
    timeBox.add( box.createVerticalGlue());
  }

  void createSelectionPanel() {
    if (TRACE) System.out.println( "createSelectionPanel entered");
    selectionLabel = new JLabel("ddd mmm dd hh:mm yyyy");
    selectionLabel.setBackground( Color.red );
    selectionLabel.setForeground( Color.black );
    selectionPanel = new JPanel();
    selectionPanel.add( selectionLabel );
    int x = selectionPanel.getPreferredSize().width;
    int y = 15;
    //selectionPanel.setPreferredSize( new Dimension( x,y));
    //selectionPanel.setMaximumSize( new Dimension( x,y));
    //selectionPanel.setBackground( Color.yellow );
    updateDateLabel();
  }
  //----------------------------------------------------------------
  // create the okay/cancel panel
  void createOkPanel() {

    if (TRACE) System.out.println( "createOkPanel entered");
    okButn = new JButton(    "OK"); 
    okButn.addActionListener( this );
    okButn.setBackground( calBackground );
    okButn.setMinimumSize( new Dimension( 51, 25 ));
    okButn.setMaximumSize( new Dimension( 51, 25 ));

    cancelButn = new JButton("Cancel");
    cancelButn.addActionListener( this );
    cancelButn.setBackground( calBackground );
    cancelButn.setMinimumSize( new Dimension( 73, 25 ));
    cancelButn.setMaximumSize( new Dimension( 73, 25 ));

    okPanel = new JPanel();
    okPanel.setLayout( new GridLayout(1,2));
    okPanel.add( okButn);
    okPanel.add( cancelButn);
  }  // create okPanel

  //----------------------------------------------------------------
  // handleHourChange
  void handleHourChange(){
    if (TRACE) System.out.println( "handleHourChange entered");
    try {
      cal.setTime( liquidDate );
      computeFields( cal );
      int prevHr = cal.get(HOUR_OF_DAY);   // hang on to old value
      int hr = Integer.parseInt( hourText.getText().trim());
      hr = validateHours( hr );
      hourText.setText( (Integer.valueOf(hr)).toString() );
      cal.set( HOUR_OF_DAY, hr );
      liquidDate = new GeoDate(cal.getTime());
      if (!liquidDateWithinBounds()) {  // restore old value
        cal.set( HOUR_OF_DAY, prevHr );
        liquidDate = new GeoDate(cal.getTime());
        hourText.setText( (Integer.valueOf(prevHr)).toString() );
      }
      updateDateLabel();
    } catch (NumberFormatException e) {
    }
  }
  //----------------------------------------------------------------
  // handleYearChange
  void handleYearChange(){
    if (TRACE) System.out.println( "handleYearChange entered");
    try {
      cal.setTime( liquidDate );
      computeFields( cal );
      int prevYr = cal.get(YEAR);   // hang on to old value
      int yr = Integer.parseInt( yearText.getText().trim());
      //yr = validateYear( yr );
      yearText.setText( (Integer.valueOf(yr)).toString() );
      cal.set( YEAR, yr );
      liquidDate = new GeoDate(cal.getTime());
      if (!liquidDateWithinBounds()) {  // restore old value
        //boundsMsg();
        cal.set( YEAR, prevYr );
        liquidDate = new GeoDate(cal.getTime());
        yearText.setText( (Integer.valueOf(prevYr)).toString() );
      }
      else {
        updateGUIAfterLiquidDateChange();
      }
      updateDateLabel();
    } catch (NumberFormatException e) {
    }
  }
  //----------------------------------------------------------------
  // Validate Hours
  int validateHours( int hr) {
    if (TRACE) System.out.println( "validateHours entered");
    if (hr < 0) return(0);
    if (hr > 23) return(23);
    return( hr );
  }
  //----------------------------------------------------------------
  // check Date within range
  boolean liquidDateWithinBounds( ) {

    if (TRACE) System.out.println( "liquidDateWithinBounds entered");
    /*
      cal.setTime( earliestDateAllowed );
      computeFields( cal );
      System.out.println(" Floor date is: " + 
      " year " + cal.get( YEAR) +
      " month " + cal.get( MONTH) +
      " date " + cal.get( DATE) +
      " hour " + cal.get( cal.HOUR) +
      " min " + cal.get( MINUTE));

      cal.setTime( latestDateAllowed );
      computeFields( cal );
      System.out.println(" Latest date is: " + 
      " year " + cal.get( YEAR) +
      " month " + cal.get( MONTH) +
      " date " + cal.get( DATE) +
      " hour " + cal.get( cal.HOUR) +
      " min " + cal.get( MINUTE));

      cal.setTime( liquidDate );
      computeFields( cal );
      System.out.println(" Liquid date is: " + 
      " year " + cal.get( YEAR) +
      " month " + cal.get( MONTH) +
      " date " + cal.get( DATE) +
      " hour " + cal.get( cal.HOUR) +
      " min " + cal.get( MINUTE));
    */

    if (earliestCheckingEnabled) {
      System.out.println("liquidDate is: " + liquidDate);
      System.out.println("earliestDateAllowed is: " + earliestDateAllowed);
      if (liquidDate.getTime() < earliestDateAllowed.getTime()) {
        lowerBoundMsg();
        return (false);
      }
    }
    if (latestCheckingEnabled) {
      if (liquidDate.getTime() > latestDateAllowed.getTime()) {
        upperBoundMsg();
        return( false );
      }
    }
    return( true );
  }
  //----------------------------------------------------------------
  // 
  void lowerBoundMsg() {
    if (TRACE) System.out.println( "lowerBoundMsg entered");
    JOptionPane.showMessageDialog(this, 
                                  (new String("Lower bound restricted to " + composeTimeLabel( getEarliestDateAllowed()))),
                                  "Calendar Bounds Exception",
                                  JOptionPane.WARNING_MESSAGE);
  }
  //----------------------------------------------------------------
  // 
  void upperBoundMsg() {
    if (TRACE) System.out.println( "upperBoundMsg entered");
    JOptionPane.showMessageDialog(this, 
                                  (new String("Upper bound restricted to " + composeTimeLabel( getLatestDateAllowed()))),
                                  "Calendar Bounds Exception",
                                  JOptionPane.WARNING_MESSAGE);
  }
  //----------------------------------------------------------------
  // attempt to add or subract a year
  void attemptToAddOrSubtractAYear( int numYears ) {
    if (TRACE) System.out.println( "attemptToAddOrSubtractAYear entered");
    cal.setTime( liquidDate );
    computeFields( cal );
    int yr = cal.get(YEAR) + numYears;
    cal.set( YEAR, yr );
    liquidDate = new GeoDate(cal.getTime());
    if (liquidDateWithinBounds()) {
      updateGUIAfterLiquidDateChange();
    }
    else {  // drops below floor or above ceiling; return to previous value
      cal.set( YEAR, (cal.get(YEAR) - numYears) );
      liquidDate = new GeoDate(cal.getTime());
    }
  }
  //----------------------------------------------------------------
  // update GUI components after liquid Date change
  void updateGUIAfterLiquidDateChange() {
    if (TRACE) System.out.println( "updateGUIAfterLIquidDateChange entered");
    resetMonthYearPanel( liquidDate );
    resetCalendarPanel( liquidDate );
    softwareDayOfMonthClick( liquidDate );
    updateDateLabel();
  }
  //----------------------------------------------------------------
  // item State Changed
  public void itemStateChanged( ItemEvent itemEvent) {
    if (TRACE) System.out.println( "itemStateChanged entered");

    if (itemEvent.getSource() == monthList) {

      int mon = monthList.getSelectedIndex();
      cal.setTime( liquidDate );
      computeFields( cal );
      int prevMon = cal.get(MONTH);

      if (prevMon != mon) {
        cal.set( MONTH, mon );
        liquidDate = new GeoDate(cal.getTime());
        if (!liquidDateWithinBounds()) {
          cal.set(MONTH, prevMon );
          liquidDate = new GeoDate(cal.getTime());
        }
        updateGUIAfterLiquidDateChange();
      }
    }
    else if (itemEvent.getSource() == minList) {
      int min = Integer.parseInt( (String) minList.getSelectedItem());
      cal.setTime( liquidDate );
      computeFields( cal );
      int oldMin = cal.get(MINUTE);
      cal.set(MINUTE, min  );
      liquidDate = new GeoDate(cal.getTime());
      if (!liquidDateWithinBounds()) {
        cal.set(MINUTE, oldMin);
        liquidDate = new GeoDate(cal.getTime());
        minList.setVisible(false);
        minList.setSelectedIndex( oldMin/5 );
        minList.setVisible(true);
      }
      updateDateLabel();
    }
  }
  //----------------------------------------------------------------
  // Action
  public void actionPerformed( ActionEvent event ) {
    if (TRACE) System.out.println( "actionPerformed entered");
    Object source = event.getSource();
    int mon = 0;
    int prevMon = 0;
    int yr = 0;
    int day = 0;
    int inputDayOfMonth;

    if (source == okButn) {
      // First, get the hour from the TextField in case user didn't hit 
      // return
      handleHourChange();
      if (sdf != null) {
        pcs.firePropertyChange("FormattedDateTime",
                               sdf.format( getInitialDate()), 
                               sdf.format(liquidDate));
      }
      pcs.firePropertyChange("DateTime",
                             getInitialDate(),
                             liquidDate);
      result_ = OK_RESPONSE;
      closeDown();
    }
    else if (source == cancelButn) {
      result_ = CANCEL_RESPONSE;
      closeDown();
    }
    else if (source == subYearButn) {
      attemptToAddOrSubtractAYear( -1 );
    }
    else if (source == addYearButn) {
      attemptToAddOrSubtractAYear( 1 );
    }
    else if (source == subMonthButn) {
      cal.setTime( liquidDate );
      computeFields( cal );
      mon = cal.get(MONTH) - 1;
      if (mon == -1) { // Jan --> Dec
        mon = 11;
        cal.set( MONTH, mon );
        yr = cal.get( YEAR ) - 1;
        cal.set( YEAR, yr );
        liquidDate = new GeoDate(cal.getTime());
        if (!liquidDateWithinBounds()) {
          yr = cal.get( YEAR ) + 1;
          cal.set( YEAR, yr);
          liquidDate = new GeoDate(cal.getTime());
        }
      }
      cal.set( MONTH, mon);
      liquidDate = new GeoDate(cal.getTime());
      if (!liquidDateWithinBounds()) {
        mon = mon + 1;
        if (mon == 12) mon = 0;
        cal.set( MONTH, mon );
        liquidDate = new GeoDate(cal.getTime());
      }
      updateGUIAfterLiquidDateChange();
    }
    else if (source == addMonthButn) {
      cal.setTime( liquidDate );
      computeFields( cal );
      mon = cal.get(MONTH) + 1;
      if (mon == 12) {
        mon = 0;
        cal.set( MONTH, 0 );
        yr = cal.get(YEAR) + 1;
        cal.set( YEAR, yr );
        liquidDate = new GeoDate(cal.getTime());
        if (!liquidDateWithinBounds()) {
          cal.set( YEAR, cal.get(YEAR) - 1);
          liquidDate = new GeoDate(cal.getTime());
        }
      }
      cal.set(MONTH, mon );
      liquidDate = new GeoDate(cal.getTime());
      if (!liquidDateWithinBounds()) {
        mon = mon - 1;
        if (mon == -1) mon = 11;
        cal.set( MONTH, mon );
        liquidDate = new GeoDate(cal.getTime());
      }
      updateGUIAfterLiquidDateChange();
    }
    else if (source == hourText) {
      handleHourChange();
    }
    else if (source == yearText) {
      handleYearChange();
    }
    else if (source == subHourButn) {
      try {
        // User may have changed the hour text without hitting "return"
        // so must synchronize the value in TextField with our variables
        handleHourChange();
        // Now, get the value in TextField and decrement
        int hr = Integer.parseInt( hourText.getText().trim());
        hr--;
        if (hr == -1) {
          hr = 23;
        }
        hourText.setText( (Integer.valueOf(hr)).toString() );
        handleHourChange();
      } catch (NumberFormatException e) {
      }
    }
    else if (source == addHourButn) {
      try {
        // User may have changed the hour text without hitting "return"
        // so must synchronize the value in TextField with our variables
        handleHourChange();
        // Now, get the value in TextField and decrement
        int hr = Integer.parseInt( hourText.getText().trim());
        hr++;
        if (hr == 24) {
          hr = 0;
        }
        hourText.setText( (Integer.valueOf(hr)).toString() );
        handleHourChange();
      } catch (NumberFormatException e) {
      }
    }
    else if (source == subMinButn) {
      try {
        int min = Integer.parseInt( (String) minList.getSelectedItem());
        min  = min - 5;
        if (min == -5) {
          min = 55;
        }
        cal.setTime( liquidDate );
        computeFields( cal );
        int oldMin = cal.get( MINUTE);
        cal.set( MINUTE, min );
        liquidDate = new GeoDate(cal.getTime());
        if (!liquidDateWithinBounds()) {
          cal.set( MINUTE,oldMin);
          liquidDate = new GeoDate(cal.getTime());
          min = oldMin;
        }
        minList.setVisible(false);
        minList.setSelectedIndex( min/5 );
        minList.setVisible(true);
        updateDateLabel();
      } catch (NumberFormatException e) {
      }
    }
    else if (source == addMinButn) {
      try {
        int min = Integer.parseInt( (String) minList.getSelectedItem());
        min  = min + 5;
        if (min == 60) {
          min = 0;
        }
        cal.setTime( liquidDate );
        computeFields( cal );
        int oldMin = cal.get(MINUTE);
        cal.set(MINUTE, min );
        liquidDate = new GeoDate(cal.getTime());
        if (!liquidDateWithinBounds()) {
          cal.set( MINUTE, oldMin);
          liquidDate = new GeoDate(cal.getTime());
          min = oldMin;
        }
        minList.setVisible(false);
        minList.setSelectedIndex( min/5 );
        minList.setVisible(true);
        updateDateLabel();
      } catch (NumberFormatException e) {
      }
    }
    else if (source == monthList) {
    }
    else { // must be from the calendar
      try {
        inputDayOfMonth = Integer.parseInt( event.getActionCommand().trim());
        cal.setTime( liquidDate );
        computeFields( cal );
        int lastDate = cal.get(DATE);
        cal.set(DATE, inputDayOfMonth );
        liquidDate = new GeoDate(cal.getTime());
        if (liquidDateWithinBounds()) {
          selectDay( inputDayOfMonth);
          updateDateLabel();
        }
        else {
          cal.set(DATE, lastDate );
          liquidDate = new GeoDate(cal.getTime());
        }
      } catch (NumberFormatException e) {
        System.out.println(" !!! Trouble with " + event.getActionCommand());
      }
    }
  }

  public GeoDate getDate() {
    return liquidDate;
  }

  public void propertyChange( java.beans.PropertyChangeEvent event) {
    if (TRACE) System.out.println( "propertyChange entered");
    Object obj = event.getSource();
    this.setTitle( composeTimeLabel( liquidDate));
    if (event.getPropertyName().equals("DateTime")) {
      /*
        System.out.println(" Property Change, old value: " + 
        composeTimeLabel( (Date) event.getOldValue()) + " new value: " +
        composeTimeLabel( (Date) event.getNewValue()));
      */
    }
    else if (event.getPropertyName().equals("FormattedDateTime")) {
    }
  }
  public void addPropertyChangeListener( PropertyChangeListener l ) {
    pcs.addPropertyChangeListener( l );
  }
  public void removePropertyChangeListener( PropertyChangeListener l ) {
    pcs.removePropertyChangeListener( l );
  }



  // ----------------------------------------------------------------
  //
  public static void main(String args[]) {

    GeoDateDialog dtg = new GeoDateDialog();
    dtg.setOutputDateFormatter( new SimpleDateFormat(" dd MMM yyyy HH:mm "));

    /*
      JFrame jf = new JFrame();
      jf.setVisible( true );
      jf.setSize(220 ,323);
      jf.getContentPane().add( dtg );
      jf.invalidate();
      jf.validate();
      jf.addWindowListener( new WindowAdapter() {
      public void windowClosing( WindowEvent e) {
      Window w = e.getWindow();
      w.setVisible( false );
      w.dispose();
      System.exit( 0 );
      }
      });
    */

    int result = dtg.showDialog(new GeoDate(), 200, 200);

    System.out.println("Result = " + result);
    System.out.println("Date = " + dtg.getGeoDate().toString());

    dtg.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( java.beans.PropertyChangeEvent event) {
          if (event.getPropertyName().equals("FormattedDateTime")) {
            System.out.println(" MAIN Property Change, old value: " + 
                               (String) event.getOldValue() + " new value: " +
                               (String) event.getNewValue());
          }
        }
      });
  }
}
