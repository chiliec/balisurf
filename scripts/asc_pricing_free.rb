#!/usr/bin/env ruby
# Set BaliSurf's App Store price schedule to Free (USA base territory), the
# pricing gate that blocks submission. spaceship has no appPriceSchedules
# support, so this talks to the ASC API directly.
#
# Idempotent: POSTing the schedule again just re-states "free, forever".
#
# Env: ASC_KEY_ID (default below), ASC_ISSUER_ID, ASC_APP_ID.
require "jwt"
require "net/http"
require "json"
require "openssl"

KEY_ID  = ENV.fetch("ASC_KEY_ID", "948K3FKL2H")
ISSUER  = ENV.fetch("ASC_ISSUER_ID")
P8_PATH = Dir[File.expand_path("../AuthKey_*.p8", __dir__)].first || abort("No AuthKey_*.p8 in the repo root")
APP     = ENV.fetch("ASC_APP_ID") { abort("Set ASC_APP_ID (numeric Apple App ID)") }
BASE    = "USA"

def token
  key = OpenSSL::PKey::EC.new(File.read(P8_PATH))
  JWT.encode({ iss: ISSUER, iat: Time.now.to_i, exp: Time.now.to_i + 600, aud: "appstoreconnect-v1" },
             key, "ES256", { kid: KEY_ID, typ: "JWT" })
end

def call(method, path, body = nil)
  uri = URI("https://api.appstoreconnect.apple.com#{path}")
  req = (method == :post ? Net::HTTP::Post : Net::HTTP::Get).new(uri)
  req["Authorization"] = "Bearer #{token}"
  if body
    req["Content-Type"] = "application/json"
    req.body = JSON.dump(body)
  end
  res = Net::HTTP.start(uri.host, uri.port, use_ssl: true) { |h| h.request(req) }
  [res.code, (JSON.parse(res.body) rescue res.body)]
end

# The free price point for the base territory (customerPrice "0.0").
code, points = call(:get, "/v1/apps/#{APP}/appPricePoints?filter%5Bterritory%5D=#{BASE}&limit=200")
abort("Could not list price points (HTTP #{code}): #{points.inspect[0, 300]}") unless code == "200"
free = points["data"].find { |p| p["attributes"]["customerPrice"].to_f.zero? }
abort("No free price point in #{BASE}") unless free
puts "Free price point (#{BASE}): #{free['id']}"

body = {
  "data" => {
    "type" => "appPriceSchedules",
    "relationships" => {
      "app" => { "data" => { "type" => "apps", "id" => APP } },
      "baseTerritory" => { "data" => { "type" => "territories", "id" => BASE } },
      "manualPrices" => { "data" => [{ "type" => "appPrices", "id" => "${price1}" }] }
    }
  },
  "included" => [
    {
      "type" => "appPrices",
      "id" => "${price1}",
      "attributes" => { "startDate" => nil, "endDate" => nil },
      "relationships" => { "appPricePoint" => { "data" => { "type" => "appPricePoints", "id" => free["id"] } } }
    }
  ]
}

code, res = call(:post, "/v1/appPriceSchedules", body)
puts "POST /v1/appPriceSchedules -> HTTP #{code}"
puts JSON.pretty_generate(res) if code != "201"
abort("Pricing not set") unless code == "201"

code, prices = call(:get, "/v1/appPriceSchedules/#{APP}/manualPrices?include=appPricePoint&limit=5")
puts "Verify manualPrices -> HTTP #{code}"
if code == "200"
  (prices["included"] || []).each { |p| puts "  customerPrice=#{p.dig('attributes', 'customerPrice')}" }
end
