var loader = require("./loader");
var config = loader.config;

loader.load(function(NRS) {
    const decimals = 2;
    var quantity = 2.5;
    var price = 1.3;
    var data = {
        asset: "6146869985881051310",
        quantityQNT: NRS.convertToQNT(quantity, decimals),
        priceNQTPerShare: NRS.calculatePricePerWholeQNT(NRS.convertToNQT(price), decimals),
        secretPhrase: config.secretPhrase,
        chain: config.chain
    };
    data = Object.assign(
        data,
        NRS.getMandatoryParams()
    );
    NRS.sendRequest("placeAskOrder", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
