var exec = require('cordova/exec');

module.exports = {
    start: function(captureCallback, options) {
        var callbackWrapper = function(data) {
            if (!!data) {
                captureCallback(data);
            }
        };

        console.log(options);
        cordova.exec(callbackWrapper, false, "BarcodeScanner", "startCapture", [options]);
    },

    stop: function() {
        cordova.exec(false, false, "BarcodeScanner", "stopCapture", []);
    }
};
