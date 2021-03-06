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

import chart.types.IndicatorParameters.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.candles.LowerShadowIndicator;
import org.ta4j.core.indicators.candles.RealBodyIndicator;
import org.ta4j.core.indicators.candles.UpperShadowIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.keltner.KeltnerChannelLowerIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelUpperIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.volume.*;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;

import static chart.types.IndicatorParameters.TaCategory.DEFAULT;
import static chart.types.IndicatorParameters.TaCategory.HELPERS;

// TODO: overload notifyObserver function to change just entries that are modified
public class TaChartIndicatorBox extends Observable {
    private Map<String,TaChartIndicator> chartIndicatorMap;
    private Map<String, TradingRecord> tradingRecordMap;
    private TaPropertiesManager parameter;

    private TimeSeries series;
    private ClosePriceIndicator closePriceIndicator;

    /**
     * Constructor
     */
    public TaChartIndicatorBox(TimeSeries series){
        this.chartIndicatorMap = new HashMap<>();
        this.tradingRecordMap = new HashMap<>();
        this.series = series;
        this.closePriceIndicator = new ClosePriceIndicator(series);
        this.parameter = new TaPropertiesManager(this);
    }

    public void addTradingRecord(String name, TradingRecord record){
        this.tradingRecordMap.put(name,record);
    }

    public TradingRecord getTradingRecord(String name){
        return this.tradingRecordMap.get(name);
    }

    public Map<String,TradingRecord> getAllTradingRecords(){
        return this.tradingRecordMap;
    }

    // simple moving average
    private void loadSMAIndicator(String key) throws XPathException {
        int smaTimeFrame = Integer.parseInt(parameter.getParameter(key,"Time Frame"));
        TaColor color = TaColor.valueOf(parameter.getParameter(key, "Color"));
        TaStroke stroke = TaStroke.valueOf(parameter.getParameter(key, "Stroke"));
        TaShape shape = TaShape.valueOf(parameter.getParameter(key,"Shape"));
        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);

        TaChartIndicator sma = new TaChartIndicator(new SMAIndicator(closePriceIndicator, smaTimeFrame),
                String.format("%s (%s) (%s)",getIdentifier(key),getID(key),smaTimeFrame),
                createRendere(color,stroke,shape),
                chartType.toBoolean(),
                category);
        addChartIndicator(key, sma);
    }

    // exponential moving average
    private void loadEMAIndicator(String key)throws XPathException {
        int timeFrame = Integer.parseInt(parameter.getParameter(key,"Time Frame"));
        TaColor color = TaColor.valueOf(parameter.getParameter(key, "Color"));
        TaStroke stroke = TaStroke.valueOf(parameter.getParameter(key, "Stroke"));
        TaShape shape = TaShape.valueOf(parameter.getParameter(key,"Shape"));
        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);

        addChartIndicator(key, new EMAIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key),getID(key),timeFrame),
                createRendere(color, stroke, shape),
                chartType.toBoolean(),
                category);
    }

    //CCI
    private void loadCCIIndicator(String key)throws XPathException{
        int timeFrame = Integer.parseInt(parameter.getParameter(key,"Time Frame"));
        TaColor color = TaColor.valueOf(parameter.getParameter(key, "Color"));
        TaStroke stroke = TaStroke.valueOf(parameter.getParameter(key, "Stroke"));
        TaShape shape = TaShape.valueOf(parameter.getParameter(key,"Shape"));
        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);

        addChartIndicator(key, new CCIIndicator(series, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key),getID(key),timeFrame),
                createRendere(color, stroke, shape),
                chartType.toBoolean(),
                category);
    }

    //CMO
    private void loadCMOIndicator(String key)throws XPathException{
        int timeFrame = Integer.parseInt(parameter.getParameter(key,"Time Frame"));
        TaColor color = TaColor.valueOf(parameter.getParameter(key, "Color"));
        TaStroke stroke = TaStroke.valueOf(parameter.getParameter(key, "Stroke"));
        TaShape shape = TaShape.valueOf(parameter.getParameter(key,"Shape"));
        TaChartType chartType = TaChartType.valueOf(parameter.getParameter(key, "Chart Type"));
        TaCategory category = parameter.getCategory(key);

        addChartIndicator(key, new CMOIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key), getID(key), timeFrame),
                createRendere(color, stroke, shape),
                chartType.toBoolean(),
                category);
    }

    // Bollinger Bands and the width
    public void loadBollingerBands(String key) throws XPathException{
        List<Indicator> indicatorList = new ArrayList<>();
        List<String> namesList = new ArrayList<>();
        XYLineAndShapeRenderer bbRenderer = new XYLineAndShapeRenderer();
        int id = getID(key);

        int timeFrame = Integer.parseInt(parameter.getParameter(key,"Time Frame"));

        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePriceIndicator, timeFrame);
        EMAIndicator bollingerEMA = new EMAIndicator(closePriceIndicator,timeFrame);
        TaColor color1 = TaColor.valueOf(parameter.getParameter(key, "Color Middle Band"));
        TaStroke stroke1 = TaStroke.valueOf(parameter.getParameter(key, "Stroke Middle Band"));
        TaShape shape1 = TaShape.valueOf(parameter.getParameter(key,"Shape Middle Band"));
        TaColor color2 = TaColor.valueOf(parameter.getParameter(key, "Color Upper Band"));
        TaStroke stroke2 = TaStroke.valueOf(parameter.getParameter(key, "Stroke Upper Band"));
        TaShape shape2 = TaShape.valueOf(parameter.getParameter(key,"Shape Upper Band"));
        TaColor color3 = TaColor.valueOf(parameter.getParameter(key, "Color Lower Band"));
        TaStroke stroke3 = TaStroke.valueOf(parameter.getParameter(key, "Stroke Lower Band"));
        TaShape shape3 = TaShape.valueOf(parameter.getParameter(key,"Shape Lower Band"));
        TaChartType chartType = TaChartType.valueOf(parameter.getParameter(key, "Chart Type"));
        TaCategory category = parameter.getCategory(key);
        TaBoolean addWidth = TaBoolean.valueOf(parameter.getParameter(key, "Add Bollinger Bands Width"));

        BollingerBandsMiddleIndicator bbm = new BollingerBandsMiddleIndicator(bollingerEMA);
        BollingerBandsUpperIndicator bbu = new BollingerBandsUpperIndicator(bbm,sd);
        BollingerBandsLowerIndicator bbl = new BollingerBandsLowerIndicator(bbm,sd);

        indicatorList.add(bbm);
        indicatorList.add(bbu);
        indicatorList.add(bbl);

        namesList.add("Middle Band"+ timeFrame);
        namesList.add("Upper Band ");
        namesList.add("Lower Band ");

        bbRenderer.setSeriesPaint(0, color1.getPaint());
        bbRenderer.setSeriesStroke(0, stroke1.getStroke());
        bbRenderer.setSeriesShape(0, shape1.getShape());
        bbRenderer.setSeriesPaint(1, color2.getPaint());
        bbRenderer.setSeriesStroke(1,stroke2.getStroke());
        bbRenderer.setSeriesShape(1, shape2.getShape());
        bbRenderer.setSeriesPaint(2, color3.getPaint());
        bbRenderer.setSeriesStroke(2, stroke3.getStroke());
        bbRenderer.setSeriesShape(2, shape3.getShape());
        addChartIndicator(key,
                indicatorList,
                namesList,
                String.format("Bollinger Bands [%s] (%s)",id,timeFrame),
                bbRenderer,chartType.toBoolean(),
                category);
        if(addWidth.toBoolean()) {
            addChartIndicator("BollingerBandsWidth_" + getID(key),
                    new BollingerBandWidthIndicator(bbu, bbm, bbl),
                    String.format("Bollinger Band Width [%s]", id),
                    bbRenderer,
                    TaChartType.SUBCHART.toBoolean(),
                    category);
        } else{
            removeIndicator("BollingerBandsWidth_"+getID(key));
        }

    }


    public void loadPercentBIndicator(String key) throws XPathException{
        int timeFrame =Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        Decimal k = Decimal.valueOf(parameter.getParameter(key,"K Multiplier"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType type = parameter.getChartType(key);
        addChartIndicator(key,
                new PercentBIndicator(closePriceIndicator, timeFrame, k),
                String.format("%s [%s] (%s, %s)",getIdentifier(key),getID(key),timeFrame,k),
                renderer,
                type.toBoolean(),
                category);

    }

    //Amount Indicator
    public void loadAmountIndicator(String key) throws XPathExpressionException {
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key,
                new AmountIndicator(series),
                String.format("Amount [%s]", getID(key)),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // RSI Indicator
    public void loadRSIIndicator(String key) throws XPathExpressionException {
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");

        addChartIndicator(key,
                new RSIIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key), getID(key),timeFrame),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // SmoothedRSIIndicator
    public void loadSmoothedRSIIndicator(String key) throws XPathExpressionException {
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");

        addChartIndicator(key,
                new RSIIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key), getID(key),timeFrame),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // PVIIndicator
    public void loadPVIIndicator(String key) throws XPathExpressionException {
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");

        addChartIndicator(key,
                new PVIIndicator(series),
                String.format("%s [%s]",getIdentifier(key), getID(key)),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // NVIIndicator
    public void loadNVIIndicator(String key) throws XPathExpressionException {
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        addChartIndicator(key,
                new NVIIndicator(series),
                String.format("%s [%s]",getIdentifier(key), getID(key)),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // OnBalanceVolumeIndicator
    public void loadOnBalanceVolumeIndicator(String key) throws XPathExpressionException {
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");

        addChartIndicator(key,
                new OnBalanceVolumeIndicator(series),
                String.format("%s [%s]",getIdentifier(key), getID(key)),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // VWAPIndicator
    public void loadVWAPIndicator(String key) throws XPathExpressionException {
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");

        addChartIndicator(key,
                new VWAPIndicator(series, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key), getID(key), timeFrame),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // MACD Indicator
    public void loadMACDIndicator(String key) throws XPathExpressionException {
        int timeFrameShort = Integer.parseInt(parameter.getParameter(key, "Time Frame Short"));
        int timeFrameLong = Integer.parseInt(parameter.getParameter(key, "Time Frame Long"));
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        TaBoolean signalLine = TaBoolean.valueOf(parameter.getParameter(key, "Add Signal Line"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        MACDIndicator mcd = new MACDIndicator(closePriceIndicator, timeFrameShort, timeFrameLong);
        if(!signalLine.toBoolean()){
            addChartIndicator(key,
                    mcd,
                    String.format("%s [%s] (%s, %s)",getIdentifier(key), getID(key), timeFrameShort,timeFrameLong),
                    renderer,
                    chartType.toBoolean(),
                    category);

        } else{
            int timeFrameSignal = Integer.parseInt(parameter.getParameter(key, "Time Frame Signal Line"));
            List<String> names = new ArrayList<>();
            List<Indicator> indicators = new ArrayList<>();
            indicators.add(mcd);
            indicators.add(new EMAIndicator(mcd, timeFrameSignal));
            names.add(String.format("%s [%s] (%s, %s)",getIdentifier(key), getID(key), timeFrameShort,timeFrameLong));
            names.add(String.format("Signal Line [%s] (%s)",getID(key),timeFrameSignal));
            TaColor color = TaColor.valueOf(parameter.getParameter(key, "Color Signal Line"));
            TaShape shape = TaShape.valueOf(parameter.getParameter(key, "Shape Signal Line"));
            TaStroke stroke = TaStroke.valueOf(parameter.getParameter(key, "Stroke Signal Line"));
            renderer.setSeriesPaint(1,color.getPaint());
            renderer.setSeriesShape(1,shape.getShape());
            renderer.setSeriesStroke(1, stroke.getStroke());
            addChartIndicator(key,
                    indicators,
                    names,
                    String.format("%s [%s] (%s, %s)",getIdentifier(key), getID(key), timeFrameShort,timeFrameLong),
                    renderer,
                    chartType.toBoolean(),
                    category);
        }



    }

    //Average Directional Movement Down and Up
    public void loadAverageDirectionalMovementUP_DOWN(String key) throws XPathExpressionException {
        TaColor color1 = TaColor.valueOf(parameter.getParameter(key, "Color Up"));
        TaStroke stroke1 = TaStroke.valueOf(parameter.getParameter(key, "Stroke Up"));
        TaShape shape1 = TaShape.valueOf(parameter.getParameter(key,"Shape Up"));
        TaColor color2 = TaColor.valueOf(parameter.getParameter(key, "Color Down"));
        TaStroke stroke2 = TaStroke.valueOf(parameter.getParameter(key, "Stroke Down"));
        TaShape shape2 = TaShape.valueOf(parameter.getParameter(key,"Shape Down"));
        TaChartType chartType = TaChartType.valueOf(parameter.getParameter(key, "Chart Type"));
        TaCategory category = parameter.getCategory(key);
        int timeFrameUp = Integer.parseInt(parameter.getParameter(key, "Time Frame Up"));
        int timeFrameDown = Integer.parseInt(parameter.getParameter(key, "Time Frame Up"));
        List<Indicator> ilAdx = new ArrayList<>();
        List<String> nlAdx = new ArrayList<>();

        ilAdx.add(new AverageDirectionalMovementUpIndicator(series, timeFrameDown));
        ilAdx.add(new AverageDirectionalMovementDownIndicator(series, timeFrameDown));
        nlAdx.add("ADX UP "+timeFrameUp);
        nlAdx.add("ADX Down "+timeFrameUp);
        XYLineAndShapeRenderer adxRenderer = new XYLineAndShapeRenderer();
        adxRenderer.setSeriesPaint(0, color1.getPaint());
        adxRenderer.setSeriesStroke(0, stroke1.getStroke());
        adxRenderer.setSeriesShape(0, shape1.getShape());
        adxRenderer.setSeriesPaint(1, color2.getPaint());
        adxRenderer.setSeriesStroke(1, stroke2.getStroke());
        adxRenderer.setSeriesShape(1, shape2.getShape());
        addChartIndicator(key,
                ilAdx,
                nlAdx,
                String.format("%s [%s] (%s, %s)", getIdentifier(key), getID(key), timeFrameUp, timeFrameDown ),
                adxRenderer,
                chartType.toBoolean(),
                category);

    }

    // True Range Indicator
    public void loadTrueRangeIndicator(String key) throws XPathExpressionException {
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);
        addChartIndicator(key,
                new TrueRangeIndicator(series),
                String.format("%s [%s]",getIdentifier(key), getID(key)),
                renderer,
                chartType.toBoolean(),
                category);

    }

    // Keltner channels
    public void loadKeltner(String key) throws XPathException{

        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        Decimal ratio = Decimal.valueOf(parameter.getParameter(key, "Ratio"));
        int atr = Integer.parseInt(parameter.getParameter(key, "Time Frame ATR"));
        TaColor colorU = TaColor.valueOf(parameter.getParameter(key, "Color Upper"));
        TaStroke strokeU = TaStroke.valueOf(parameter.getParameter(key, "Stroke Upper"));
        TaShape shapeU = TaShape.valueOf(parameter.getParameter(key,"Shape Upper"));
        TaColor colorL = TaColor.valueOf(parameter.getParameter(key, "Color Lower"));
        TaStroke strokeL = TaStroke.valueOf(parameter.getParameter(key, "Stroke Lower"));
        TaShape shapeL = TaShape.valueOf(parameter.getParameter(key,"Shape Lower"));
        TaChartType chartType = TaChartType.valueOf(parameter.getParameter(key, "Chart Type"));
        TaCategory category = parameter.getCategory(key);

        XYLineAndShapeRenderer renderer = createRendere(key, "Color Middle", "Shape Middle", "Stroke Middle");
        renderer.setSeriesStroke(1, strokeU.getStroke());
        renderer.setSeriesStroke(2, strokeL.getStroke());
        renderer.setSeriesShape(1, shapeU.getShape());
        renderer.setSeriesShape(2, shapeL.getShape());
        renderer.setSeriesPaint(1, colorU.getPaint());
        renderer.setSeriesPaint(2, colorL.getPaint());

        KeltnerChannelMiddleIndicator kcM = new KeltnerChannelMiddleIndicator(series, timeFrame);
        KeltnerChannelUpperIndicator kcU = new KeltnerChannelUpperIndicator(kcM,ratio,atr);
        KeltnerChannelLowerIndicator kcL = new KeltnerChannelLowerIndicator(kcM,ratio,atr);

        List<Indicator> ilKelt = new ArrayList<>();
        List<String> nlKelt = new ArrayList<>();
        ilKelt.add(kcL);
        ilKelt.add(kcM);
        ilKelt.add(kcU);
        nlKelt.add("Keltner Lower");
        nlKelt.add("Keltner Middle");
        nlKelt.add("Keltner Upper");
        addChartIndicator(key,
                ilKelt,
                nlKelt,
                String.format("%s [%s] (%s, %s, %S)", getIdentifier(key), getID(key), timeFrame, ratio, atr),
                renderer,
                chartType.toBoolean(),
                category);
    }

    // Arron Up/Down in one subplot
    public void loadAroonUP_DOWN(String key) throws XPathException{
        int arronUp = Integer.parseInt(parameter.getParameter(key, "Time Frame Up"));
        int arronDown = Integer.parseInt(parameter.getParameter(key, "Time Frame Down"));
        TaColor colorD = TaColor.valueOf(parameter.getParameter(key, "Color Down"));
        TaStroke strokeD = TaStroke.valueOf(parameter.getParameter(key, "Stroke Down"));
        TaShape shapeD = TaShape.valueOf(parameter.getParameter(key, "Shape Down"));
        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);

        List<Indicator> ilAroon = new ArrayList<>();
        List<String> nlAroon = new ArrayList<>();
        ilAroon.add(new AroonDownIndicator(series, arronDown));
        ilAroon.add(new AroonUpIndicator(series, arronUp));
        nlAroon.add("Aroon Down "+arronDown);
        nlAroon.add("Aroon Up "+arronUp);
        XYLineAndShapeRenderer arronUpDownRenderer =createRendere(key, "Color Up", "Shape Up", "Stroke Up");

        arronUpDownRenderer.setSeriesPaint(1, colorD.getPaint());
        arronUpDownRenderer.setSeriesStroke(1, strokeD.getStroke());
        arronUpDownRenderer.setSeriesShape(1, shapeD.getShape());

        addChartIndicator(key,
                ilAroon,
                nlAroon,String.format("%s [%s] (%s, %s)",getIdentifier(key), getID(key),arronUp, arronDown),
                arronUpDownRenderer,
                chartType.toBoolean(),
                category);
    }

    // Lower Shadown Indicator
    public  void loadLowerShadowIndicator(String key) throws XPathExpressionException {
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        addChartIndicator(key,new LowerShadowIndicator(series),String.format("%s [%s]", getIdentifier(key), getID(key)),renderer, true, category);
    }

    // Upper Shadown Indicator
    public  void loadUpperShadowIndicator(String key) throws XPathExpressionException {
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        addChartIndicator(key,new UpperShadowIndicator(series),String.format("%s [%s]", getIdentifier(key), getID(key)),renderer, true, category);
    }

    // Upper Shadown Indicator
    public  void loadRealBodyIndicator(String key) throws XPathExpressionException {
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        addChartIndicator(key, new RealBodyIndicator(series),String.format("%s [%s]", getIdentifier(key), getID(key)),renderer, true, category);
    }

    // MVWAP + VWAP
    public void loadMVWAPIndicator(String key) throws XPathException{
        int timeFrameMVWAP = Integer.parseInt(parameter.getParameter(key, "Time Frame VWAP"));
        int timeFrameVWAP = Integer.parseInt(parameter.getParameter(key, "Time Frame MVWAP"));

        VWAPIndicator vwap = new VWAPIndicator(series,timeFrameVWAP);
        MVWAPIndicator mvwap = new MVWAPIndicator(vwap,timeFrameMVWAP);

        List<Indicator> ilVwap = new ArrayList<>();
        List<String> nlVwap = new ArrayList<>();

        XYLineAndShapeRenderer wapRenderer = createRendere(key, "Color MVWAP", "Shape MVWAP", "Stroke MVWAP");
        TaColor vwapColor = TaColor.valueOf(parameter.getParameter(key, "Color VWAP"));
        TaStroke vwapStroke = TaStroke.valueOf(parameter.getParameter(key, "Stroke VWAP"));
        TaShape vwapShape = TaShape.valueOf(parameter.getParameter(key, "Shape VWAP"));
        wapRenderer.setSeriesPaint(1, vwapColor.getPaint());
        wapRenderer.setSeriesStroke(1, vwapStroke.getStroke());
        wapRenderer.setSeriesShape(1, vwapShape.getShape());
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        ilVwap.add(mvwap);
        ilVwap.add(vwap);
        nlVwap.add(String.format("%s [%s] (%s)",getIdentifier(key), getID(key), timeFrameMVWAP));
        nlVwap.add(String.format("%s [%s] (%s)","VWAP", getID(key), timeFrameVWAP));
        addChartIndicator(key, ilVwap, nlVwap,"MVWAP/VWAP ",wapRenderer, chartType.toBoolean(), category);
    }

    // TrailingStopLossIndicator
    public void loadTraillingStopLossIndicator(String key) throws XPathException{
        Double threshold = Double.parseDouble(parameter.getParameter(key, "Threshold"));

        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);
        XYLineAndShapeRenderer xyLineAndShapeRenderer = createRendere(key, "Color", "Shape", "Stroke");
        addChartIndicator(key,
                new TrailingStopLossIndicator(closePriceIndicator,Decimal.valueOf(threshold)),
                String.format("%s [%s] (%s)", getIdentifier(key), getID(key), threshold),
                xyLineAndShapeRenderer,
                chartType.toBoolean(),
                category);
    }

    // Triple EMAIndicator
    public void loadTrippleEMAIndicator(String key) throws XPathException{
        int timeFrame =Integer.parseInt(parameter.getParameter(key, "Time Frame"));

        TaChartType chartType = parameter.getChartType(key);
        TaCategory category = parameter.getCategory(key);
        XYLineAndShapeRenderer xyLineAndShapeRenderer = createRendere(key, "Color", "Shape", "Stroke");
        addChartIndicator(key,
                new TripleEMAIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s] (%s)", getIdentifier(key), getID(key), timeFrame),
                xyLineAndShapeRenderer,
                chartType.toBoolean(),
                category);
    }

    // UlcerIndexIndicator
    public void loadUlcerIndexIndicator(String key) throws XPathException{
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key, new UlcerIndexIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s]", getIdentifier(key), getID(key)),renderer, chartType.toBoolean(), category);
    }


    // WMAIndicator
    public void loadWMAIndicator(String key) throws XPathException{
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key, new WMAIndicator(closePriceIndicator, timeFrame),String.format("%s [%s] (%s)",
                getIdentifier(key), getID(key), timeFrame),renderer, chartType.toBoolean(), category);
    }


    // ZLEMAIndicator
    public void loadZLEMAIndicator(String key) throws XPathException{
        int ZLEMAIndicator_1 = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key,
                new ZLEMAIndicator(closePriceIndicator, ZLEMAIndicator_1),String.format("%s [%s] (%s)",
                        getIdentifier(key), getID(key),ZLEMAIndicator_1), renderer, chartType.toBoolean(), category);
    }




    // RAVI Indicator
    public void loadRAVIIndicator(String key) throws XPathException{
        int timeFrameShort = Integer.parseInt(parameter.getParameter(key, "Time Frame Short"));
        int timeFrameLong = Integer.parseInt(parameter.getParameter(key, "Time Frame Long"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);

        addChartIndicator(key,new RAVIIndicator(closePriceIndicator, timeFrameShort, timeFrameLong),
                String.format("%s [%s] (%s, %s)", getIdentifier(key), getID(key),timeFrameShort,timeFrameLong),
                renderer,chartType.toBoolean(), category);
    }


    // ROC Indicator
    public void loadROCIndicator(String key) throws XPathException{
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key,
                new ROCIndicator(closePriceIndicator, timeFrame),String.format("%s [%s] (%s)",
                        getIdentifier(key), getID(key), timeFrame), renderer, chartType.toBoolean(), category);
    }

    // Fisher Indicator
    public void loadFischerIndicator(String key) throws XPathException{
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        double alpha = Double.parseDouble(parameter.getParameter(key, "Alpha"));
        double beta = Double.parseDouble(parameter.getParameter(key, "Beta"));
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key, new FisherIndicator(closePriceIndicator, timeFrame, Decimal.valueOf(alpha), Decimal.valueOf(beta)),
                String.format("%s [%s] (%s, %s, %s)", getIdentifier(key), getID(key),timeFrame,alpha,beta),
                chartType.toBoolean(), category);
    }

    // HMA Indicator
    public void loadHMAIndicator(String key) throws XPathException {
        int timeFrame = Integer.parseInt(parameter.getParameter(key, "Time Frame"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);
        addChartIndicator(key, new HMAIndicator(closePriceIndicator, timeFrame),
                String.format("%s [%s] (%s)",getIdentifier(key), getID(key), timeFrame), chartType.toBoolean(),
                category);
    }

    // KAMA Indicator
    public void loadKAMAIndicator(String key) throws XPathException{
        int timeFrameEffRatio = Integer.parseInt(parameter.getParameter(key, "Time Frame Effective Ratio"));
        int timeFrameFast = Integer.parseInt(parameter.getParameter(key, "Time Frame Slow"));
        int timeFrameSlow = Integer.parseInt(parameter.getParameter(key, "Time Frame Fast"));
        XYLineAndShapeRenderer renderer = createRendere(key, "Color", "Shape", "Stroke");
        TaCategory category = parameter.getCategory(key);
        TaChartType chartType = parameter.getChartType(key);

        addChartIndicator(key,new KAMAIndicator(closePriceIndicator,timeFrameEffRatio,timeFrameFast,timeFrameSlow),
                String.format("%s [%s] (%s, %s, %s)",getIdentifier(key), getID(key), timeFrameEffRatio, timeFrameFast, timeFrameSlow),
                chartType.toBoolean(), category);
    }


    /**
     * Creates and add all ta4j indicators with generic type Decimal to the box.
     * Use the parameter from the indicatorParameter.properties for indicator parameter
     */
    public void initAllIndicators(){

        addChartIndicator("closePriceIndicator", closePriceIndicator, false, HELPERS);

        List<String> allKeys = parameter.getAllKeys();
        for(String key: allKeys){
            try {
                reloadIndicator(key);
            } catch (XPathException xpe){
                //TODO: handle exception
                xpe.printStackTrace();
            }
        }

        /*
        // Average Gain indicator
        int averGainTimeFrame = parameter.getOneIntFor("AverageGainIndicator_1",20);
        addChartIndicator(new AverageGainIndicator(closePriceIndicator, averGainTimeFrame),"Average Gain"+averGainTimeFrame, true,
                TaTypes.categories.HELPERS);

        // Average Loss indicator
        int averLossTimeFrame = parameter.getOneIntFor("AverageLossIndicator_1",20);
        addChartIndicator(new AverageLossIndicator(closePriceIndicator, averLossTimeFrame),"Average Gain "+averLossTimeFrame, true,
                TaTypes.categories.HELPERS);

        // Average True Range indicator
        int averTrueRangeTimeFrame = parameter.getOneIntFor("AverageTrueRangeIndicator_1",20);
        addChartIndicator("AverageTrueRangeIndicator",new AverageTrueRangeIndicator(series, averTrueRangeTimeFrame), true,
                TaTypes.categories.HELPERS);

        // Close Location Value indicator
        addChartIndicator("CloseLocationValueIndicator",new CloseLocationValueIndicator(series), true,
                TaTypes.categories.HELPERS);

        // Constant Indicator TODO: does not work, time series is null
        // buildChartIndicator(new ConstantIndicator(closePriceIndicator.getValue(0)),Color.RED,"Constant First CP", false);

        // Cumulated Gains Indicator
        int cumulatedGainsFrame = parameter.getOneIntFor("CumulatedGainsIndicator_1",20);
        addChartIndicator("CumulatedGainsIndicator_1",new CumulatedGainsIndicator(closePriceIndicator,cumulatedGainsFrame), true,
                TaTypes.categories.HELPERS);

        // Cumulated Losses Indicator
        addChartIndicator("CumulatedLossesIndicator",new CumulatedLossesIndicator(closePriceIndicator,20), true,
                TaTypes.categories.HELPERS);

        // Directional Up and Down Indicator
        int directUp = parameter.getOneIntFor("DirectionalUpIndicator_1",20);
        int directDown = parameter.getOneIntFor("DirectionalDownIndicator_1",20);
        List<Indicator> ilDud = new ArrayList<>();
        ilDud.add(new DirectionalDownIndicator(series,directUp));
        ilDud.add(new DirectionalUpIndicator(series,directDown));
        List<String> nlDud = new ArrayList<>();
        nlDud.add("Directional Down "+directDown);
        nlDud.add("Directional Up "+directUp);
        XYLineAndShapeRenderer directUpDownRenderer = new XYLineAndShapeRenderer();
        directUpDownRenderer.setSeriesPaint(0, Color.RED);
        directUpDownRenderer.setSeriesStroke(0, TaTypes.SMALL_LINE);
        directUpDownRenderer.setSeriesShape(0, TaTypes.shape_smallRec );
        directUpDownRenderer.setSeriesPaint(1, Color.GREEN);
        directUpDownRenderer.setSeriesStroke(1,TaTypes.SMALL_LINE);
        directUpDownRenderer.setSeriesShape(1, TaTypes.shape_smallRec);
        addChartIndicator(ilDud, nlDud, "Directional Up/Down "+directUp+", "+directDown,directUpDownRenderer, true,
                TaTypes.categories.HELPERS);


        //Directional Movement Down and UP
        List<Indicator> ilDmud = new ArrayList<>();
        ilDmud.add(new DirectionalMovementDownIndicator(series));
        ilDmud.add(new DirectionalMovementUpIndicator(series));
        List<String> nlmDud = new ArrayList<>();
        nlmDud.add("Directional Movement Down");
        nlmDud.add("Directional Movement Up");

        XYLineAndShapeRenderer dmudRenderer = new XYLineAndShapeRenderer();
        dmudRenderer.setSeriesPaint(0, Color.RED);
        dmudRenderer.setSeriesStroke(0, TaTypes.SMALL_LINE);
        dmudRenderer.setSeriesShape(0, TaTypes.shape_smallRec );
        dmudRenderer.setSeriesPaint(1, Color.GREEN);
        dmudRenderer.setSeriesStroke(1,TaTypes.SMALL_LINE);
        dmudRenderer.setSeriesShape(1, TaTypes.shape_smallRec);
        addChartIndicator(ilDmud, nlmDud, "Directional Movement Up/Down", dmudRenderer,true,
                TaTypes.categories.HELPERS);

        // Highest Value Indicator
        int highestValueT = parameter.getOneIntFor("highestValue_1",20);
        addChartIndicator(new HighestValueIndicator(closePriceIndicator, highestValueT), "Highest Value "+highestValueT,false, TaTypes.categories.HELPERS);

        // Lowest Value Indicator
        int lowestValueT = parameter.getOneIntFor("lowestValue_1",20);
        addChartIndicator(new LowestValueIndicator(closePriceIndicator, lowestValueT),"Lowest Value "+lowestValueT,false, TaTypes.categories.HELPERS);

        // Max Price Indicator
        addChartIndicator(new MaxPriceIndicator(series),"Max Price Indicator",false, TaTypes.categories.HELPERS);

        // Mean Deviation Indicator
        int meanDevT = parameter.getOneIntFor("MeanDeviationIndicator_1",20);
        addChartIndicator(new MeanDeviationIndicator(closePriceIndicator, meanDevT),
                "Mean Deciation "+meanDevT,false, TaTypes.categories.HELPERS);

        // Mean Price Indicator
        addChartIndicator("MedianPriceIndicator",new MedianPriceIndicator(series),false, TaTypes.categories.HELPERS);

        // Median Price Indicator
        addChartIndicator("MedianPriceIndicator",new MedianPriceIndicator(series),false, TaTypes.categories.HELPERS);

        // Open price Indicator
        addChartIndicator("OpenPriceIndicator",new OpenPriceIndicator(series),false, TaTypes.categories.HELPERS);

        // Previous Value Indicator
        int prevValT = parameter.getOneIntFor("PreviousValueIndicator_1",1);
        addChartIndicator(new PreviousValueIndicator(closePriceIndicator, prevValT),
                "Previous Value (cp, "+prevValT+")", false, TaTypes.categories.HELPERS);

        // Price Variantion Indicator
        addChartIndicator("PriceVariationIndicator",new PriceVariationIndicator(series),true, TaTypes.categories.HELPERS);

        // Smoothed Average Gain Indicator+
        int meanDevTimeFrame = parameter.getOneIntFor("SmoothedAverageGainIndicator_1",20);
        addChartIndicator("SmoothedAverageGainIndicator",new SmoothedAverageGainIndicator(closePriceIndicator,20),true, TaTypes.categories.HELPERS);

        // Smoothed Average Gain Indicator
        addChartIndicator("SmoothedAverageLossIndicator",new SmoothedAverageLossIndicator(closePriceIndicator,20),true, TaTypes.categories.HELPERS);

        // Trade Count //TODO: integer does not work
        //addChartIndicator(new TradeCountIndicator(series),true, TaTypes.categories.HELPERS);

        // Typical Price Indicator
        addChartIndicator("TypicalPriceIndicator",new TypicalPriceIndicator(series),false, TaTypes.categories.HELPERS);

        // Volume Indicator
        addChartIndicator("VolumeIndicator",new VolumeIndicator(series),true, TaTypes.categories.HELPERS);

        //ichimoku
        int kijunSenPara = parameter.getOneIntFor("IchimokuKijunSenIndicator_1",20);
        int tenkanSenPara = parameter.getOneIntFor("IchimokuTenkanSenIndicator_1",26);
        int spanB = parameter.getOneIntFor("IchimokuSenkouSpanBIndicator_1",56);
        List<Indicator> ilIchi = new ArrayList<>();
        IchimokuKijunSenIndicator kijunSen = new IchimokuKijunSenIndicator(series, kijunSenPara);
        IchimokuTenkanSenIndicator tenkanSen = new IchimokuTenkanSenIndicator(series, tenkanSenPara);

        ilIchi.add(new IchimokuSenkouSpanAIndicator(series, tenkanSen, kijunSen));
        ilIchi.add(new IchimokuSenkouSpanBIndicator(series, spanB));
        ilIchi.add(kijunSen);
        ilIchi.add(tenkanSen);
        XYLineAndShapeRenderer ichiRenderer = new XYLineAndShapeRenderer();
        ichiRenderer.setSeriesPaint(0, Color.GREEN);
        ichiRenderer.setSeriesStroke(0, TaTypes.SMALL_LINE);
        ichiRenderer.setSeriesShape(0, TaTypes.NONE );
        ichiRenderer.setSeriesPaint(1, Color.RED);
        ichiRenderer.setSeriesStroke(1,TaTypes.SMALL_LINE);
        ichiRenderer.setSeriesShape(1, TaTypes.NONE);
        ichiRenderer.setSeriesPaint(2, Color.RED.brighter());
        ichiRenderer.setSeriesStroke(2,TaTypes.SMALL_LINE);
        ichiRenderer.setSeriesShape(2, TaTypes.NONE);
        ichiRenderer.setSeriesPaint(3, Color.BLUE);
        ichiRenderer.setSeriesStroke(3,TaTypes.SMALL_LINE);
        ichiRenderer.setSeriesShape(3, TaTypes.NONE);
        List<String> nlIchi = new ArrayList<>();
        nlIchi.add("Senkou Span A (TenkanSen, KijunSen)"); // no idea what i am doing^^
        nlIchi.add("Senkou Span B "+spanB);
        nlIchi.add("KijunSen "+kijunSenPara);
        nlIchi.add("TenkanSen "+tenkanSenPara);
        addChartIndicator(ilIchi,nlIchi,"Ichimoku All",ichiRenderer, false, TaTypes.categories.ICHIMOKU);

        addChartIndicator("IchimokuKijunSenIndicator_1",kijunSen, false, TaTypes.categories.ICHIMOKU);
        addChartIndicator("IchimokuTenkanSenIndicator_1",tenkanSen, false, TaTypes.categories.ICHIMOKU);



        // Correlation Coefficient Indicator
        int correlationTimeFrame = parameter.getOneIntFor("CorrelationCoefficientIndicator_1",5);
        addChartIndicator(new CorrelationCoefficientIndicator(closePriceIndicator, new MinPriceIndicator(series),correlationTimeFrame),
           "Correlation Coefficient cp, minP"+correlationTimeFrame,true, TaTypes.categories.STATISTICS);

        // Covariance Indicator
        int covarrianceTimeFrame = parameter.getOneIntFor("CovarianceIndicator_1",5);
        addChartIndicator(new CovarianceIndicator(closePriceIndicator,new MinPriceIndicator(series),20),
             "Covariance Indicator cp minP "+covarrianceTimeFrame,true, TaTypes.categories.STATISTICS);

        // Period Growth Rate Indicator
        int periodicalGrowthRateFrame = parameter.getOneIntFor("PeriodicalGrowthRateIndicator_1",5);
        addChartIndicator(new PeriodicalGrowthRateIndicator(closePriceIndicator,periodicalGrowthRateFrame),"Period Growth Rate cp "+periodicalGrowthRateFrame,true, TaTypes.categories.STATISTICS);

        // Simple Linear Regression Indicator
        int simpleLinearRegressionTimeFrame = parameter.getOneIntFor("SimpleLinearRegressionIndicator_1",15);
        addChartIndicator(new SimpleLinearRegressionIndicator(closePriceIndicator,simpleLinearRegressionTimeFrame), "Simple Linear Regression cp "+simpleLinearRegressionTimeFrame,false, TaTypes.categories.STATISTICS);

        // Standard Deviatation Indicator
        int standardDeviationTimeFrame = parameter.getOneIntFor("StandardDeviationIndicator_1",15);
        addChartIndicator(new StandardDeviationIndicator(closePriceIndicator, standardDeviationTimeFrame),
               "Standard Deviatation cp "+standardDeviationTimeFrame, true, TaTypes.categories.STATISTICS);

        //Standard Error Indicator
        int standardErrorTimeFrame = parameter.getOneIntFor("standardErrorTimeFrame",5);
        addChartIndicator(new StandardErrorIndicator(closePriceIndicator,standardErrorTimeFrame), "Standard Error cp "+standardErrorTimeFrame, true, TaTypes.categories.STATISTICS);

        //VarianceIndicator
        int varianceTimeFrame = parameter.getOneIntFor("VarianceIndicator_1",10);
        addChartIndicator(new VarianceIndicator(closePriceIndicator,varianceTimeFrame),"Variance cp "+varianceTimeFrame, true, TaTypes.categories.STATISTICS);

        //Accerleration Deceleration Indicator
        int[] accDeDef = {20,50};
        accDeDef = parameter.getXIntFor("AccelerationDecelerationIndicator_1",2,accDeDef);
        addChartIndicator(new AccelerationDecelerationIndicator(series,20,50),"Accel. Decel. "+accDeDef[0]+" "+accDeDef[1], true, TaTypes.categories.DEFAULT);



        // Average Directional Movement
        int admTimeFrame = parameter.getOneIntFor("AverageDirectionalMovementIndicator_1",14);
        AverageDirectionalMovementIndicator admd = new AverageDirectionalMovementIndicator(series, 14);
        addChartIndicator(new AverageDirectionalMovementDownIndicator(series, admTimeFrame),
                "ADX "+admTimeFrame,true, TaTypes.categories.DEFAULT);

        // Awesome Oscillator
        int[] awsDef = {5,34};
        int[] awesomeOscillator= parameter.getXIntFor("AwesomeOscillatorIndicator_1",2,awsDef);
        addChartIndicator(new AwesomeOscillatorIndicator(closePriceIndicator,awesomeOscillator[0],awesomeOscillator[1]),
                "Awesome Oscillator cp "+awesomeOscillator[0]+" "+awesomeOscillator[1],true, TaTypes.categories.DEFAULT);



        XYLineAndShapeRenderer chandLong = new XYLineAndShapeRenderer();
        chandLong.setSeriesStroke(0,TaTypes.SMALL_LINE);
        chandLong.setSeriesPaint(0,Color.GREEN);
        chandLong.setSeriesShape(0, TaTypes.NONE);

        // ChandelierExitLongIndicator
        addChartIndicator(new ChandelierExitLongIndicator(series),"Chandelier Exit Long", chandLong,false, TaTypes.categories.DEFAULT);

        XYLineAndShapeRenderer chandShort = new XYLineAndShapeRenderer();
        chandShort.setSeriesStroke(0,TaTypes.SMALL_LINE);
        chandShort.setSeriesPaint(0,Color.RED);
        chandShort.setSeriesShape(0, TaTypes.NONE);
        // ChandelierExitShortIndicator
        addChartIndicator(new ChandelierExitShortIndicator(series),"Chandelier Exit Short",chandShort,false, TaTypes.categories.DEFAULT);

        // CMO Indicator
        int cmoTimeFrame = parameter.getOneIntFor("CMOIndicator_1",14);
        addChartIndicator(new CMOIndicator(closePriceIndicator, cmoTimeFrame),"CMO cp "+cmoTimeFrame,
                true, TaTypes.categories.DEFAULT);

        // Coppock Curve Indicator
        int[]coppCurveDef = {14,11,10};
        coppCurveDef = parameter.getXIntFor("CoppockCurveIndicator_1",3,coppCurveDef);
        addChartIndicator(new CoppockCurveIndicator(closePriceIndicator,coppCurveDef[0],coppCurveDef[1],coppCurveDef[2]),
                "Coppock Curve "+coppCurveDef[0]+" "+coppCurveDef[1]+" "+coppCurveDef[2], true, TaTypes.categories.DEFAULT);

        // Directional Movement Indicator
        int directMoveTimeFrame = parameter.getOneIntFor("DirectionalMovementIndicator_1",14);
        addChartIndicator(new DirectionalMovementIndicator(series, directMoveTimeFrame),
                "Directional Movement "+directMoveTimeFrame, true, TaTypes.categories.DEFAULT);

        // Double EMA Indicator
        int doubleEmaTimeFrame = parameter.getOneIntFor("DoubleEMAIndicator_1", 20);
        addChartIndicator(new DoubleEMAIndicator(closePriceIndicator, doubleEmaTimeFrame),
                "DoubleEMA cp "+doubleEmaTimeFrame, false, TaTypes.categories.DEFAULT);

        // DPO Indicator
        /**
         * @since since the repository moved to https://github.com/ta4j/ta4j/ this indicator has been fixed
         */
        /*
        int dpoFrame = parameter.getOneIntFor("DPOIndicator_1",20);
        addChartIndicator(new indicators.DPOIndicator(closePriceIndicator,20),
                "DPO (20)", true, TaTypes.categories.DEFAULT);



        // Mass Index Indicator
        int[] massParams = {20,10};
        massParams = parameter.getXIntFor("MACDIndicator_1",2,macParams);
        addChartIndicator(new MassIndexIndicator(series,massParams[0],massParams[1]),
                "Mass Index Indicator "+massParams[0]+" "+massParams[1], true, TaTypes.categories.DEFAULT);

        /**@since since the repository moved to https://github.com/ta4j/ta4j/ this indicator has been fixed*/
        // Parabolic Sar Indicator
        /*
        int parabolicTimeFrame = parameter.getOneIntFor("ParabolicSarIndicator_1", 20);
        indicators.ParabolicSarIndicator nPara = new indicators.ParabolicSarIndicator(series);
        XYLineAndShapeRenderer parabolicRenderer = new XYLineAndShapeRenderer();
        parabolicRenderer.setSeriesPaint(0,Color.YELLOW);
        parabolicRenderer.setSeriesStroke(0, TaTypes.BIG_DOTS);
        addChartIndicator(nPara, "Parabolic Sar "+parabolicTimeFrame, parabolicRenderer, false, TaTypes.categories.DEFAULT);

        // PPO Indicator
        int[] ppoDef = {10,50};
        ppoDef = parameter.getXIntFor("PPOIndicator", 2, ppoDef);
        addChartIndicator(new PPOIndicator(closePriceIndicator, 10, 50),
                "PPO (10,50)", false, TaTypes.categories.DEFAULT);


        // Random Walk High and Low
        int rwHighTimeFrame = parameter.getOneIntFor("RandomWalkIndexHighIndicator_1",20);
        int rwLowTimeFrame = parameter.getOneIntFor("RandomWalkIndexLowIndicator_1",20);
        List<Indicator> ilRw = new ArrayList<>();
        List<String> nlRw = new ArrayList<>();

        ilRw.add(new RandomWalkIndexHighIndicator(series, rwHighTimeFrame));
        ilRw.add(new RandomWalkIndexLowIndicator(series, rwLowTimeFrame));
        nlRw.add("Random Walk High " + rwHighTimeFrame);
        nlRw.add("Random Walk Low " + rwLowTimeFrame );
        XYLineAndShapeRenderer randWalkRenderer = new XYLineAndShapeRenderer();
        randWalkRenderer.setSeriesPaint(0, Color.GREEN);
        randWalkRenderer.setSeriesStroke(0,TaTypes.SMALL_LINE);
        randWalkRenderer.setSeriesShape(0, TaTypes.NONE);
        randWalkRenderer.setSeriesPaint(1, Color.RED);
        randWalkRenderer.setSeriesStroke(1,TaTypes.SMALL_LINE);
        randWalkRenderer.setSeriesShape(1, TaTypes.NONE);
        addChartIndicator(ilRw,nlRw,"Random Walk "+rwHighTimeFrame+" "+rwLowTimeFrame,randWalkRenderer,true, TaTypes.categories.DEFAULT);

        // Stochastic RSI Indicator
        int stochRsiPara = parameter.getOneIntFor("StochasticRSIIndicator_1", 20);
        addChartIndicator(new StochasticRSIIndicator(closePriceIndicator, 20),
                "Stochastic RSIIndicator cp "+stochRsiPara, true, TaTypes.categories.DEFAULT);

        // StochasticOscillatorKIndicator StochasticOscillatorDIndicator
        List<Indicator> ilStKd = new ArrayList<>();
        List<String> nlStKd = new ArrayList<>();
        int stochOindicator = parameter.getOneIntFor("StochasticOscillatorKIndicator_1",20);
        StochasticOscillatorKIndicator stk = new StochasticOscillatorKIndicator(series, stochOindicator);
        StochasticOscillatorDIndicator std = new StochasticOscillatorDIndicator(stk);

        ilStKd.add(stk);
        ilStKd.add(std);
        nlStKd.add("Stoch. O. K "+stochOindicator);
        nlStKd.add("Stoch. O. D (K)");
        XYLineAndShapeRenderer osziRender = new XYLineAndShapeRenderer();
        osziRender.setSeriesPaint(0, Color.BLUE);
        osziRender.setSeriesStroke(0,TaTypes.SMALL_LINE);
        osziRender.setSeriesShape(0, TaTypes.NONE);
        osziRender.setSeriesPaint(1, Color.MAGENTA);
        osziRender.setSeriesStroke(1,TaTypes.SMALL_LINE);
        osziRender.setSeriesShape(1, TaTypes.NONE);
        addChartIndicator(ilStKd,nlStKd,"Stochastic Oscilator D K "+stochOindicator,osziRender, true, TaTypes.categories.DEFAULT);

        addChartIndicator(new StochasticOscillatorDIndicator(closePriceIndicator),
                "Stochastic Oscillator D", true, TaTypes.categories.DEFAULT);
        addChartIndicator(new StochasticOscillatorKIndicator(series, stochOindicator),
                "Stochastic Oscillator K "+stochOindicator, true, TaTypes.categories.DEFAULT);


        // AccumulationDistributionIndicator
        addChartIndicator(new AccumulationDistributionIndicator(series),"Accumulation Distribution", true, TaTypes.categories.VOLUME);

        // Chaikin Money Flow Indicator
        int ChaikinMoneyFlowIndicator_1 = parameter.getOneIntFor("ChaikinMoneyFlowIndicator_1",20);
        addChartIndicator(new ChaikinMoneyFlowIndicator(series,ChaikinMoneyFlowIndicator_1),
                "Chaikin Money "+ChaikinMoneyFlowIndicator_1, true, TaTypes.categories.VOLUME);



        */

    }

    public void addChartIndicator(String identifier, List<Indicator> indicators, List<String> names, String generalName,XYLineAndShapeRenderer renderer, boolean isSubchart, TaCategory c){
        chartIndicatorMap.put(identifier, new TaChartIndicator(indicators,names,generalName,renderer,isSubchart,c));
        setChanged();
        notifyObservers();
    }

    /**
     * Build and add an chart indicator to charts indicator list (random color, default name)
     * @param indicator the ta4j indicator
     * @param isSubchart flag if indicator should be plotted on sub chart
     */
    public void addChartIndicator(Indicator indicator, boolean isSubchart){
         chartIndicatorMap.put(indicator.toString(),new TaChartIndicator(indicator, indicator.toString(), isSubchart, DEFAULT));
        setChanged();
        notifyObservers();
    }

    /**
     * Build and add an chart indicator to charts indicator list (random color, default name)
     * @param indicator the ta4j indicator
     * @param isSubchart flag if indicator should be plotted on su bchart
     * @param c the category of the chart
     */
    public void addChartIndicator(String identifier, Indicator indicator, boolean isSubchart, TaCategory c){
        chartIndicatorMap.put(identifier, new TaChartIndicator(indicator,indicator.toString(), isSubchart, c));
        setChanged();
        notifyObservers();
    }

    /**
     * Build and add an chart indicator to charts indicator list
     * @param indicator the ta4j indicator
     * @param isSubchart flag if indicator should be plotted on sub chart
     * @param name the name of the indicator that should be displayed
     * @param c the category of the chart
     */
    public void addChartIndicator(String identifier, Indicator indicator, String name, boolean isSubchart, TaCategory c){
        chartIndicatorMap.put(identifier, new TaChartIndicator(indicator, name, isSubchart, c));
        setChanged();
        notifyObservers();
    }

    public void addChartIndicator(String identifier, Indicator indicator, String name,XYLineAndShapeRenderer renderer, boolean isSubchart, TaCategory c){
        chartIndicatorMap.put(identifier, new TaChartIndicator(indicator, name, renderer, isSubchart, c));
        setChanged();
        notifyObservers();
    }

    /**
     * Adds an existing TaChartIndicator to the box
     * @param taChartIndicator an TaChartIndicator
     */
    public void addChartIndicator(String identifier, TaChartIndicator taChartIndicator){
        chartIndicatorMap.put(identifier, taChartIndicator);
        setChanged();
        notifyObservers();
    }

    public void removeIndicator(String key){
        this.chartIndicatorMap.remove(key);
    }

    /**
     * Get all indicators that are stored in this box
     * @return all ChartIndicators that are stored in this box
     */
    public Map<String, TaChartIndicator> getChartIndicatorMap() {
        return chartIndicatorMap;
    }

    /**
     * Returns the indicator that is stored for the identifier
     * @param identifier the identifier of the indicator (display identifier/general identifier/properties identifier)
     * @return the indicator that is stored for the identifier
     */
    public TaChartIndicator getChartIndicator(String identifier){
        return this.chartIndicatorMap.get(identifier);
    }

    public TaPropertiesManager getPropertiesManager(){
        return this.parameter;
    }

    //TODO: implement public initXXXIndicator(Object params..) functions for creating "subindicators" e.g. keltner, bollinger..
    /**
     * Reloads the indicator from the properties file
     * @param key
     */
    public void reloadIndicator(String key) throws IllegalArgumentException, XPathException{
        String indicatorName = key.split("_")[0];

        switch (indicatorName){
            case "SMAIndicator": {
                loadSMAIndicator(key);
                break;
            }
            case "CCIIndicator":{
                loadCCIIndicator(key);
                break;
            }
            case "EMAIndicator":{
                loadEMAIndicator(key);
                break;
            }
            case "CMOIndicator":{
                loadCMOIndicator(key);
                break;
            }
            case "BollingerBands": {
                loadBollingerBands(key);
                break;
            }
            case "PercentBIIndicator":{
                loadPercentBIndicator(key);
                break;
            }
            case "AmountIndicator":{
                loadAmountIndicator(key);
                break;
            }

            case "RSIIndicator":{
                loadRSIIndicator(key);
                break;
            }
            case "SmoothedRSIIndicator":{
                loadSmoothedRSIIndicator(key);
                break;
            }

            case "PVIIndicator":{
                loadPVIIndicator(key);
                break;
            }
            case "NVIIndicator":{
                loadNVIIndicator(key);
                break;
            }
            case "OnBalanceVolumeIndicator":{
                loadOnBalanceVolumeIndicator(key);
                break;
            }
            case "MACDIndicator":{
                loadMACDIndicator(key);
                break;
            }
            case "AverageDirectionalMovementUpDown":{
                loadAverageDirectionalMovementUP_DOWN(key);
                break;
            }
            case "TrueRangeIndicator":{
                loadTrueRangeIndicator(key);
                break;
            }
            case "AroonUpDown":{
                loadAroonUP_DOWN(key);
                break;
            }
            case "Keltner":{
                loadKeltner(key);
                break;
            }

            case "LowerShadowIndicator":{
                loadLowerShadowIndicator(key);
                break;
            }
            case "MVWAPIndicator":{
                loadMVWAPIndicator(key);
                break;
            }
             case "RealBodyIndicator":{
                loadRealBodyIndicator(key);
                break;
            }
            case "UpperShadowIndicator":{
                loadUpperShadowIndicator(key);
                break;
            }
            case "TrailingStopLossIndicator":{
                loadTraillingStopLossIndicator(key);
                break;
            }
            case "TripleEMAIndicator":{
                loadTrippleEMAIndicator(key);
                break;
            }
           case "UlcerIndexIndicator":{
                loadUlcerIndexIndicator(key);
                break;
            }
            case "VWAPIndicator":{
                loadVWAPIndicator(key);
                break;
            }
            case "WMAIndicator":{
                loadWMAIndicator(key);
                break;
            }
            case "ZLEMAIndicator":{
                loadZLEMAIndicator(key);
                break;
            }
               case "RAVIIndicator":{
                loadRAVIIndicator(key);
                break;
            }
            case "ROCIndicator":{
                loadROCIndicator(key);
                break;
            }
            case "HMAIndicator":{
                loadHMAIndicator(key);
                break;
            }
            case "FisherIndicator":{
                loadFischerIndicator(key);
                break;
            }
            case "KAMAIndicator":{
                loadKAMAIndicator(key);
                break;
            }

            default:
                throw new IllegalArgumentException(key+ " could not be loaded!");
        }
    }

    /**
     *
     * @param key key of an indicator (instance)
     * @return the id of an indiator instance
     */
    private int getID(String key) {
        return Integer.parseInt(key.split("_")[1]);
    }

    /**
     *
     * @param key key of an indicator (instance)
     * @return the identifier of xml indicator
     */
    private String getIdentifier(String key){
        return key.split("_")[0];
    }

    //TODO: add more features in xml: lines, Based on indicator
    //TODO: add createRenderer function for several lines like itchimoku needs
    private XYLineAndShapeRenderer createRendere(TaColor color, TaStroke stroke, TaShape shape){
        boolean sh = true;

        if (shape.equals(TaShape.NONE)){
            sh = false;
        }

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true,sh);
        renderer.setSeriesStroke(0,stroke.getStroke());
        if(sh){
            renderer.setSeriesShape(0, shape.getShape());
        }
        renderer.setSeriesPaint(0, color.getPaint());

        return renderer;
    }


    private XYLineAndShapeRenderer createRendere(String key, String color, String shape, String stroke) throws XPathExpressionException {
        TaColor c = TaColor.valueOf(parameter.getParameter(key,color));
        TaStroke st = TaStroke.valueOf(parameter.getParameter(key,stroke));
        TaShape sh = TaShape.valueOf(parameter.getParameter(key,shape));
        return createRendere(c,st,sh);
    }

}
