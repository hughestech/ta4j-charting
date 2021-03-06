/*
 The MIT License (MIT)
 Copyright (c) 2017 Wimmer, Simon-Justus

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package chart;

import chart.types.IndicatorParameters.TaCategory;
import chart.types.IndicatorParameters.TaChartType;
import chart.types.IndicatorParameters.TaShape;
import chart.types.IndicatorParameters.TaStroke;
import chart.types.Paths;
import com.sun.org.apache.xml.internal.dtm.ref.DTMNodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TaPropertiesManager {

    private File propertiesFile;
    private Document doc;
    private XPath xPath;
    private StreamResult result;
    private Transformer transformer;


    public TaPropertiesManager(TaChartIndicatorBox chartIndicatorBox) {
        loadParametersFile();

    }

    /**
     * Load the property file (if there is any) and initialize the class variables
     */
    private void loadParametersFile() {
        try {
            ClassLoader cl = getClass().getClassLoader();
            URL fileURL = cl.getResource(Paths.PROPERTIES_FILE);

            if (fileURL == null) { // no file found create one
                propertiesFile = new File(Paths.PROPERTIES_FILE);
                propertiesFile.createNewFile();
            } else {
                propertiesFile = new File(fileURL.getFile());
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(propertiesFile);
            doc.getDocumentElement().normalize();
            XPathFactory xPathfactory = XPathFactory.newInstance();
            xPath = xPathfactory.newXPath();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            result = new StreamResult(propertiesFile);
        } catch (IOException e) {
            //TODO: Exception handling
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads a parameter from the parameter file
     *
     * @param key key for property
     * @param paramName default string array if not found
     * @return
     */
    public String getParameter(String key, String paramName) throws XPathExpressionException {
        String raw[] = key.split("_");
        String indicator = raw[0];
        String id = raw[1];
        String command = String.format("//indicator[@identifier='%s']/instance[@id='%s']/param[@name='%s']",indicator,id,paramName);
        XPathExpression expr = xPath.compile(command);
        Node resultNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
        return resultNode.getTextContent();
    }

    /**
     *
     * @param key the key of the indicator
     * @return the category if found, DEFAULT else
     * @throws XPathExpressionException d
     */
    public TaCategory getCategory(String key) throws XPathExpressionException {
        Node node = getNodeForInstance(key);
        String command = "@category";
        XPathExpression expr = xPath.compile(command);
        String categorie = (String) expr.evaluate(node, XPathConstants.STRING);
        if(categorie.equals(""))
            return TaCategory.DEFAULT;
        return TaCategory.valueOf(categorie);
    }

    //TODO overload those with extra int id for further color, shape and stroke params
    public Shape getShapeOf(String key) throws XPathExpressionException {
        Node node = getNodeForInstance(key);
        String command = "./param[@name='Shape']";
        XPathExpression expr = xPath.compile(command);
        Node paramNode = (Node) expr.evaluate(node,XPathConstants.NODE);
        String shape = paramNode.getTextContent();
        if (shape.equals(""))
            return null;
        return TaShape.valueOf(shape).getShape();
    }

    //TODO: add more colors
    public Paint getColorOf(String key) throws XPathExpressionException {
        Node node = getNodeForInstance(key);
        String command = "./param[@name='Color']";
        XPathExpression expr = xPath.compile(command);
        Node paramNode = (Node) expr.evaluate(node,XPathConstants.NODE);
        String color = paramNode.getTextContent();
        switch (color){
            case "YELLOW":
                return Color.YELLOW;
            case "BLUE":
                return Color.BLUE;
            case "GREEN":
                return Color.GREEN;
            case "RED":
                return Color.RED;
            default:
                return Color.MAGENTA;
        }
    }

    /**
     * returns the main {@link Stroke} of the indicator identified by key
     * @param key the identifier of the indicator
     * @return Stroke object or null
     * @throws XPathExpressionException
     */
    public Stroke getStrokeOf(String key) throws XPathExpressionException {
        Node node = getNodeForInstance(key);
        String command = "./param[@name='Stroke']";
        XPathExpression expr = xPath.compile(command);
        Node paramNode = (Node) expr.evaluate(node,XPathConstants.NODE);
        String stroke = paramNode.getTextContent();
        if(stroke.equals("")){
            return null;
        }
        return TaStroke.valueOf(stroke).getStroke();
    }

    public TaChartType getChartType(String key) throws XPathExpressionException {
        Node node = getNodeForInstance(key);
        String command = "./param[@name='Chart Type']";
        XPathExpression expr = xPath.compile(command);
        Node paramNode = (Node) expr.evaluate(node,XPathConstants.NODE);
        String chartType = paramNode.getTextContent();
        if(chartType.equals("")){
            return null;
        }
        return TaChartType.valueOf(chartType);
    }

    public void setParameter(String key, String paramName, String value) throws IOException, XPathExpressionException, TransformerException {
        String raw[] = key.split("_");
        String indicator = raw[0];
        String id = raw[1];
        String command = String.format("//indicator[@identifier='%s']/instance[@id='%s']/param[@name='%s']",indicator,id,paramName);
        XPathExpression expr = xPath.compile(command);
        Node resultNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
        //((Element) resultNode).getAttribute("type"); //TODO implement type check
        resultNode.setTextContent(value);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
    }

    /**
     * @return all indicator names for that are properties stored as a list
     */
    public java.util.List<String> getAllKeys() {
        List<String> keyList = new ArrayList<String>();
        NodeList indicators = doc.getElementsByTagName("indicator");
        for (int i = 0; i < indicators.getLength(); i++){
            Node node = indicators.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE){
                String indicator= ((Element)node).getAttribute("identifier");
                NodeList instances = ((Element) node).getElementsByTagName("instance");
                for (int j=0; j<instances.getLength(); j++){
                    String id = ((Element)instances.item(j)).getAttribute("id");
                    keyList.add(indicator+"_"+id);
                }
            }
        }
        return keyList;
    }

    public List<String> getKeysForCategory(TaCategory category) throws XPathExpressionException {
        String command = String.format("//instance[@category='%s']",category.toString());
        XPathExpression expr = xPath.compile(command);
        DTMNodeList nodes = (DTMNodeList) expr.evaluate(doc, XPathConstants.NODESET);

        List<String> keyList = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE){
                String id = ((Element) node).getAttribute("id");
                Element parent =(Element) node.getParentNode();
                String name = parent.getAttribute("identifier");
                keyList.add(name+"_"+id);
            }
        }
        return keyList;
    }

    /**
     * Get all parameters for a
     * @param key identifier of the indicator
     * @return a Map of name and value of the parameter
     */
    public Map<String,String> getParametersFor(String key) throws XPathExpressionException {
        String raw[] = key.split("_");
        String indicator = raw[0];
        String id = raw[1];
        String command = String.format("//indicator[@identifier='%s']/instance[@id='%s']/*",indicator,id);
        XPathExpression expr = xPath.compile(command);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        Map<String,String> mapNameValue = new HashMap<>();
        for (int i = 0; i<nodes.getLength(); i++){
            Node node = nodes.item(i);
            if(node.getNodeType()==Node.ELEMENT_NODE){
                Element paraElement = (Element) node;
                if(paraElement.getNodeName().equals("param")){
                    String name = paraElement.getAttribute("name");
                    String value = paraElement.getTextContent();
                    mapNameValue.put(name,value);
                }

            }
        }
        return mapNameValue;
    }

    public void duplicate(String key) throws XPathExpressionException, TransformerException {
        String raw[] = key.split("_");
        String indicator = raw[0];
        String id = raw[1];
        //get valid id (the biggest+1 ...)
        String command = String.format("//indicator[@identifier='%s']/instance",indicator,id);
        XPathExpression expr = xPath.compile(command);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        int nextID = 1;
        for(int i = 0; i < nodes.getLength(); i++){
            Element instance = (Element) nodes.item(i);
            int elementId = Integer.parseInt(instance.getAttribute("id"));
            if (nextID<=elementId){
                nextID = elementId;
            }
        }
        nextID++;

        // get instance, clone it and append to parent of instance
        Node toDuplicate = getNodeForInstance(key);
        Element duplicate = (Element) toDuplicate.cloneNode(true);
        duplicate.setAttribute("id", String.valueOf(nextID));
        Node parent = toDuplicate.getParentNode();
        parent.appendChild(duplicate);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
    }

    private Node getNodeForInstance(String key) throws XPathExpressionException {
        String raw[] = key.split("_");
        String indicator = raw[0];
        String id = raw[1];
        String command = String.format("//indicator[@identifier='%s']/instance[@id='%s']",indicator,id);
        XPathExpression expr = xPath.compile(command);
        return (Node) expr.evaluate(doc, XPathConstants.NODE);
    }


    public String getDescription(String key) throws XPathExpressionException {
        String raw[] = key.split("_");
        String indicator = raw[0];
        String command = String.format("//indicator[@identifier='%s']/description/text()",indicator);
        XPathExpression expr = xPath.compile(command);
        String result = (String) expr.evaluate(doc,XPathConstants.STRING);
        return result;
    }

    public String getParameterType(String key, String param) throws XPathExpressionException {
        Node instanceNode = getNodeForInstance(key);
        String command = String.format("./param[@name='%s']/@type",param);
        XPathExpression expr = xPath.compile(command);
        String result = (String) expr.evaluate(instanceNode,XPathConstants.STRING);
        return result;
    }
}
