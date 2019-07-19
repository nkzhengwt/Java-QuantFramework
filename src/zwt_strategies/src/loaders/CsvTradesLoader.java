package loaders;

import com.opencsv.CSVReader;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CsvTradesLoader {

    
//    加载比特币行情序列
    public static TimeSeries loadBitstampSeries() {

//        从CSV文件中读取所有行
        InputStream stream = CsvTradesLoader.class.getClassLoader().getResourceAsStream("bitstamp_trades_from_20131125_usd.csv");
        CSVReader csvReader = null;
        List<String[]> lines = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',');
            lines = csvReader.readAll();
            lines.remove(0); // Removing header line
        } catch (IOException ioe) {
            Logger.getLogger(CsvTradesLoader.class.getName()).log(Level.SEVERE, "Unable to load trades from CSV", ioe);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        TimeSeries series = new BaseTimeSeries();
        if ((lines != null) && !lines.isEmpty()) {

//            得到第一个和最后一个时间戳
            ZonedDateTime beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lines.get(0)[0]) * 1000), ZoneId.systemDefault());
            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lines.get(lines.size() - 1)[0]) * 1000), ZoneId.systemDefault());
            if (beginTime.isAfter(endTime)) {
                Instant beginInstant = beginTime.toInstant();
                Instant endInstant = endTime.toInstant();
                beginTime = ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault());
                endTime = ZonedDateTime.ofInstant(beginInstant, ZoneId.systemDefault());
                Collections.reverse(lines);
            }
//            构造一串bar
           	buildSeries(series,beginTime, endTime, 300, lines);
        }

        return series;
    }

    /**
     * 构造一串bar
     * @param beginTime 回测时期的起始时间
     * @param endTime 回测时期的结束时间
     * @param duration bar的持续时间周期（秒）
     * @param lines 从csv读到的数据
     */
    private static void buildSeries(TimeSeries series, ZonedDateTime beginTime, ZonedDateTime endTime, int duration, List<String[]> lines) {


    	Duration barDuration = Duration.ofSeconds(duration);
    	ZonedDateTime barEndTime = beginTime;
    	int i = 0;
    	do {
//    		构造一个bar
    		barEndTime = barEndTime.plus(barDuration);
    		Bar bar = new BaseBar(barDuration, barEndTime, series.function());
    		do {
//                        得到一次交易
    			String[] tradeLine = lines.get(i);
    			ZonedDateTime tradeTimeStamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(tradeLine[0]) * 1000), ZoneId.systemDefault());
//                        如果交易发生在一根bar内
    			if (bar.inPeriod(tradeTimeStamp)) {
    				// add the trade to the bar
//                                将这次交易加入到这根bar中
    				double tradePrice = Double.parseDouble(tradeLine[1]);
    				double tradeVolume = Double.parseDouble(tradeLine[2]);
                                bar.addTrade(tradeVolume, tradePrice, series.function());
    			} else {
//                            如果交易发生在一根bar之后，则移到下根bar中
    				break;
    			}
    			i++;
    		} while (i < lines.size());
    		if (bar.getTrades() > 0) {
    			series.addBar(bar);
    		}
    	} while (barEndTime.isBefore(endTime));
    }

    public static void main(String[] args) {
        TimeSeries series = CsvTradesLoader.loadBitstampSeries();

        System.out.println("Series: " + series.getName() + " (" + series.getSeriesPeriodDescription() + ")");
        System.out.println("Number of bars: " + series.getBarCount());
        System.out.println("First bar: \n"
                + "\tVolume: " + series.getBar(0).getVolume() + "\n"
                + "\tNumber of trades: " + series.getBar(0).getTrades() + "\n"
                + "\tClose price: " + series.getBar(0).getClosePrice());
    }
}