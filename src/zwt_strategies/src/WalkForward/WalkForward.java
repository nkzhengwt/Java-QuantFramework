package WalkForward;

import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.num.Num;
import loaders.CsvTradesLoader;
import strategies.CCICorrectionStrategy;
import strategies.GlobalExtremaStrategy;
import strategies.MovingMomentumStrategy;
import strategies.RSI2Strategy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalkForward {


    public static List<Integer> getSplitBeginIndexes(TimeSeries series, Duration splitDuration) {
        ArrayList<Integer> beginIndexes = new ArrayList<>();

        int beginIndex = series.getBeginIndex();
        int endIndex = series.getEndIndex();


        beginIndexes.add(beginIndex);


        ZonedDateTime beginInterval = series.getFirstBar().getEndTime();
        ZonedDateTime endInterval = beginInterval.plus(splitDuration);

        for (int i = beginIndex; i <= endIndex; i++) {

            ZonedDateTime barTime = series.getBar(i).getEndTime();
            if (barTime.isBefore(beginInterval) || !barTime.isBefore(endInterval)) {

                if (!endInterval.isAfter(barTime)) {

                    beginIndexes.add(i);
                }


                beginInterval = endInterval.isBefore(barTime) ? barTime : endInterval;
                endInterval = beginInterval.plus(splitDuration);
            }
        }
        return beginIndexes;
    }


    public static TimeSeries subseries(TimeSeries series, int beginIndex, Duration duration) {

        ZonedDateTime beginInterval = series.getBar(beginIndex).getEndTime();
        ZonedDateTime endInterval = beginInterval.plus(duration);


        int subseriesNbBars = 0;
        int endIndex = series.getEndIndex();
        for (int i = beginIndex; i <= endIndex; i++) {

            ZonedDateTime barTime = series.getBar(i).getEndTime();
            if (barTime.isBefore(beginInterval) || !barTime.isBefore(endInterval)) {

                break;
            }

            subseriesNbBars++;
        }

        return series.getSubSeries(beginIndex, beginIndex + subseriesNbBars);
    }


    public static List<TimeSeries> splitSeries(TimeSeries series, Duration splitDuration, Duration sliceDuration) {
        ArrayList<TimeSeries> subseries = new ArrayList<>();
        if (splitDuration != null && !splitDuration.isZero()
                && sliceDuration != null && !sliceDuration.isZero()) {

            List<Integer> beginIndexes = getSplitBeginIndexes(series, splitDuration);
            for (Integer subseriesBegin : beginIndexes) {
                subseries.add(subseries(series, subseriesBegin, sliceDuration));
            }
        }
        return subseries;
    }


    public static Map<Strategy, String> buildStrategiesMap(TimeSeries series) {
        HashMap<Strategy, String> strategies = new HashMap<>();
        strategies.put(CCICorrectionStrategy.buildStrategy(series), "CCI Correction");
        strategies.put(GlobalExtremaStrategy.buildStrategy(series), "Global Extrema");
        strategies.put(MovingMomentumStrategy.buildStrategy(series), "Moving Momentum");
        strategies.put(RSI2Strategy.buildStrategy(series), "RSI-2");
        return strategies;
    }

    public static void main(String[] args) {

        TimeSeries series = CsvTradesLoader.loadBitstampSeries();
        List<TimeSeries> subseries = splitSeries(series, Duration.ofHours(6), Duration.ofDays(7));


        Map<Strategy, String> strategies = buildStrategiesMap(series);


        AnalysisCriterion profitCriterion = new TotalProfitCriterion();

        for (TimeSeries slice : subseries) {

            System.out.println("Sub-series: " + slice.getSeriesPeriodDescription());
            TimeSeriesManager sliceManager = new TimeSeriesManager(slice);
            for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
                Strategy strategy = entry.getKey();
                String name = entry.getValue();
    
                TradingRecord tradingRecord = sliceManager.run(strategy);
                Num profit = profitCriterion.calculate(slice, tradingRecord);
                System.out.println("\tProfit for " + name + ": " + profit);
            }
            Strategy bestStrategy = profitCriterion.chooseBest(sliceManager, new ArrayList<Strategy>(strategies.keySet()));
            System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");
        }
    }

}
