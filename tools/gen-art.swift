#!/usr/bin/env swift
// Renders every raster store/launcher asset from the Tropic Clean palette.
// CoreGraphics only — no ImageMagick/PIL on this machine, and none needed.
//
//   swift tools/gen-art.swift
//
// Outputs: androidMain mipmaps (adaptive foreground + legacy + round),
// store-assets/icons/play-icon-512.png, store-assets/feature-graphic/*.png.
// Re-run after a palette change; the mark is code, not a binary to hand-edit.

import Foundation
import CoreGraphics
import CoreText
import ImageIO
import UniformTypeIdentifiers

// Tropic Clean, mirrored from ui/Theme.kt BaliColors.
let teal = CGColor(red: 0x0D / 255, green: 0x94 / 255, blue: 0x88 / 255, alpha: 1)
let deepTeal = CGColor(red: 0x0B / 255, green: 0x5C / 255, blue: 0x6B / 255, alpha: 1)
let white = CGColor(gray: 1, alpha: 1)
let sand = CGColor(red: 0xF7 / 255, green: 0xFA / 255, blue: 0xFB / 255, alpha: 1)

let space = CGColorSpaceCreateDeviceRGB()

func ctx(_ w: Int, _ h: Int) -> CGContext {
    let c = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0,
                      space: space, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    c.setAllowsAntialiasing(true)
    return c
}

func write(_ c: CGContext, _ path: String) {
    let url = URL(fileURLWithPath: path)
    try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(),
                                             withIntermediateDirectories: true)
    let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, c.makeImage()!, nil)
    CGImageDestinationFinalize(dest)
    print("wrote \(path) (\(c.width)x\(c.height))")
}

func fillGradient(_ c: CGContext, _ rect: CGRect) {
    let g = CGGradient(colorsSpace: space, colors: [teal, deepTeal] as CFArray, locations: [0, 1])!
    c.saveGState()
    c.clip(to: rect)
    c.drawLinearGradient(g, start: CGPoint(x: rect.minX, y: rect.maxY),
                         end: CGPoint(x: rect.maxX, y: rect.minY), options: [])
    c.restoreGState()
}

/// The mark: three stacked swell lines, the crest breaking into a curl.
/// Drawn in a unit square so every output size is one scale factor away.
func drawWaves(_ c: CGContext, size: CGFloat, origin: CGPoint = .zero, color: CGColor) {
    let s = size
    c.saveGState()
    c.translateBy(x: origin.x, y: origin.y)
    c.setStrokeColor(color)
    c.setLineCap(.round)

    // Curl: an open crescent, the readable part of the mark at 48px.
    c.setLineWidth(s * 0.115)
    let curl = CGMutablePath()
    curl.move(to: CGPoint(x: s * 0.14, y: s * 0.46))
    curl.addCurve(to: CGPoint(x: s * 0.86, y: s * 0.58),
                  control1: CGPoint(x: s * 0.34, y: s * 0.94),
                  control2: CGPoint(x: s * 0.74, y: s * 0.96))
    curl.addCurve(to: CGPoint(x: s * 0.52, y: s * 0.50),
                  control1: CGPoint(x: s * 0.94, y: s * 0.30),
                  control2: CGPoint(x: s * 0.64, y: s * 0.30))
    c.addPath(curl)
    c.strokePath()

    // Two swell lines underneath, shorter and thinner as they recede.
    c.setLineWidth(s * 0.075)
    for (i, y) in [0.30, 0.17].enumerated() {
        let inset = s * (0.16 + CGFloat(i) * 0.10)
        let p = CGMutablePath()
        p.move(to: CGPoint(x: inset, y: s * CGFloat(y)))
        p.addCurve(to: CGPoint(x: s - inset, y: s * CGFloat(y)),
                   control1: CGPoint(x: s * 0.36, y: s * CGFloat(y) + s * 0.11),
                   control2: CGPoint(x: s * 0.64, y: s * CGFloat(y) - s * 0.11))
        c.addPath(p)
        c.strokePath()
    }
    c.restoreGState()
}

func roundedIcon(_ px: Int, circular: Bool) -> CGContext {
    let c = ctx(px, px)
    let s = CGFloat(px)
    let rect = CGRect(x: 0, y: 0, width: s, height: s)
    c.saveGState()
    let clip = circular ? CGPath(ellipseIn: rect, transform: nil)
                        : CGPath(roundedRect: rect, cornerWidth: s * 0.22, cornerHeight: s * 0.22, transform: nil)
    c.addPath(clip)
    c.clip()
    fillGradient(c, rect)
    drawWaves(c, size: s * 0.72, origin: CGPoint(x: s * 0.14, y: s * 0.14), color: white)
    c.restoreGState()
    return c
}

/// Adaptive foreground: transparent, mark confined to the inner 66% safe zone.
func adaptiveForeground(_ px: Int) -> CGContext {
    let c = ctx(px, px)
    let s = CGFloat(px)
    drawWaves(c, size: s * 0.56, origin: CGPoint(x: s * 0.22, y: s * 0.22), color: white)
    return c
}

func text(_ c: CGContext, _ string: String, font: String, size: CGFloat, color: CGColor, at p: CGPoint) {
    // Raw CoreText keys: the NSAttributedString.Key constants live in AppKit,
    // which this script deliberately does not link.
    let f = CTFontCreateWithName(font as CFString, size, nil)
    let attrs: [CFString: Any] = [
        kCTFontAttributeName: f,
        kCTForegroundColorAttributeName: color,
        kCTKernAttributeName: size * 0.01,
    ]
    let line = CTLineCreateWithAttributedString(
        CFAttributedStringCreate(nil, string as CFString, attrs as CFDictionary))
    c.textPosition = p
    CTLineDraw(line, c)
}

func featureGraphic() -> CGContext {
    let c = ctx(1024, 500)
    let rect = CGRect(x: 0, y: 0, width: 1024, height: 500)
    fillGradient(c, rect)
    // Mark on the left, wordmark on the right — safe against Play's center crop
    // on small layouts, which keeps roughly the middle 924px.
    drawWaves(c, size: 300, origin: CGPoint(x: 78, y: 100), color: white)
    text(c, "BaliSurf", font: "HelveticaNeue-Bold", size: 108, color: white, at: CGPoint(x: 430, y: 268))
    text(c, "Know before you paddle out", font: "HelveticaNeue", size: 40, color: sand,
         at: CGPoint(x: 436, y: 196))
    return c
}

// --- Android launcher -------------------------------------------------------
let res = "composeApp/src/androidMain/res"
// dp -> px per density bucket, for the 108dp adaptive canvas and 48dp legacy icon.
let densities: [(String, Int, Int)] = [
    ("mdpi", 108, 48), ("hdpi", 162, 72), ("xhdpi", 216, 96),
    ("xxhdpi", 324, 144), ("xxxhdpi", 432, 192),
]
for (bucket, adaptive, legacy) in densities {
    write(adaptiveForeground(adaptive), "\(res)/mipmap-\(bucket)/ic_launcher_foreground.png")
    write(roundedIcon(legacy, circular: false), "\(res)/mipmap-\(bucket)/ic_launcher.png")
    write(roundedIcon(legacy, circular: true), "\(res)/mipmap-\(bucket)/ic_launcher_round.png")
}

// --- Play listing -----------------------------------------------------------
// Play's 512 icon must be square and fully opaque; it applies its own mask.
let playIcon = ctx(512, 512)
fillGradient(playIcon, CGRect(x: 0, y: 0, width: 512, height: 512))
drawWaves(playIcon, size: 512 * 0.72, origin: CGPoint(x: 512 * 0.14, y: 512 * 0.14), color: white)
write(playIcon, "store-assets/icons/play-icon-512.png")
write(featureGraphic(), "store-assets/feature-graphic/play-feature-1024x500.png")
