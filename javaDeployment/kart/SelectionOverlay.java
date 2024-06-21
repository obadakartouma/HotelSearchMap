package kart;

import fu.geo.Spherical;

import pp.dorenda.client2.MapWidget;
import pp.dorenda.client2.overlay.MapOverlay;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.Point;


import com.vividsolutions.jts.geom.Coordinate;


/**
* Displays the User's geometric selection 
* @author Joerg.Roth@Wireless-earth.org
*/
public class SelectionOverlay extends MapOverlay {


    private final static Color SELECTION_BORDER_COLOR=new Color(0,0,0,255);
    private final static Color SELECTION_COLOR=new Color(255,0,0,100);
    private final static Color SELECTION_ARROW_COLOR=new Color(0,0,50,255);
    private final static Color SELECTION_BOX_COLOR=new Color(255,255,200,255);

    private final static Color SET_SELECTION_BORDER_COLOR=new Color(0,0,0,150);
    private final static Color SET_SELECTION_COLOR=new Color(0,0,255,10);
    private final static Color SET_SELECTION_ARROW_COLOR=new Color(0,0,0,100);
    private final static Color SET_SELECTION_BOX_COLOR=new Color(255,255,255,150);


    private static final BasicStroke SELECTION_STROKE = new BasicStroke(2.0f);


    private static final double arrowAngle=0.4d;
    private static final double sina=Math.sin(arrowAngle);
    private static final double cosa=Math.cos(arrowAngle);


    private int selectionPhase; // 0: nicht, 1: wird gezogen 2: ist fest


// Rechteckselektion
    private double minLat=0;
    private double maxLat=0;
    private double minLon=0;
    private double maxLon=0;

    private double minLatSet=0;
    private double maxLatSet=0;
    private double minLonSet=0;
    private double maxLonSet=0;


    private boolean showSelectionMeters=false;

    private MapWidget mapWidget;


    public SelectionOverlay(MapWidget mapWidget) {
        this.mapWidget=mapWidget;
    }


    public void setShowSelectionMeters(boolean showSelectionMeters) {
        this.showSelectionMeters=showSelectionMeters;
    }


    public void unsetCondition() {
        selectionPhase=0;
    }


    public void selectRect(int lat1E6,int lon1E6,int lat2E6,int lon2E6) {
        selectionPhase=1;
        minLat=Math.min(lat1E6,lat2E6)/1e6d;
        maxLat=Math.max(lat1E6,lat2E6)/1e6d;
        minLon=Math.min(lon1E6,lon2E6)/1e6d;
        maxLon=Math.max(lon1E6,lon2E6)/1e6d;
    }


    public void setRectCondition(int lat1E6,int lon1E6,int lat2E6,int lon2E6) {
        selectionPhase=2;
        minLatSet=Math.min(lat1E6,lat2E6)/1e6d;
        maxLatSet=Math.max(lat1E6,lat2E6)/1e6d;
        minLonSet=Math.min(lon1E6,lon2E6)/1e6d;
        maxLonSet=Math.max(lon1E6,lon2E6)/1e6d;
    }

	
/**
* Performs the actual painting. The application developer has to overwrite this method.
* This method should never be called be the application developer.	
* @param g graphics to paint on
*/
    public void paint(Graphics2D g) {

        int[] xy1=null;
        int[] xy2=null;
        int[] xy3=null;
        int[] xy4=null;

        Stroke initialStroke = g.getStroke();

        Stroke currentStroke=null;
        Color currentFill=null;
        Color currentBorder=null;
        Color currentArrow=null;
        Color currentBox=null;

        switch (selectionPhase) {
            case 0:
                return;
            case 1:
                currentStroke=SELECTION_STROKE;
                currentFill=SELECTION_COLOR;
                currentBorder=SELECTION_BORDER_COLOR;
                currentArrow=SELECTION_ARROW_COLOR;
                currentBox=SELECTION_BOX_COLOR;
                break;
            case 2:
                currentStroke=SELECTION_STROKE;
                currentFill=SET_SELECTION_COLOR;
                currentBorder=SET_SELECTION_BORDER_COLOR;
                currentArrow=SET_SELECTION_ARROW_COLOR;
                currentBox=SET_SELECTION_BOX_COLOR;

                break;
        }
        g.setStroke(currentStroke);


        xy1=geo2pixelI(minLat,minLon);
        xy2=geo2pixelI(maxLat,minLon);
        xy3=geo2pixelI(maxLat,maxLon);
        xy4=geo2pixelI(minLat,maxLon);

        int[] xx=new int[4];
        int[] yy=new int[4];
        xx[0]=xy1[0];yy[0]=xy1[1];
        xx[1]=xy2[0];yy[1]=xy2[1];
        xx[2]=xy3[0];yy[2]=xy3[1];
        xx[3]=xy4[0];yy[3]=xy4[1];

        g.setColor(currentFill);
        g.fillPolygon(xx,yy,xx.length);

        g.setColor(currentBorder);
        g.drawPolygon(xx,yy,xx.length);

        if (showSelectionMeters) {
            paintMeterArrow(g,currentArrow,currentBox,minLat,minLon,minLat,maxLon,0,-15);
            paintMeterArrow(g,currentArrow,currentBox,minLat,maxLon,maxLat,maxLon,-15,0);
        }

        g.setStroke(initialStroke);
    }


    private void paintMeterArrow(Graphics2D g,Color linecol,Color boxcol,double lat1,double lon1,double lat2,double lon2,int offsetX,int offsetY) {
         double meters=Spherical.greatCircleMeters(lat1,lon1,lat2,lon2);
         if (meters<15)
             return;

         g.setColor(linecol);

         int[] pix1=geo2pixelI(lat1,lon1);
         int[] pix2=geo2pixelI(lat2,lon2);

         int fromX=pix1[0]+offsetX;
         int fromY=pix1[1]+offsetY;
         int toX=pix2[0]+offsetX;
         int toY=pix2[1]+offsetY;

         g.drawLine(fromX,fromY,toX,toY);

         int[] arrowPixels=arrowPoints(fromX,fromY,toX,toY,10);
         g.drawLine(arrowPixels[0],arrowPixels[1],arrowPixels[2],arrowPixels[3]);
         g.drawLine(arrowPixels[0],arrowPixels[1],arrowPixels[4],arrowPixels[5]);

         arrowPixels=arrowPoints(toX,toY,fromX,fromY,10);
         g.drawLine(arrowPixels[0],arrowPixels[1],arrowPixels[2],arrowPixels[3]);
         g.drawLine(arrowPixels[0],arrowPixels[1],arrowPixels[4],arrowPixels[5]);

         String metersStr=null;
         if (meters>=10000)
             metersStr=Math.round(meters/1000f)+"km";
         else
             metersStr=Math.round(meters)+"m";

         mapWidget.paintText(g,new Point((fromX+toX)/2,(fromY+toY)/2),boxcol,linecol,linecol,metersStr);
    }


    private int[] arrowPoints(int fromX,int fromY,int toX,int toY,int length) { 
        double l;
        double x;
        double y;
        double x2;
        double y2;
        double x3;
        double y3;
        
        int cx=0;
        int cy=0;
        int[] result;
        
        x=toX-fromX;
        y=toY-fromY;

        l=Math.sqrt(x*x+y*y);
        if (Math.abs(l)>1)
        {
            x=x/l*length;
            y=y/l*length;
        }

        x2=x*cosa-y*sina;
        y2=x*sina+y*cosa;

        x3=x*cosa+y*sina;
        y3=-x*sina+y*cosa;
        result=new int[6];

        result[0]=toX;
        result[1]=toY;
        result[2]=toX-(int)Math.round(x2);
        result[3]=toY-(int)Math.round(y2);
        result[4]=toX-(int)Math.round(x3);
        result[5]=toY-(int)Math.round(y3);

        return result;
    }

	
}
