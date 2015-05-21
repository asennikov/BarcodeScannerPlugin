#import "BarcodeScanner.h"
#import <MobileCoreServices/MobileCoreServices.h>

// parameters
#define kDevicePositionKey  @"cameraPosition"

@interface BarcodeScanner () {
    NSString *_captureCallbackId;
    UIView *_previewView;

    // parameters
    NSString *_cameraPosition;
}

@end

@implementation BarcodeScanner

- (CDVPlugin*) initWithWebView:(UIWebView*)theWebView
{
  self = (BarcodeScanner*)[super initWithWebView:theWebView];
  return self;
}

#pragma mark - Interfaces

- (void)startCapture:(CDVInvokedUrlCommand *)command
{
  CDVPluginResult *pluginResult = nil;
  _captureCallbackId = command.callbackId;

  _cameraPosition = @"front";

  // parse options
  if ([command.arguments count] > 0)
  {
      NSDictionary *jsonData = [command.arguments objectAtIndex:0];
      [self getOptions:jsonData];
  }
  NSLog(@"Device position is %@", _cameraPosition);

  // check already initialized
  if (_previewView) {
    if ([_cameraPosition isEqualToString:@"front"]) {
      self.capture.camera = self.capture.front;
    }
    else {
      self.capture.camera = self.capture.back;
    }

    [self.capture start];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_captureCallbackId];

    return;
  }

  _previewView = [[UIView alloc] initWithFrame:self.webView.superview.frame];

  self.capture = [[ZXCapture alloc] init];

  if ([_cameraPosition isEqualToString:@"front"]) {
    self.capture.camera = self.capture.front;
  }
  else {
    self.capture.camera = self.capture.back;
  }

  self.capture.focusMode = AVCaptureFocusModeContinuousAutoFocus;
  self.capture.rotation = 90.0f;

  self.capture.layer.frame = _previewView.bounds;
  [_previewView.layer addSublayer:self.capture.layer];
  [self.webView.superview insertSubview:_previewView belowSubview:self.webView];

  [self.webView setBackgroundColor:[UIColor clearColor]];
  self.webView.opaque = NO;

  self.capture.delegate = self;

  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [pluginResult setKeepCallbackAsBool:YES];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:_captureCallbackId];
}

- (void)stopCapture:(CDVInvokedUrlCommand *)command
{
  [self.capture stop];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

#pragma mark - ZXCaptureDelegate Methods

- (void)captureResult:(ZXCapture *)capture result:(ZXResult *)result {
  if (!result || !self.capture.running) return;

  [self.capture stop];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result.text];
  [pluginResult setKeepCallbackAsBool:YES];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:_captureCallbackId];

  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
    [self.capture start];
  });
}

#pragma mark - Utilities

- (void)getOptions: (NSDictionary *)jsonData
{
    if (![jsonData isKindOfClass:[NSDictionary class]])
        return;

    // device position
    NSString *obj = [jsonData objectForKey:kDevicePositionKey];

    if (obj != nil) {
      _cameraPosition = obj;
    }
}

@end
