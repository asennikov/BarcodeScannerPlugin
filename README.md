BarcodeScannerPlugin
============================

Cordova barcode scanner plugin for iOS/Android, supports camera preview and continuos scanning.

### Plugin's Purpose
The purpose of the plugin is to continuously scan for barcodes and
show camera preview under main Cordova webView.

## Supported Platforms
- **iOS**<br>
- **Android**<br>

### Adding the Plugin to your project
Through the [Command-line Interface][CLI]:
```bash
# ~~ from master ~~
cordova plugin add https://github.com/asennikov/BarcodeScannerPlugin.git && cordova prepare
```

### Removing the Plugin from your project
Through the [Command-line Interface][CLI]:
```bash
cordova plugin rm com.sandyclock.plugins.BarcodeScanner
```

## Using the plugin
The plugin creates the object ```window.plugins.barcodeScanner``` with the following methods:

### Plugin initialization
The plugin and its methods are not available before the *deviceready* event has been fired.

### start
Start capture process. `captureCallback` function will be called with decoded string data at each time when the plugin successfully decode barcode.<br>

```javascript
window.barcodeScanner.start(captureCallback, options);
```

Available options:

- cameraPosition ('front' / 'back').

#### Example
```javascript
function onStart() {
    var options = {
        cameraPosition: 'front'
    };
    window.plugins.barcodeScanner.start(onSucess, options);
}

function onSucess(data) {
    window.alert(data);
}
```

### stop
Stop capture process.

```javascript
window.barcodeScanner.stop();
```

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
