package cx.viz.balisurf.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.UIKit.UIDevice

internal actual val userAgent: String by lazy {
    val os = UIDevice.currentDevice.systemVersion.replace('.', '_')
    val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0"
    "Mozilla/5.0 (iPhone; CPU iPhone OS $os like Mac OS X) BaliSurf/$version"
}

internal actual fun postJson(url: String, body: String, userAgent: String) {
    val request = NSMutableURLRequest(uRL = NSURL(string = url))
    request.setHTTPMethod("POST")
    request.setTimeoutInterval(5.0)
    request.setValue("application/json", forHTTPHeaderField = "Content-Type")
    request.setValue(userAgent, forHTTPHeaderField = "User-Agent")
    request.setHTTPBody((body as NSString).dataUsingEncoding(NSUTF8StringEncoding))
    NSURLSession.sharedSession.dataTaskWithRequest(request) { _, _, _ -> }.resume()
}
