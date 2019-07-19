package sma;

import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import loaders.CsvTradesLoader;

import java.time.ZonedDateTime;


public class TradingBotOnMovingTimeSeries {


    private static Num LAST_BAR_CLOSE_PRICE;


    private static TimeSeries initMovingTimeSeries(int maxBarCount) {
        TimeSeries series = CsvTradesLoader.loadBitstampSeries();
        System.out.print("Initial bar count: " + series.getBarCount());

        series.setMaximumBarCount(maxBarCount);
        LAST_BAR_CLOSE_PRICE = series.getBar(series.getEndIndex()).getClosePrice();
        System.out.println(" (limited to " + maxBarCount + "), close price = " + LAST_BAR_CLOSE_PRICE);
        return series;
    }


    private static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 12);


        Strategy buySellSignals = new BaseStrategy(
                new OverIndicatorRule(sma, closePrice),
                new UnderIndicatorRule(sma, closePrice)
        );
        return buySellSignals;
    }


    private static Num randDecimal(Num min, Num max) {
        Num randomDecimal = null;
        if (min != null && max != null && min.isLessThan(max)) {
            Num range = max.minus(min);
            Num position = range.multipliedBy(PrecisionNum.valueOf(Math.random()));
            randomDecimal = min.plus(position);
        }
        return randomDecimal;
    }


    private static Bar generateRandomBar() {
        final Num maxRange = PrecisionNum.valueOf("0.03"); // 3.0%
        Num openPrice = LAST_BAR_CLOSE_PRICE;
        Num minPrice = openPrice.minus(maxRange.multipliedBy(PrecisionNum.valueOf(Math.random())));
        Num maxPrice = openPrice.plus(maxRange.multipliedBy(PrecisionNum.valueOf(Math.random())));
        Num closePrice = randDecimal(minPrice, maxPrice);
        LAST_BAR_CLOSE_PRICE = closePrice;
        return new BaseBar(ZonedDateTime.now(), openPrice, maxPrice, minPrice, closePrice, PrecisionNum.valueOf(1), PrecisionNum.valueOf(1));
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("********************** Initialization **********************");

        TimeSeries series = initMovingTimeSeries(20);


        Strategy strategy = buildStrategy(series);


        TradingRecord tradingRecord = new BaseTradingRecord();
        System.out.println("************************************************************");


        for (int i = 0; i < 50; i++) {


            Thread.sleep(30); 
            Bar newBar = generateRandomBar();
            System.out.println("------------------------------------------------------\n"
                    + "Bar "+i+" added, close price = " + newBar.getClosePrice().doubleValue());
            series.addBar(newBar);

            int endIndex = series.getEndIndex();
            if (strategy.shouldEnter(endIndex)) {

                System.out.println("Strategy should ENTER on " + endIndex);
                boolean entered = tradingRecord.enter(endIndex, newBar.getClosePrice(), PrecisionNum.valueOf(10));
                if (entered) {
                    Order entry = tradingRecord.getLastEntry();
                    System.out.println("Entered on " + entry.getIndex()
                            + " (price=" + entry.getPrice().doubleValue()
                            + ", amount=" + entry.getAmount().doubleValue() + ")");
                }
            } else if (strategy.shouldExit(endIndex)) {

                System.out.println("Strategy should EXIT on " + endIndex);
                boolean exited = tradingRecord.exit(endIndex, newBar.getClosePrice(), PrecisionNum.valueOf(10));
                if (exited) {
                    Order exit = tradingRecord.getLastExit();
                    System.out.println("Exited on " + exit.getIndex()
                            + " (price=" + exit.getPrice().doubleValue()
                            + ", amount=" + exit.getAmount().doubleValue() + ")");
                }
            }
        }
    }
}
