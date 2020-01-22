package cat.indiketa.degiro.utils;

import cat.indiketa.degiro.log.DLog;
import cat.indiketa.degiro.model.*;
import cat.indiketa.degiro.model.DCashFunds.DCashFund;
import cat.indiketa.degiro.model.DLastTransactions.DTransaction;
import cat.indiketa.degiro.model.DPortfolioProducts.DPortfolioProduct;
import cat.indiketa.degiro.model.raw.*;
import cat.indiketa.degiro.model.raw.DRawPortfolio.Value;
import cat.indiketa.degiro.model.raw.DFieldValue;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author indiketa
 */
public class DUtils {

    private static final SimpleDateFormat HM_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_FORMAT2 = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static DPortfolioProducts convert(DRawPortfolio rawPortfolio, String currency) {
        DPortfolioProducts portfolio = new DPortfolioProducts();
        final DRawPortfolio.Portfolio portfolio1 = rawPortfolio.getPortfolio();
        portfolio.setLastUpdate(portfolio1.getLastUpdated());
        for (Value value : portfolio1.getValue()) {
            if(value.getIsRemoved()) {
                portfolio.getRemoved().add(value.getId());
            }else {
                DPortfolioProduct productRow = convertProduct(value, currency);
                if(value.getIsAdded()) {
                    portfolio.getAdded().add(productRow);
                }else{
                    portfolio.getUpdates().add(productRow);
                }
                if(productRow.getId() == null) {
                    productRow.setId(value.getId());
                }
            }
        }
        return portfolio;

    }

    public static DPortfolioSummaryUpdate convertPortfolioSummary(DRawPortfolioSummary.TotalPortfolio row) {
        final DPortfolioSummaryUpdate update = new DPortfolioSummaryUpdate();
        update.setLastUpdated(row.getLastUpdated());
        if(row.getValue() == null || row.getValue().isEmpty()) {
            return update;
        }
        update.setAdded(row.getIsAdded());
        DPortfolioSummary portfolioSummary = new DPortfolioSummary();

        for (DFieldValue value : row.getValue()) {

            try {

                String methodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, value.getName());

                switch (value.getName()) {
                    case "cashFundCompensationCurrency":
                        final String value1 = (String) value.getValue();
                        DPortfolioSummary.class.getMethod(methodName, String.class).invoke(portfolioSummary, value1);
                        break;
                    case "freeSpaceNew":
                        final HashMap<String, BigDecimal> freeSpaceNew = new HashMap<>();
                        for (Map.Entry<String, Double> stringDoubleEntry : ((Map<String, Double>) value.getValue())
                                .entrySet()) {
                            freeSpaceNew.put(stringDoubleEntry.getKey(), getBigDecimal(stringDoubleEntry.getValue()));
                        }
                        portfolioSummary.setFreeSpaceNew(freeSpaceNew);
                        break;
                    case "cash":
                    case "cashFundCompensation":
                    case "cashFundCompensationWithdrawn":
                    case "cashFundCompensationPending":
                    case "todayNonProductFees":
                    case "totalNonProductFees":
                        BigDecimal bdValue = getBigDecimal(value.getValue());
                        DPortfolioSummary.class.getMethod(methodName, BigDecimal.class).invoke(portfolioSummary, bdValue);
                        break;

                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DLog.DEGIRO.error("Error while setting value of portfolioSummary." + value.getName(), e);
            }
        }
        update.setPortfolioSummary(portfolioSummary);

        return update;
    }

    private static BigDecimal getBigDecimal(Object value1) {
        BigDecimal bdValue = new BigDecimal((double) value1);
        if (bdValue.scale() > 4) {
            bdValue = bdValue.setScale(4, RoundingMode.HALF_UP);
        }
        return bdValue;
    }

    public static DPortfolioProduct convertProduct(Value row, String currency) {
        DPortfolioProduct productRow = new DPortfolioProduct(currency);

        for (DFieldValue value : row.getValue()) {

            try {
                String methodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, value.getName());

                switch (value.getName()) {
                    case "size":
                    case "averageFxRate":
                        Long longValue = Double.valueOf(value.getValue().toString()).longValue();
                        DPortfolioProduct.class.getMethod(methodName, Long.class).invoke(productRow, longValue);
                        break;
                    case "id":
                    case "positionType":
                    case "accruedInteres":
                        String stringValue = (String) value.getValue();
                        DPortfolioProduct.class.getMethod(methodName, String.class).invoke(productRow, stringValue);
                        break;
                    case "lastUpdate":
                        break;
                    case "price":
                    case "value":
                    case "breakEvenPrice":
                    case "portfolioValueCorrection":
                    case "realizedProductPl":
                    case "realizedFxPl":
                    case "todayRealizedProductPl":
                    case "todayRealizedFxPl":
                        BigDecimal bdValue = new BigDecimal((double) value.getValue());
                        if (!value.getName().equals("change") && bdValue.scale() > 4) {
                            bdValue = bdValue.setScale(4, RoundingMode.HALF_UP);
                        }
                        DPortfolioProduct.class.getMethod(methodName, BigDecimal.class).invoke(productRow, bdValue);
                        break;
                    case "todayPlBase":
                    case "plBase":
                        Map values = (Map) value.getValue();
                        DPortfolioProduct.class.getMethod(methodName, BigDecimal.class).invoke(productRow, new BigDecimal(values.get(currency).toString()));
                        break;

                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DLog.DEGIRO.error("Error while setting value of portfolio", e);
            }
        }

        return productRow;
    }

    public static DCashFunds convert(DRawCashFunds rawCashFunds) {
        DCashFunds cashFunds = new DCashFunds();
        List<DCashFund> list = new LinkedList<>();

        for (Value value : rawCashFunds.getCashFunds().getValue()) {
            DCashFund cashFund = convertCash(value);
            list.add(cashFund);
        }

        cashFunds.setCashFunds(list);

        return cashFunds;

    }

    public static DCashFund convertCash(Value row) {

        DCashFund cashFund = new DCashFund();

        for (DFieldValue value : row.getValue()) {

            try {

                String methodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, value.getName());

                switch (value.getName()) {
                    case "id":
                        int intValue = (int) (double) value.getValue();
                        DCashFund.class.getMethod(methodName, int.class).invoke(cashFund, intValue);
                        break;
                    case "currencyCode":
                        String stringValue = (String) value.getValue();
                        DCashFund.class.getMethod(methodName, String.class).invoke(cashFund, stringValue);
                        break;
                    case "value":
                        BigDecimal bdValue = getBigDecimal(value.getValue());
                        DCashFund.class.getMethod(methodName, BigDecimal.class).invoke(cashFund, bdValue);
                        break;

                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DLog.DEGIRO.error("Error while setting value of cash fund", e);
            }

        }
        return cashFund;
    }

    public static DOrders convert(DRawOrders rawOrders) {
        DOrders orders = new DOrders();
        orders.setLastUpdate(rawOrders.orders.lastUpdated);

        for (DRawOrders.Value value : rawOrders.getOrders().getValue()) {
            if (value.getIsRemoved()) {
                orders.getRemoved().add(value.getId());
            } else {
                DOrder order = convertOrder(value);
                if (value.getIsAdded()) {
                    orders.getAdded().add(order);
                }else{
                    orders.getUpdates().add(order);
                }
                if(order.getId() == null) {
                    order.setId(value.getId());
                }
            }
        }
        return orders;

    }

    public static DOrder convertOrder(DRawOrders.Value row) {

        DOrder order = new DOrder();
        for (DFieldValue value : row.getValue()) {

            try {

                String methodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, value.getName());

                switch (value.getName()) {
                    case "contractType":
                    case "contractSize":
                        Integer intValue = (int) (double) value.getValue();
                        DOrder.class.getMethod(methodName, Integer.class).invoke(order, intValue);
                        break;
                    case "productId":
                    case "size":
                    case "quantity":
                        Long longValue = (long) (double) value.getValue();
                        DOrder.class.getMethod(methodName, Long.class).invoke(order, longValue);
                        break;
                    case "id":
                    case "product":
                    case "currency":
                        String stringValue = (String) value.getValue();
                        DOrder.class.getMethod(methodName, String.class).invoke(order, stringValue);
                        break;
                    case "buysell":
                        String stringValue2 = (String) value.getValue();
                        order.setBuySell(DOrderAction.getOrderByValue(stringValue2));
                        break;

                    case "date":
                        Calendar calendar = processDate((String) value.getValue());
                        DOrder.class.getMethod(methodName, Calendar.class).invoke(order, calendar);
                        break;
                    case "orderTypeId":
                        order.setOrderType(DOrderType.getOrderByValue((int) (double) value.getValue()));
                        break;
                    case "orderTimeTypeId":
                        order.setOrderTime(DOrderTime.getOrderByValue((int) (double) value.getValue()));
                        break;
                    case "price":
                    case "stopPrice":
                    case "totalOrderValue":
                        BigDecimal bdValue = getBigDecimal(value.getValue());
                        DOrder.class.getMethod(methodName, BigDecimal.class).invoke(order, bdValue);
                        break;

                    case "isModifiable":
                    case "isDeletable":
                        Boolean booleanValue = (Boolean) value.getValue();
                        DOrder.class.getMethod(methodName, boolean.class).invoke(order, booleanValue);
                        break;

                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DLog.DEGIRO.error("Error while setting value of order", e);
            }

        }
        return order;
    }

    private static Calendar processDate(String date) {
        Calendar parsed = null;

        date = Strings.nullToEmpty(date);

        if (date.contains(":")) {
            parsed = Calendar.getInstance();
            parsed.set(Calendar.HOUR_OF_DAY, Integer.parseInt(date.split(":")[0]));
            parsed.set(Calendar.MINUTE, Integer.parseInt(date.split(":")[1]));
            parsed.set(Calendar.SECOND, 0);
            parsed.set(Calendar.MILLISECOND, 0);
        } else if (date.contains("/")) {
            parsed = Calendar.getInstance();
            int month = Integer.parseInt(date.split("/")[1]) - 1;

            if (parsed.get(Calendar.MONTH) < month) {
                parsed.add(Calendar.YEAR, -1);
            }

            parsed.set(Calendar.MONTH, month);
            parsed.set(Calendar.DATE, Integer.parseInt(date.split("/")[0]));
            parsed.set(Calendar.HOUR_OF_DAY, 0);
            parsed.set(Calendar.MINUTE, 0);
            parsed.set(Calendar.SECOND, 0);
            parsed.set(Calendar.MILLISECOND, 0);

        } else {
        }
        return parsed;
    }

    public static DLastTransactions convert(DRawTransactions rawOrders) {
        DLastTransactions transactions = new DLastTransactions();
        List<DTransaction> list = new LinkedList<>();

        for (Value value : rawOrders.getTransactions().getValue()) {
            DTransaction order = convertTransaction(value);
            list.add(order);
        }

        transactions.setTransactions(list);

        return transactions;

    }

    public static DTransaction convertTransaction(Value row) {

        DTransaction transaction = new DTransaction();

        for (DFieldValue value : row.getValue()) {

            try {

                String methodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, value.getName());

                switch (value.getName()) {
                    case "contractType":
                    case "contractSize":
                        int intValue = (int) (double) value.getValue();
                        DTransaction.class.getMethod(methodName, int.class).invoke(transaction, intValue);
                        break;
                    case "productId":
                    case "size":
                    case "quantity":
                    case "id":
                        long longValue = (long) (double) value.getValue();
                        DTransaction.class.getMethod(methodName, long.class).invoke(transaction, longValue);
                        break;
                    case "product":
                    case "currency":
                        String stringValue = (String) value.getValue();
                        DTransaction.class.getMethod(methodName, String.class).invoke(transaction, stringValue);
                        break;
                    case "buysell":
                        String stringValue2 = (String) value.getValue();
                        transaction.setBuysell(DOrderAction.getOrderByValue(stringValue2));
                        break;

                    case "date":
                        Calendar calendar = processDate((String) value.getValue());
                        DTransaction.class.getMethod(methodName, Calendar.class).invoke(transaction, calendar);
                        break;
                    case "orderType":
                        transaction.setOrderType(DOrderType.getOrderByValue((int) (double) value.getValue()));
                        break;
                    case "orderTime":
                        transaction.setOrderTime(DOrderTime.getOrderByValue((int) (double) value.getValue()));
                        break;
                    case "price":
                    case "stopPrice":
                    case "totalOrderValue":
                        BigDecimal bdValue = getBigDecimal(value.getValue());
                        DTransaction.class.getMethod(methodName, BigDecimal.class).invoke(transaction, bdValue);
                        break;

                    case "isModifiable":
                    case "isDeletable":
                        Boolean booleanValue = (Boolean) value.getValue();
                        DTransaction.class.getMethod(methodName, boolean.class).invoke(transaction, booleanValue);
                        break;

                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DLog.DEGIRO.error("Error while setting value of order", e);
            }

        }
        return transaction;
    }

    public static List<DPrice> convert(List<DRawVwdPrice> data) {

        Set<String> issues = new HashSet<>(100);
        Map<String, String> dataMap = new HashMap<>(data.size());

        if (data != null) {
            for (DRawVwdPrice dRawVwdPrice : data) {

                if (Strings.nullToEmpty(dRawVwdPrice.getM()).equals("a_req")) {
                    String firstVal = dRawVwdPrice.getV().get(0);
                    issues.add(firstVal.substring(0, firstVal.lastIndexOf(".")));

                }

                if (dRawVwdPrice.getV() != null && !dRawVwdPrice.getV().isEmpty()) {
                    String v2 = null;

                    if (dRawVwdPrice.getV().size() > 1) {
                        v2 = dRawVwdPrice.getV().get(1);
                    }

                    dataMap.put(dRawVwdPrice.getV().get(0), v2);
                }
            }
        }

        List<DPrice> prices = new ArrayList<>(issues.size());

        for (String issue : issues) {
            DPrice price = new DPrice();
            price.setIssueId(issue);
            price.setBid(Doubles.tryParse(getData(issue, "BidPrice", dataMap)));
            price.setAsk(Doubles.tryParse(getData(issue, "AskPrice", dataMap)));
            price.setLast(Doubles.tryParse(getData(issue, "LastPrice", dataMap)));
            String df = getData(issue, "LastTime", dataMap);
            if (!Strings.isNullOrEmpty(df)) {
                try {
                    price.setLastTime(HM_FORMAT.parse(df));
                    Date d = new Date();
                    price.getLastTime().setDate(1);
                    price.getLastTime().setYear(d.getYear());
                    price.getLastTime().setMonth(d.getMonth());
                    price.getLastTime().setDate(d.getDate());
                    if (price.getLastTime().getTime() > System.currentTimeMillis()) {
                        price.getLastTime().setTime(price.getLastTime().getTime() - 1 * 24 * 60 * 60 * 1000);
                    }
                } catch (ParseException e) {
                    DLog.DEGIRO.warn("Exception parsing lastTime: " + df + " from issue " + issue);
                }
            }

            prices.add(price);
        }

        return prices;

    }

    private static String getData(String issue, String name, Map<String, String> dataMap) {

        String retVal = "";

        if (dataMap.containsKey(issue + "." + name)) {
            retVal = Strings.nullToEmpty(dataMap.get(dataMap.get(issue + "." + name)));
        }

        return retVal;

    }

    public static class ProductTypeAdapter extends TypeAdapter<DProductType> {

        @Override
        public DProductType read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            int value = reader.nextInt();

            return DProductType.getProductTypeByValue(value);

        }

        @Override
        public void write(JsonWriter writer, DProductType value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }

            writer.value(value.getTypeCode());
        }
    }

    public static class BigDecimalTypeAdapter extends TypeAdapter<BigDecimal> {

        @Override
        public BigDecimal read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            String value = reader.nextString();

            BigDecimal bd = null;

            if (!Strings.isNullOrEmpty(value)) {
                bd = new BigDecimal(value);
            }

            return bd;

        }

        @Override
        public void write(JsonWriter writer, BigDecimal value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }

            writer.value(value.toPlainString());
        }
    }

    public static class OrderTimeTypeAdapter extends TypeAdapter<DOrderTime> {

        @Override
        public DOrderTime read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            String value = reader.nextString();

            return DOrderTime.getOrderByValue(value);

        }

        @Override
        public void write(JsonWriter writer, DOrderTime value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }

            writer.value(value.getStrValue());
        }
    }

    public static class OrderTypeTypeAdapter extends TypeAdapter<DOrderType> {

        @Override
        public DOrderType read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            String value = reader.nextString();

            return DOrderType.getOrderByValue(value);

        }

        @Override
        public void write(JsonWriter writer, DOrderType value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }

            writer.value(value.getStrValue());
        }
    }

    public static class OrderActionTypeAdapter extends TypeAdapter<DOrderAction> {

        @Override
        public DOrderAction read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            String value = reader.nextString();

            return DOrderAction.getOrderByValue(value);

        }

        @Override
        public void write(JsonWriter writer, DOrderAction value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }

            writer.value(value.getStrValue());
        }
    }

    public static class DateTypeAdapter extends TypeAdapter<Date> {

        @Override
        public Date read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            String value = reader.nextString();

            Date d = null;
            try {
                d = DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Date not parseable: " + value, e);
            }
            return d;
        }

        @Override
        public void write(JsonWriter writer, Date value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }

            writer.value(DATE_FORMAT.format(value));
        }
    }

    public static boolean isNumeric(String strNum) {
        return strNum.matches("\\d+");
    }

    public static class CalendarTypeAdapter extends TypeAdapter<Calendar> {
        @Override
        public void write(JsonWriter writer, Calendar value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(DATE_TIME_FORMAT.format(value));
        }

        @Override
        public Calendar read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            String value = reader.nextString();

            Calendar d = null;
            try {
                final Date parse = DATE_TIME_FORMAT.parse(value);
                final Calendar instance = Calendar.getInstance();
                instance.setTime(parse);
                return instance;
            } catch (ParseException e) {
                DLog.DEGIRO.warn("DateTime not parseable: " + value);
            }
            return d;
        }
    }
}
