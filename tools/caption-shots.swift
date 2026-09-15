#!/usr/bin/env swift
// Frames raw simulator screenshots into captioned App Store shots.
// CoreGraphics only, same as tools/gen-art.swift.
//
//   swift tools/caption-shots.swift <in.png> <out.png> "Headline" "Subline" [cropTopPx]
//
// cropTopPx trims the simulator status bar (and anything scrolled under it)
// off the top of the source shot before framing.
//
// Output is always 1320x2868 (iPhone 6.9"): teal gradient, caption on top,
// the screenshot inset below it with rounded corners.

import Foundation
import CoreGraphics
import CoreText
import ImageIO
import UniformTypeIdentifiers

let teal = CGColor(red: 0x0D / 255, green: 0x94 / 255, blue: 0x88 / 255, alpha: 1)
let deepTeal = CGColor(red: 0x0B / 255, green: 0x5C / 255, blue: 0x6B / 255, alpha: 1)
let white = CGColor(gray: 1, alpha: 1)
let sand = CGColor(red: 0xD7 / 255, green: 0xEE / 255, blue: 0xEC / 255, alpha: 1)
let space = CGColorSpaceCreateDeviceRGB()

let W = 1320, H = 2868
let inset: CGFloat = 100          // side margin around the screenshot
let capTop: CGFloat = 300         // headline baseline zone from the top
let shotWidth = CGFloat(W) - inset * 2

let args = CommandLine.arguments
guard args.count >= 4 else {
    FileHandle.standardError.write("usage: caption-shots.swift <in.png> <out.png> <headline> [subline]\n".data(using: .utf8)!)
    exit(2)
}
let (inPath, outPath, headline) = (args[1], args[2], args[3])
let subline = args.count > 4 ? args[4] : ""
let cropTop = args.count > 5 ? Int(args[5]) ?? 0 : 0

func font(_ name: String, _ size: CGFloat) -> CTFont { CTFontCreateWithName(name as CFString, size, nil) }

func line(_ s: String, _ f: CTFont, _ color: CGColor) -> CTLine {
    let attrs: [CFString: Any] = [kCTFontAttributeName: f, kCTForegroundColorAttributeName: color]
    return CTLineCreateWithAttributedString(CFAttributedStringCreate(nil, s as CFString, attrs as CFDictionary))
}

func width(_ s: String, _ f: CTFont) -> CGFloat { CGFloat(CTLineGetTypographicBounds(line(s, f, white), nil, nil, nil)) }

/// Greedy word wrap to `max` points.
func wrap(_ s: String, _ f: CTFont, max: CGFloat) -> [String] {
    var out: [String] = []
    var cur = ""
    for word in s.split(separator: " ") {
        let trial = cur.isEmpty ? String(word) : cur + " " + word
        if width(trial, f) <= max { cur = trial } else { if !cur.isEmpty { out.append(cur) }; cur = String(word) }
    }
    if !cur.isEmpty { out.append(cur) }
    return out
}

func draw(_ c: CGContext, _ s: String, _ f: CTFont, _ color: CGColor, centeredAt y: CGFloat) {
    let l = line(s, f, color)
    let w = CGFloat(CTLineGetTypographicBounds(l, nil, nil, nil))
    c.textPosition = CGPoint(x: (CGFloat(W) - w) / 2, y: y)
    CTLineDraw(l, c)
}

guard let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: inPath) as CFURL, nil),
      let loaded = CGImageSourceCreateImageAtIndex(src, 0, nil) else {
    FileHandle.standardError.write("cannot read \(inPath)\n".data(using: .utf8)!)
    exit(1)
}
let shot = cropTop > 0
    ? loaded.cropping(to: CGRect(x: 0, y: cropTop, width: loaded.width, height: loaded.height - cropTop))!
    : loaded

let c = CGContext(data: nil, width: W, height: H, bitsPerComponent: 8, bytesPerRow: 0,
                  space: space, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
c.setAllowsAntialiasing(true)

// Background gradient (top-left teal -> bottom-right deep teal).
let g = CGGradient(colorsSpace: space, colors: [teal, deepTeal] as CFArray, locations: [0, 1])!
c.drawLinearGradient(g, start: CGPoint(x: 0, y: H), end: CGPoint(x: W, y: 0), options: [])

// Caption. Headline wraps; subline sits under it.
let hFont = font("HelveticaNeue-Bold", 76)
let sFont = font("HelveticaNeue", 44)
let hLines = wrap(headline, hFont, max: shotWidth)
var y = CGFloat(H) - capTop + CGFloat(hLines.count - 1) * 92
for l in hLines { draw(c, l, hFont, white, centeredAt: y); y -= 92 }
if !subline.isEmpty {
    for l in wrap(subline, sFont, max: shotWidth) { draw(c, l, sFont, sand, centeredAt: y - 12); y -= 58 }
}

// Screenshot: scaled to the inset width, top-aligned under the caption, bottom
// cropped by the canvas edge if it runs long.
let scale = shotWidth / CGFloat(shot.width)
let shotH = CGFloat(shot.height) * scale
let top = y - 40
let rect = CGRect(x: inset, y: top - shotH, width: shotWidth, height: shotH)
c.saveGState()
c.addPath(CGPath(roundedRect: rect, cornerWidth: 56, cornerHeight: 56, transform: nil))
c.clip()
c.draw(shot, in: rect)
c.restoreGState()

let url = URL(fileURLWithPath: outPath)
try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, c.makeImage()!, nil)
CGImageDestinationFinalize(dest)
print("wrote \(outPath) (\(W)x\(H))")
