package sma;

import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.*;
import loaders.CsvTradesLoader;
import strategies.MovingMomentumStrategy;


public class StrategyAnalysis {

    public static void main(String[] args) {


        TimeSeries series = CsvTradesLoader.loadBitstampSeries();

        Strategy strategy = MovingMomentumStrategy.buildStrategy(series);

        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);




        TotalProfitCriterion totalProfit = new TotalProfitCriterion();
        System.out.println("Total profit: " + totalProfit.calculate(series, tradingRecord));

        System.out.println("Number of bars: " + new NumberOfBarsCriterion().calculate(series, tradingRecord));

        System.out.println("Average profit (per bar): " + new AverageProfitCriterion().calculate(series, tradingRecord));

        System.out.println("Number of trades: " + new NumberOfTradesCriterion().calculate(series, tradingRecord));
 
        System.out.println("Profitable trades ratio: " + new AverageProfitableTradesCriterion().calculate(series, tradingRecord));

        System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));

        System.out.println("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(series, tradingRecord));

        System.out.println("Total transaction cost (from $1000): " + new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));

        System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));

        System.out.println("Custom strategy profit vs buy-and-hold strategy profit: " + new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));
    }
}
