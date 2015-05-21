#import <Cordova/CDVPlugin.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

#import "ZXingObjC.h"

@interface BarcodeScanner : CDVPlugin <ZXCaptureDelegate>

@property (nonatomic, strong) ZXCapture *capture;

- (void)startCapture:(CDVInvokedUrlCommand *)command;
- (void)stopCapture:(CDVInvokedUrlCommand *)command;

@end
