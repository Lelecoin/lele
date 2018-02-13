/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.http.coinexchange;

import nxt.BlockchainTest;
import nxt.Tester;
import nxt.blockchain.ChildChain;
import nxt.http.APICall;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static nxt.blockchain.ChildChain.IDDR;
import static nxt.blockchain.ChildChain.SEMAR;

public class CoinExchangeTest extends BlockchainTest {

    @Test
    public void simpleExchange() {
        // Want to buy 25 IDDR with a maximum price of 4 SEMAR per IDDR
        // Convert the amount to SEMAR
        long displayIDDRAmount = 25;
        long quantityQNT = displayIDDRAmount * IDDR.ONE_COIN;
        long displayIgnisPerIDDRPrice = 4;
        long priceNQT = displayIgnisPerIDDRPrice * SEMAR.ONE_COIN;

        // Submit request to buy 25 IDDR with a maximum price of 4 SEMAR per IDDR
        // Quantity is denominated in IDDR and price is denominated in SEMAR per whole IDDR
        APICall apiCall = new APICall.Builder("exchangeCoins").
                secretPhrase(ALICE.getSecretPhrase()).
                param("feeRateNQTPerFXT", SEMAR.ONE_COIN).
                param("chain", SEMAR.getId()).
                param("exchange", IDDR.getId()).
                param("quantityQNT", quantityQNT).
                param("priceNQTPerCoin", priceNQT).
                build();
        JSONObject response = apiCall.invoke();
        Logger.logDebugMessage("exchangeCoins: " + response);
        generateBlock();

        JSONObject transactionJSON = (JSONObject)response.get("transactionJSON");
        String orderId = Tester.responseToStringId(transactionJSON);
        apiCall = new APICall.Builder("getCoinExchangeOrder").
                param("order", orderId).
                build();
        response = apiCall.invoke();
        Assert.assertEquals(Long.toString(25 * IDDR.ONE_COIN), response.get("quantityQNT"));
        Assert.assertEquals(Long.toString(4 * SEMAR.ONE_COIN), response.get("bidNQTPerCoin"));
        Assert.assertEquals(Long.toString((long)(1.0 / 4 * IDDR.ONE_COIN)), response.get("askNQTPerCoin"));

        // Want to buy 110 SEMAR with a maximum price of 1/4 IDDR per SEMAR
        // Quantity is denominated in SEMAR price is denominated in IDDR per whole SEMAR
        apiCall = new APICall.Builder("exchangeCoins").
                secretPhrase(BOB.getSecretPhrase()).
                param("feeRateNQTPerFXT", IDDR.ONE_COIN).
                param("chain", IDDR.getId()).
                param("exchange", SEMAR.getId()).
                param("quantityQNT", 100 * SEMAR.ONE_COIN + 10 * SEMAR.ONE_COIN).
                param("priceNQTPerCoin", IDDR.ONE_COIN / 4).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("exchangeCoins: " + response);
        generateBlock();

        transactionJSON = (JSONObject)response.get("transactionJSON");
        orderId = Tester.responseToStringId(transactionJSON);
        apiCall = new APICall.Builder("getCoinExchangeOrder").
                param("order", orderId).
                build();
        response = apiCall.invoke();
        Assert.assertEquals(Long.toString(10 * SEMAR.ONE_COIN), response.get("quantityQNT")); // leftover after the exchange of 100
        Assert.assertEquals(Long.toString((long) (0.25 * IDDR.ONE_COIN)), response.get("bidNQTPerCoin"));
        Assert.assertEquals(Long.toString(4 * SEMAR.ONE_COIN), response.get("askNQTPerCoin"));

        // Now look at the resulting trades
        apiCall = new APICall.Builder("getCoinExchangeTrades").
                param("chain", ChildChain.IDDR.getId()).
                param("account", BOB.getRsAccount()).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("GetCoinExchangeTrades: " + response);

        // Bob received 100 SEMAR and paid 0.25 IDDR per SEMAR
        JSONArray trades = (JSONArray) response.get("trades");
        JSONObject trade = (JSONObject) trades.get(0);
        Assert.assertEquals(IDDR.getId(), (int)(long)trade.get("chain"));
        Assert.assertEquals(SEMAR.getId(), (int)(long)trade.get("exchange"));
        Assert.assertEquals("" + (100 * SEMAR.ONE_COIN), trade.get("quantityQNT")); // SEMAR bought
        Assert.assertEquals("" + (long)(0.25 * IDDR.ONE_COIN), trade.get("priceNQTPerCoin")); // IDDR per SEMAR price

        apiCall = new APICall.Builder("getCoinExchangeTrades").
                param("chain", SEMAR.getId()).
                param("account", ALICE.getRsAccount()).
                build();
        response = apiCall.invoke();
        Logger.logDebugMessage("GetCoinExchangeTrades: " + response);

        // Alice received 25 IDDR and paid 4 SEMAR per IDDR
        trades = (JSONArray) response.get("trades");
        trade = (JSONObject) trades.get(0);
        Assert.assertEquals(SEMAR.getId(), (int)(long)trade.get("chain"));
        Assert.assertEquals(IDDR.getId(), (int)(long)trade.get("exchange"));
        Assert.assertEquals("" + (25 * IDDR.ONE_COIN), trade.get("quantityQNT")); // IDDR bought
        Assert.assertEquals("" + (4 * SEMAR.ONE_COIN), trade.get("priceNQTPerCoin")); // SEMAR per IDDR price

        Assert.assertEquals(-100 * SEMAR.ONE_COIN - SEMAR.ONE_COIN / 10, ALICE.getChainBalanceDiff(SEMAR.getId()));
        Assert.assertEquals(25 * IDDR.ONE_COIN, ALICE.getChainBalanceDiff(IDDR.getId()));
        Assert.assertEquals(100 * SEMAR.ONE_COIN, BOB.getChainBalanceDiff(SEMAR.getId()));
        Assert.assertEquals(-25 * IDDR.ONE_COIN - IDDR.ONE_COIN / 10, BOB.getChainBalanceDiff(IDDR.getId()));
    }

    @Test
    public void ronsSample() {
        long IDDRToBuy = 5 * IDDR.ONE_COIN;
        long ignisPerWholeIDDR = (long) (0.75 * SEMAR.ONE_COIN);

        APICall apiCall = new APICall.Builder("exchangeCoins").
                secretPhrase(ALICE.getSecretPhrase()).
                param("feeRateNQTPerFXT", SEMAR.ONE_COIN).
                param("chain", SEMAR.getId()).
                param("exchange", IDDR.getId()).
                param("quantityQNT", IDDRToBuy).
                param("priceNQTPerCoin", ignisPerWholeIDDR).
                build();
        JSONObject response = apiCall.invoke();
        String aliceOrder = Tester.responseToStringId(response);
        generateBlock();

        long ignisToBuy = 5 * SEMAR.ONE_COIN;
        long IDDRPerWholeIgnis = (long) (1.35 * IDDR.ONE_COIN);

        apiCall = new APICall.Builder("exchangeCoins").
                secretPhrase(BOB.getSecretPhrase()).
                param("feeRateNQTPerFXT", IDDR.ONE_COIN).
                param("chain", IDDR.getId()).
                param("exchange", SEMAR.getId()).
                param("quantityQNT", ignisToBuy).
                param("priceNQTPerCoin", IDDRPerWholeIgnis).
                build();
        response = apiCall.invoke();
        String bobOrder = Tester.responseToStringId(response);
        generateBlock();

        Assert.assertEquals((long)(-3.75 * SEMAR.ONE_COIN) - SEMAR.ONE_COIN / 10, ALICE.getChainBalanceDiff(SEMAR.getId()));
        Assert.assertEquals(5 * IDDR.ONE_COIN, ALICE.getChainBalanceDiff(IDDR.getId()));
        Assert.assertEquals((long)(3.75 * SEMAR.ONE_COIN), BOB.getChainBalanceDiff(SEMAR.getId()));
        Assert.assertEquals(-5 * IDDR.ONE_COIN - IDDR.ONE_COIN / 10, BOB.getChainBalanceDiff(IDDR.getId()));

        apiCall = new APICall.Builder("getCoinExchangeOrder").
                param("order", aliceOrder).
                build();
        response = apiCall.invoke();
        Assert.assertEquals(5L, response.get("errorCode"));

        apiCall = new APICall.Builder("getCoinExchangeOrder").
                param("order", bobOrder).
                build();
        response = apiCall.invoke();
        Assert.assertEquals((long)(1.25 * SEMAR.ONE_COIN), Long.parseLong((String) response.get("quantityQNT")));
        Assert.assertEquals((long)(1.35 * IDDR.ONE_COIN), Long.parseLong((String) response.get("bidNQTPerCoin")));
        Assert.assertEquals((long)(0.74074074 * SEMAR.ONE_COIN), Long.parseLong((String) response.get("askNQTPerCoin")));
    }

}
