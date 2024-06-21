package kart;

import java.io.File;
import java.io.IOException;
import java.util.*;

import fu.util.StringUtil;
import fu.util.DBUtil;
import fu.keys.LSIClassCentreDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import pp.dorenda.client2.MapWidget;
import pp.dorenda.client2.access.MapAccess;
import pp.dorenda.client2.event.MapClickEvent;
import pp.dorenda.client2.event.MapClickListener;
import pp.dorenda.client2.event.MapMoveEvent;
import pp.dorenda.client2.event.MapMoveListener;
import pp.dorenda.client2.event.MapDragEvent;
import pp.dorenda.client2.event.MapDragListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javax.swing.*;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.ComponentListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import java.awt.event.ComponentEvent;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

// import ResultsOverlay;


/**
 * Test activity to presents the capabilities of the MapWidget. This class is only a sample and not intended to be part of own developments. 
 * @author Joerg.Roth@Wireless-earth.org
 */
public class Hotel extends JFrame implements ActionListener,WindowListener,MapMoveListener,MapClickListener,MapDragListener,ComponentListener {


    public static int INITIAL_SIZE_X=1200;
    public static int INITIAL_SIZE_Y=950;



    private GridBagConstraints c = new GridBagConstraints();
    private GridBagLayout gridbag=new GridBagLayout();

    private static String nearObjectToken;
    private static double nearObjectDistance;
    private static int nearObjectfrom;
    private static int nearObjectto;
    private static int farObjectfrom;
    private static int farObjectto;

    private static String farObjectToken;
    private static double farObjectDistance;

    private JButton setposButton;
    private JTextField posField;

    private JTextField cityField;
    private JTextField nameField;

    private JButton zoomInButton; 
    private JButton zoomOutButton;


    private JComboBox<String> typeCombo;
    private JComboBox<String> hTypeCombo;

    private JButton screenshotButton;
    private JButton addNearObjButton;
    private JButton addFarObjButton;

    private static String mapAccessString=null;
    private static String cmdlineStartPos=null;
    private static String cmdlineScreenShot=null;
    private static String cmdlineSx=null;
    private static String cmdlineSy=null;


    private static Hotel mapFrame=null; 

    private int screenShotFileNr=0;              // Laufende Nummer fuer Screenshots

    private int startSelectionLatE6=0;           // Fuer Selektion Rechteck
    private int startSelectionLonE6=0;


    private JButton clearSelectionButton;
    private JTextField selectionFromLat;
    private JTextField selectionFromLon;
    private JTextField selectionToLat;
    private JTextField selectionToLon;
    private double fromLat, fromLon, toLat, toLon;

    private JComboBox<String> queryCombo;
    private JButton queryButton;
    private JButton cancelButton;



    private MapWidget mapWidget=null;                // Karten-Widget
    private SelectionOverlay selectionOverlay=null;  // Overlay, um die Rechteck-Selektion anzuzeigen
    // private ResultOverlay resultOverlay=null;        // Overlay, um das Resultat anzuzeigen
    private ResultsOverlay resultsOverlay = null;


    private Connection connection=null;  // DB-Connection
    private PreparedStatement pstatement=null; // Laufendes SQL-Statement

    private HashMap<String, String> nearObjects = new HashMap<>();

    private JFrame addNearWin = new JFrame();
    private JTextField obj = new JTextField();
    private JTextField distance = new JTextField();
    private JLabel objText = new JLabel("<html><br>Objekt Typ eingeben");
    private JLabel distanceText_near = new JLabel("<html>maximale Entfernung in Meter eingeben");
    private JButton addNearBtn = new JButton("Hinzufügen");
    private JPanel addNearPanel = new JPanel();
    private JComboBox tokensCombo_near;
    private JComboBox tokensCombo_far;
    private JLabel nearObjectTokenLabel;
    private JLabel nearObjectDistanceLabel;
    private JButton removeNearObjectBtn = new JButton("Entfernen");

    private JFrame addFarWin = new JFrame();
    private JTextField farDistance = new JTextField();
    private JButton addFarBtn = new JButton("Hinzufügen");
    private JPanel addFarPanel = new JPanel();
    private JLabel farObjectTokenLabel;
    private JLabel farObjectDistanceLabel;
    private JButton removeFarObjectBtn = new JButton("Entfernen");
    private JLabel distanceText_far = new JLabel("<html>mindeste Entfernung in Meter eingeben");

    private JFrame resWin = new JFrame();
    private JPanel resPanel = new JPanel();
    private JList resList = new JList();

    private JCheckBox wheelchaircheckbox = new JCheckBox();
    private JLabel wheelchair_text = new JLabel("Rollstuhlgerecht");
    private JCheckBox nosmokincheckbox = new JCheckBox();
    private JLabel nosmoking_text = new JLabel("Nichtraucher");
    private JSlider stars = new JSlider(0, 5);
    private JLabel stars_text = new JLabel("Anzahl Sterne (0 heißt alle)");



    public static void main(String[] args) {

        int optNr=0;
        String dbAccessStr=null;

        if (args.length==0) {
            System.out.println("missing arguments, -? for help");
            System.exit(1);
        }

        if (args.length>0) {
            dbAccessStr=args[0];
            mapAccessString=args[1];
        }


        /*if (optNr!=0) {
            System.out.println("Last option without argument");
            System.exit(1);
        }*/
        if (cmdlineSx!=null) {
            try {
                INITIAL_SIZE_X=Integer.parseInt(cmdlineSx);
            }
            catch (Exception e) { 
                System.out.println("Value "+cmdlineSx+" for -sx is not a number");
                System.exit(1);
            }
        }
        if (cmdlineSy!=null) {
            try {
                INITIAL_SIZE_Y=Integer.parseInt(cmdlineSy);
            }
            catch (Exception e) { 
                System.out.println("Value "+cmdlineSy+" for -sy is not a number");
                System.exit(1);
            }
        }
        if (dbAccessStr==null)  {
            System.out.println("No DB access string passed");
            System.exit(1);
        }


        mapFrame=new Hotel(dbAccessStr);

    }

    public Hotel(String dbAccessStr) {

        try {
            System.out.println("Establish access to DB "+dbAccessStr);
            DBUtil.parseDBparams(dbAccessStr,0);
            connection=DBUtil.getConnection(0);
            LSIClassCentreDB.initFromDB(connection);
            System.out.println("DB connection established "+dbAccessStr);
        }
        catch (Exception e) {
            System.out.println("Error initialising 'in' DB access: "+e.toString());
            System.exit(1);
        }


        try {
            MapAccess mapAccess=null;

            MapWidget.configure(mapAccessString);

            mapWidget=new MapWidget();

            selectionOverlay=new SelectionOverlay(mapWidget);
            selectionOverlay.setShowSelectionMeters(true);
            mapWidget.addOverlay(selectionOverlay);

            // resultOverlay=new ResultOverlay(mapWidget);
            resultsOverlay = new ResultsOverlay(mapWidget);
            // mapWidget.addOverlay(resultOverlay);
            mapWidget.addOverlay(resultsOverlay);


            mapWidget.addMapMoveListener(this);
            mapWidget.addMapClickListener(this);
            mapWidget.addMapDragListener(this);

            if (cmdlineStartPos!=null) {
                int[] newpos=posStringToInts(cmdlineStartPos);
                if (newpos==null) {
                    System.out.println("Illegal -p parameter: "+cmdlineStartPos);
                }
                mapWidget.setCenterPosition(newpos[0],newpos[1],newpos[2],newpos[3]); 
            }
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
        initUI();

        if (cmdlineScreenShot!=null) {   // Es soll ein Screenshot gemacht werden

            try {
                Thread.sleep(10000);
                BufferedImage image=new BufferedImage(mapWidget.getSize().width,mapWidget.getSize().height,BufferedImage.TYPE_INT_ARGB);
                System.out.println("Create Image "+mapWidget.getSize().width+"*"+mapWidget.getSize().height);
                Graphics g=image.getGraphics();
                mapWidget.paintComponent(g);
                ImageIO.write(image, "PNG", new File(cmdlineScreenShot));  // Als die Karte als PNG speichern
                System.exit(0);
            }
            catch (Exception e) {
                System.out.println("Error writing screenshot: "+e);
            }
        }
    }



    private void initUI() {
        // GridBagLayout gridbag=new GridBagLayout();
        // GridBagConstraints c=new GridBagConstraints();

        setLayout(gridbag);  

        c.fill=GridBagConstraints.HORIZONTAL;


        zoomInButton=new JButton("Zoom in");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(zoomInButton,c);
        add(zoomInButton);
        zoomInButton.addActionListener(this);

        zoomOutButton=new JButton("Zoom out");
        c.gridx=1;
        c.gridy=0;
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(zoomOutButton,c);
        add(zoomOutButton);
        zoomOutButton.addActionListener(this);

        posField=new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=3;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(posField,c);
        add(posField);

        setposButton=new JButton("Set Position");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=4;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(setposButton,c);
        add(setposButton);
        setposButton.addActionListener(this);

        typeCombo=new JComboBox<String>(mapTypeStrings());
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=5;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(typeCombo,c);
        add(typeCombo);
        typeCombo.addActionListener(this);

        screenshotButton=new JButton("Screenshot");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=13;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(screenshotButton,c);
        add(screenshotButton);
        screenshotButton.addActionListener(this);

        JLabel cityHelptext=new JLabel("<html><br>Ortsname eingeben");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=15;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(cityHelptext,c);
        add(cityHelptext);

        cityField = new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=16;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(cityField,c);
        add(cityField);

        JLabel typeHelptext=new JLabel("<html><br>Art des Hotels eingeben");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=18;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(typeHelptext,c);
        add(typeHelptext);


        hTypeCombo=new JComboBox<String>(new String[]{"Alle","Hotel","Hostel","Pension","Ferienhaus","Alphenhütte","Motel","Wohnheim"});
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=19;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(hTypeCombo,c);
        add(hTypeCombo);
        hTypeCombo.addActionListener(this);

        JLabel nameHelptext=new JLabel("<html><br>Name des Hotels eingeben");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=21;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(nameHelptext,c);
        add(nameHelptext);

        nameField = new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=22;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(nameField,c);
        add(nameField);

        // ----------- Rechteck-Selektion -----------

        JLabel selectionHelptext=new JLabel("<html><br>Enter selection or drag<br>with shift");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=24;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(selectionHelptext,c);
        add(selectionHelptext);

        clearSelectionButton=new JButton("Clear Selection");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=25;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(clearSelectionButton,c);
        add(clearSelectionButton);
        clearSelectionButton.addActionListener(this);

        selectionFromLat=new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=26;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(selectionFromLat,c);
        add(selectionFromLat);

        selectionFromLon=new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=1;
        c.gridy=26;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(selectionFromLon,c);
        add(selectionFromLon);

        selectionToLat=new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=27;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(selectionToLat,c);
        add(selectionToLat);

        selectionToLon=new JTextField();
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=1;
        c.gridy=27;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(selectionToLon,c);
        add(selectionToLon);

 

        JLabel nearHelptext=new JLabel("<html><br>Nähe hinzufügen");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=29;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(nearHelptext,c);
        add(nearHelptext);

        addNearObjButton=new JButton("Nähe hinzufügen");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=30;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(addNearObjButton,c);
        add(addNearObjButton);
        addNearObjButton.addActionListener(this);

        JLabel farHelptext = new JLabel("<html><br>Ferne hinzufügen");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=35;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(farHelptext,c);
        add(farHelptext);

        addFarObjButton = new JButton("Entfernung hinzufügen");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=36;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(addFarObjButton,c);
        add(addFarObjButton);
        addFarObjButton.addActionListener(this);


        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=41;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(wheelchaircheckbox,c);
        add(wheelchaircheckbox);

        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=1;
        c.gridy=41;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(wheelchair_text,c);
        add(wheelchair_text);


        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=42;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(nosmokincheckbox,c);
        add(nosmokincheckbox);

        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=1;
        c.gridy=42;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(nosmoking_text,c);
        add(nosmoking_text);

        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=43;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(stars_text,c);
        add(stars_text);

        stars.setMajorTickSpacing(1);
        stars.setPaintLabels(true);
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=44;
        c.gridwidth=1;
        c.gridheight=1;
        gridbag.setConstraints(stars,c);
        add(stars);



        // ---------- add near window ---------------
        addNearPanel.add(objText);
        setTokensCombo();
        addNearPanel.add(tokensCombo_near);
        addNearPanel.add(distanceText_near);
        addNearPanel.add(distance);
        addNearPanel.add(addNearBtn);
        addNearBtn.addActionListener(this);
        addNearPanel.setLayout(new BoxLayout(addNearPanel, BoxLayout.Y_AXIS));
        addNearWin.getContentPane().add(addNearPanel);
        addNearWin.setSize(400, 200);

        // ---------- add far window ----------------
        addFarPanel.add(objText);
        // setTokensCombo();
        addFarPanel.add(tokensCombo_far);
        addFarPanel.add(distanceText_far);
        addFarPanel.add(farDistance);
        addFarPanel.add(addFarBtn);
        addFarBtn.addActionListener(this);
        addFarPanel.setLayout(new BoxLayout(addFarPanel, BoxLayout.Y_AXIS));
        addFarWin.getContentPane().add(addFarPanel);
        addFarWin.setSize(400, 200);


        // ----------- create results window ------
        resWin.setTitle("Ergebnisse");
        resPanel.add(resList);
        resWin.setSize(350, 200);
        resWin.add(resPanel);



        queryButton=new JButton("Hotels suchen");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=49;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(queryButton,c);
        add(queryButton);
        queryButton.addActionListener(this);

        
        cancelButton=new JButton("Suche abbrechen");
        c.weightx=0.0;
        c.weighty=0.0;
        c.gridx=0;
        c.gridy=50;
        c.gridwidth=3;
        c.gridheight=1;
        gridbag.setConstraints(cancelButton,c);
        add(cancelButton);
        cancelButton.addActionListener(this);
        cancelButton.setEnabled(false);

        
// ----------- Map-Widget -----------

        c.fill=GridBagConstraints.BOTH;
        c.weightx=1.0;
        c.weighty=1.0;
        c.gridx=3;
        c.gridy=0;
        c.gridwidth=1;
        c.gridheight=50;
        gridbag.setConstraints(mapWidget,c);
        add(mapWidget);

        setSize(INITIAL_SIZE_X,INITIAL_SIZE_Y);
        setVisible(true);
        addWindowListener(this);
        addComponentListener(this);
        doLayout();

        zoomInButton.setEnabled(mapWidget.canZoomIn());
        zoomOutButton.setEnabled(mapWidget.canZoomOut());
    }


   private String[] mapTypeStrings() {
       int[] types=mapWidget.getTypes();
       String[] result=new String[types.length];
       for (int i=0;i<types.length;i++) {
           result[i]=MapWidget.typeString(types[i]);
       }
       return result;
   }


   public int[] posStringToInts(String str) {
       String[] split=str.split("/|,");
       if (split.length==2) {
           str=str+"/"+mapWidget.getZoomIndex()+"/"+Math.round(mapWidget.getAngle());
           split=str.split("/|,");
       }

       if (split.length!=4)
           return null;

       int[] newpos=new int[4];
       for (int i=0;i<4;i++) {
           try {
               if (i<=1) {
                   double value=Double.parseDouble(split[i]);
                   newpos[i]=(int)Math.round(value*1e6);
               }
               else {
                   newpos[i]=Integer.parseInt(split[i]);
               }
           }
           catch (NumberFormatException exc) {
               return null;
           }
       }
       if (newpos[2]<0 || newpos[2]>=mapWidget.getZoomSteps())
           return null;
       return newpos;
   }


@Override
   public void actionPerformed(ActionEvent e) {
        if (e.getSource()==zoomOutButton) {
            mapWidget.zoomOut();
            zoomInButton.setEnabled(mapWidget.canZoomIn());
            zoomOutButton.setEnabled(mapWidget.canZoomOut());
        }
        else if (e.getSource()==zoomInButton) {
            mapWidget.zoomIn();
            zoomInButton.setEnabled(mapWidget.canZoomIn());
            zoomOutButton.setEnabled(mapWidget.canZoomOut());
        }
        else if (e.getSource()==setposButton) {
            int[] newpos=posStringToInts(posField.getText());

            if (newpos==null)
                return;
            mapWidget.setCenterPosition(newpos[0],newpos[1],newpos[2],newpos[3]);
            zoomInButton.setEnabled(mapWidget.canZoomIn());
            zoomOutButton.setEnabled(mapWidget.canZoomOut()); 
        }
        else if (e.getSource()==screenshotButton) {
            doScreenShot();
        }
        else if (e.getSource()==typeCombo) 
            mapWidget.setTypeIndex(typeCombo.getSelectedIndex());
        else if (e.getSource()==clearSelectionButton) {
            selectionOverlay.unsetCondition();
            selectionFromLat.setText("");
            selectionFromLon.setText("");
            selectionToLat.setText("");
            selectionToLon.setText("");
            mapWidget.repaint();
        }
        else if (e.getSource()==queryButton) {

            try {

               queryButton.setEnabled(false);
               cancelButton.setEnabled(true);
               new Thread(()->searchHotels()).start();


            }
            catch (NumberFormatException exc) {
                JOptionPane.showMessageDialog(null, "Selection contains invalid numbers","Error",JOptionPane.ERROR_MESSAGE);
                return;
            } 
        }
        else if (e.getSource()==cancelButton) {
            if (pstatement!=null) {
                try {
                    pstatement.cancel();
                }
                catch (Exception exc) {System.out.println(exc);}
            }
        }
        else if (e.getSource()==addNearObjButton) {
            addNearWin.setVisible(true);
        }
        else if (e.getSource()==addNearBtn) {
            try {
                remove(nearObjectTokenLabel);
                remove(nearObjectDistanceLabel);
                remove(removeNearObjectBtn);
            } catch(Exception exc) 
            {
                System.out.println(exc);
            }
            System.out.println(tokensCombo_near.getSelectedIndex() + ": " + tokensCombo_near.getSelectedItem().toString());
            nearObjectToken = tokensCombo_near.getSelectedItem().toString();
            nearObjectDistance = Double.parseDouble(distance.getText());
            int[] lc = getLsiClassRangeByToken(nearObjectToken);
            nearObjectfrom = lc[0];
            nearObjectto = lc[1];
            nearObjectTokenLabel = new JLabel(nearObjectToken);
            nearObjectDistanceLabel = new JLabel(String.valueOf(nearObjectDistance));
            c.weightx=0.0;
            c.weighty=0.0;
            c.gridx=0;
            c.gridy=31;
            c.gridwidth=3;
            c.gridheight=1;
            gridbag.setConstraints(nearObjectTokenLabel,c);
            add(nearObjectTokenLabel);
            c.weightx=0.0;
            c.weighty=0.0;
            c.gridx=0;
            c.gridy=32;
            c.gridwidth=3;
            c.gridheight=1;
            gridbag.setConstraints(nearObjectDistanceLabel,c);
            add(nearObjectDistanceLabel);
            c.weightx=0.0;
            c.weighty=0.0;
            c.gridx=0;
            c.gridy=33;
            c.gridwidth=3;
            c.gridheight=1;
            gridbag.setConstraints(removeNearObjectBtn,c);
            add(removeNearObjectBtn);
            removeNearObjectBtn.addActionListener(this);
            addNearWin.setVisible(false);
            revalidate();
            repaint();
        }
        else if (e.getSource()==removeNearObjectBtn) {
            nearObjectToken = "";
            nearObjectDistance = 0;
            remove(nearObjectTokenLabel);
            remove(nearObjectDistanceLabel);
            remove(removeNearObjectBtn);
            revalidate();
            repaint();
        }
        else if (e.getSource()==addFarObjButton) {
            addFarWin.setVisible(true);
        }
        else if (e.getSource()==addFarBtn) {
            try {
                remove(farObjectTokenLabel);
                remove(farObjectDistanceLabel);
                remove(removeFarObjectBtn);
            } catch(Exception exc) 
            {
                System.out.println(exc);
            }
            farObjectToken = tokensCombo_far.getSelectedItem().toString();
            farObjectDistance = Double.parseDouble(farDistance.getText());
            int[] lc = getLsiClassRangeByToken(farObjectToken);
            farObjectfrom = lc[0];
            farObjectto = lc[1];
            farObjectTokenLabel = new JLabel(farObjectToken);
            farObjectDistanceLabel = new JLabel(String.valueOf(farObjectDistance));
            c.weightx=0.0;
            c.weighty=0.0;
            c.gridx=0;
            c.gridy=37;
            c.gridwidth=3;
            c.gridheight=1;
            gridbag.setConstraints(farObjectTokenLabel,c);
            add(farObjectTokenLabel);
            c.weightx=0.0;
            c.weighty=0.0;
            c.gridx=0;
            c.gridy=38;
            c.gridwidth=3;
            c.gridheight=1;
            gridbag.setConstraints(farObjectDistanceLabel,c);
            add(farObjectDistanceLabel);
            c.weightx=0.0;
            c.weighty=0.0;
            c.gridx=0;
            c.gridy=39;
            c.gridwidth=3;
            c.gridheight=1;
            gridbag.setConstraints(removeFarObjectBtn,c);
            add(removeFarObjectBtn);
            removeFarObjectBtn.addActionListener(this);
            addFarWin.setVisible(false);
            revalidate();
            repaint();
        }
        else if (e.getSource() == removeFarObjectBtn) {
            farObjectToken = "";
            farObjectDistance = 0;
            remove(farObjectTokenLabel);
            remove(farObjectDistanceLabel);
            remove(removeFarObjectBtn);
            revalidate();
            repaint();
        }
    }

    private void setTokensCombo() {
            String sql = "select token from lsiclasses;";
            PreparedStatement pStatement = null;
            Vector<String> tokens = new Vector<>();
            try {
                pStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            try {
                ResultSet r = pStatement.executeQuery();

                while (r.next()) {
                    tokens.add(r.getString(1));
                }
                tokensCombo_near = new JComboBox<String>(tokens);
                tokensCombo_far = new JComboBox<String>(tokens);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
    }

    private int[] getLsiClassRangeByToken(String token) {
        String sql = "select id, id_to from lsiclasses where token = '" + token +"'";
        int[] res = new int[2];
        PreparedStatement pStatement = null;
        try {
            pStatement = connection.prepareStatement(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        try {
            ResultSet r = pStatement.executeQuery();
            while(r.next()) {
                res[0] = (int)r.getLong(1);
                res[1] = (int)r.getLong(2);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return res;
    }


    private void searchHotels()
    {
        try {
            if (!selectionFromLat.getText().isEmpty()) {
                fromLat=Double.parseDouble(selectionFromLat.getText().trim());
                fromLon=Double.parseDouble(selectionFromLon.getText().trim());
                toLat=Double.parseDouble(selectionToLat.getText().trim());
                toLon=Double.parseDouble(selectionToLon.getText().trim());
            }
            int kind = hTypeCombo.getSelectedIndex();
            String sql;
            int kindfrom, kindto;
            String ort, hotelname;
            switch (kind) {
                case 0:
                    kindfrom = 20516100;
                    kindto = 20516199;
                    break;
                case 1:
                    kindfrom = 20516110;
                    kindto = 20516119;
                    break;
                case 2:
                    kindfrom = 20516120;
                    kindto = 20516129;
                    break;
                case 3:
                    kindfrom = 20516130;
                    kindto = 20516139;
                    break;
                case 4:
                    kindfrom = 20516140;
                    kindto = 20516149;
                    break;
                case 5:
                    kindfrom = 20516150;
                    kindto = 20516159;
                    break;
                case 6:
                    kindfrom = 20516160;
                    kindto = 20516169;
                    break;
                case 7:
                    kindfrom = 20516170;
                    kindto = 20516179;
                    break;
                default:
                    throw new RuntimeException("kein erlaubter Hoteltyp "+kind);
            }
            ort = cityField.getText();
            hotelname = nameField.getText();

            sql = "select distinct d.realname, d.lsiclass1, ST_AsEWKB(d.geom :: geometry) from domain d ";

            if (!ort.isEmpty())
                sql += "join domain orte on st_within(d.geom::geometry, orte.geom::geometry) "+
                        "and orte.lsiclass1 >= 13000000 and orte.lsiclass1 <= 13999999 "+
                        "and LOWER(orte.realname) = LOWER(?) ";

            if (nearObjectToken != null && !nearObjectToken.isEmpty())
                sql += "JOIN domain near ON ST_DWithin(d.geom, near.geom, ?)" +
                        "AND near.lsiclass1 >= ? AND near.lsiclass1 <= ? ";

            if (farObjectToken != null && !farObjectToken.isEmpty())
                sql += "LEFT JOIN domain far ON ST_DWithin(d.geom, far.geom, ?) " +
                        "AND far.lsiclass1 >= ? AND far.lsiclass1 <= ? ";

            sql +=  "where d.lsiclass1 >= ? and d.lsiclass1 <= ? ";

            if (!hotelname.isEmpty())
                sql += "AND (d.realname ILIKE ? OR d.tags_name ILIKE ?) ";

            if(fromLon != 0.0)
                sql += "and ST_within(d.geom :: geometry, ST_MakeEnvelope(?,?,?,?,4326)) ";

            if (wheelchaircheckbox.isSelected())
                sql += "and (d.tags like '%wheelchair=yes%' or d.tags like '%wheelchair=limited%') ";

            if (nosmokincheckbox.isSelected())
                sql += "and (d.tags like '%smoking=no%' or d.tags like '%smoking=limited%') ";

            int s = stars.getValue();
            if (s != 0)
                sql += "and d.tags like ? ";
            if (farObjectToken != null && !farObjectToken.isEmpty()) sql += "and far.geom IS NULL ";

            pstatement=connection.prepareStatement(sql);
            int col=1;
            if (!ort.isEmpty()) pstatement.setString(col++, ort);
            if (nearObjectToken != null && !nearObjectToken.isEmpty()) {
                pstatement.setDouble(col++, nearObjectDistance);
                pstatement.setDouble(col++, nearObjectfrom);
                pstatement.setDouble(col++, nearObjectto);
            }
            if (farObjectToken != null && !farObjectToken.isEmpty()) {
                pstatement.setDouble(col++, farObjectDistance);
                pstatement.setDouble(col++, farObjectfrom);
                pstatement.setDouble(col++, farObjectto);
            }
            pstatement.setDouble(col++, kindfrom);
            pstatement.setDouble(col++, kindto);
            if (!hotelname.isEmpty()) {
                pstatement.setString(col++, "%"+hotelname+"%");
                pstatement.setString(col++, "%name="+hotelname+"%");
            }
            if (fromLon != 0.0) {
                pstatement.setDouble(col++,fromLon);
                pstatement.setDouble(col++,fromLat);
                pstatement.setDouble(col++,toLon);
                pstatement.setDouble(col++,toLat);
            }
            if (s!=0) pstatement.setString(col++, "%stars=" + s + "%");
            ResultSet resultSet=pstatement.executeQuery();
            String realname=null;
            int lsiclass=-1;
            Geometry geom=null;

            // resultOverlay.setResult(null);
            resultsOverlay.resetResult();
            DefaultListModel<String> resListModel = new DefaultListModel<>();
            while (resultSet.next()) {
                try {
                    col=1;
                    realname=resultSet.getString(col++);
                    resListModel.addElement(realname);
                    lsiclass=(int)resultSet.getLong(col++);
                    byte[] geomdata=resultSet.getBytes(col++);
                    geom=new WKBReader().read(geomdata);
                    String lsiClassName=LSIClassCentreDB.className(lsiclass);
                    String text=realname+" ("+lsiClassName+")";
                    resultsOverlay.addResult(text, geom);
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
            resultSet.close();
            pstatement=null;
            if (resListModel.isEmpty()) resListModel.addElement("Es wurden keine Hotels gefunden");
            resList = new JList(resListModel);
            JScrollPane scrollPane = new JScrollPane(resList);
            resPanel.removeAll();
            resPanel.add(scrollPane);
            resWin.revalidate();
            resWin.repaint();
            resWin.setVisible(true);
            mapWidget.repaint();
        } catch (Exception e) {

             SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(null, "Exception executing query: "+e.toString(),"Error",JOptionPane.ERROR_MESSAGE));
        }
        fromLat = 0.0;
        fromLon = 0.0;
        toLat = 0.0;
        toLon = 0.0;
        SwingUtilities.invokeLater(()->queryButton.setEnabled(true));
        SwingUtilities.invokeLater(()->cancelButton.setEnabled(false));
    }



    private int[] getPosFromText(JTextField posField) {

            String[] split=posField.getText().split("/|,");

            if (split.length!=2)
                return null;
            int[] newpos=new int[2];
            for (int i=0;i<2;i++) {
                try {
                    newpos[i]=(int)Math.round(Double.parseDouble(split[i])*1e6);
                }
                catch (NumberFormatException exc) {
                   return null;
                }
            }
            return newpos;
    }


@Override
    public void mapMoved(MapMoveEvent event) {
         String state=StringUtil.double2String(event.getCenterLatitudeE6()/1e6,"0.000000")+"/"+
                      StringUtil.double2String(event.getCenterLongitudeE6()/1e6,"0.000000")+"/"+
                      event.getZoomIndex()+"/"+
                      Math.round(event.getAngle());
         posField.setText(state);
    }


@Override
    public void mapClicked(MapClickEvent event) {
    }


@Override
    public void windowDeactivated(WindowEvent e) {
    }


@Override
    public void windowActivated(WindowEvent e) {
    }


@Override
    public void windowIconified(WindowEvent e) {
    }


@Override
    public void windowDeiconified(WindowEvent e) {
    }


@Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }


@Override
    public void windowOpened(WindowEvent e) {
    }


@Override
    public void windowClosed(WindowEvent e) {
    }


@Override
    public void componentResized(ComponentEvent e) {
    }


@Override
    public void componentMoved(ComponentEvent e) {
    }


@Override
    public void componentShown(ComponentEvent e) {
    }


@Override
    public void componentHidden(ComponentEvent e) {
    }


@Override
    public void mapDragStarted(MapDragEvent event) {
        int key=event.getKeys();
        if ((key & MapDragEvent.KEYBOARD_SHIFT)>0) {
            startSelectionLatE6=event.getLatitudeE6();
            startSelectionLonE6=event.getLongitudeE6();
        }
    }

@Override
    public void mapDragStopped(MapDragEvent event) {
        selectionOverlay.setRectCondition(startSelectionLatE6,startSelectionLonE6,event.getLatitudeE6(),event.getLongitudeE6());
        mapWidget.repaint();
    }

@Override
    public void mapDragging(MapDragEvent event) {
        selectionOverlay.selectRect(startSelectionLatE6,startSelectionLonE6,event.getLatitudeE6(),event.getLongitudeE6());
        mapWidget.repaint();

        double fromLat=Math.min(startSelectionLatE6,event.getLatitudeE6())/1e6;
        double fromLon=Math.min(startSelectionLonE6,event.getLongitudeE6())/1e6;
        double toLat=Math.max(startSelectionLatE6,event.getLatitudeE6())/1e6;
        double toLon=Math.max(startSelectionLonE6,event.getLongitudeE6())/1e6;

        selectionFromLat.setText(StringUtil.double2String(fromLat,"0.000000"));
        selectionFromLon.setText(StringUtil.double2String(fromLon,"0.000000"));
        selectionToLat.setText(StringUtil.double2String(toLat,"0.000000"));
        selectionToLon.setText(StringUtil.double2String(toLon,"0.000000"));
    }



    private void doScreenShot() {
        try {
           screenShotFileNr++;
           String fileNrStr=""+screenShotFileNr;
           while (fileNrStr.length()<4)
               fileNrStr="0"+fileNrStr;
           String fileName="SCREENSHOT"+fileNrStr+".png";

           BufferedImage image=new BufferedImage(mapWidget.getSize().width,mapWidget.getSize().height,BufferedImage.TYPE_INT_ARGB);
           System.out.println("Make Screenshot '"+fileName+"' with "+mapWidget.getSize().width+"*"+mapWidget.getSize().height);
           Graphics g=image.getGraphics();
           mapWidget.paintComponent(g);
           ImageIO.write(image, "PNG", new File(fileName));  // Als die Karte als PNG speichern
           System.out.println("Screenshot written");
        }
        catch (IOException e) {
           System.out.println("Cannot write screenshot: "+e);
        }
    }

}