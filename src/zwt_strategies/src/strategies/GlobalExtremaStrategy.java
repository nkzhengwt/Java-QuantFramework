package strategies;

import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import loaders.CsvTradesLoader;


public class GlobalExtremaStrategy {

 
    private static final int NB_BARS_PER_WEEK = 12 * 24 * 7;


    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);


        MaxPriceIndicator maxPrices = new MaxPriceIndicator(series);
        HighestValueIndicator weekMaxPrice = new HighestValueIndicator(maxPrices, NB_BARS_PER_WEEK);

        MinPriceIndicator minPrices = new MinPriceIndicator(series);
        LowestValueIndicator weekMinPrice = new LowestValueIndicator(minPrices, NB_BARS_PER_WEEK);

 
        MultiplierIndicator downWeek = new MultiplierIndicator(weekMinPrice, 1.004);
        Rule buyingRule = new UnderIndicatorRule(closePrices, downWeek);


        MultiplierIndicator upWeek = new MultiplierIndicator(weekMaxPrice, 0.996);
        Rule sellingRule = new OverIndicatorRule(closePrices, upWeek);

        return new BaseStrategy(buyingRule, sellingRule);
    }

    public static void main(String[] args) {


        TimeSeries series = CsvTradesLoader.loadBitstampSeries();


        Strategy strategy = buildStrategy(series);


        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());
        System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
    }
}
