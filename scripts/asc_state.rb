#!/usr/bin/env ruby
# Read-only inspection of App Store Connect submission gates for BaliSurf.
# Uses the local ASC API key. Makes no writes.
#
# Env: ASC_KEY_ID, ASC_ISSUER_ID, ASC_APP_ID, plus APP_VERSION (default 1.0.0).
require "jwt"
require "net/http"
require "json"
require "openssl"

KEY_ID  = ENV.fetch("ASC_KEY_ID", "948K3FKL2H")
ISSUER  = ENV.fetch("ASC_ISSUER_ID")
P8_PATH = Dir[File.expand_path("../AuthKey_*.p8", __dir__)].first
APP_ID  = ENV.fetch("ASC_APP_ID") { abort("Set ASC_APP_ID (numeric Apple App ID)") }
VERSION = ENV.fetch("APP_VERSION", "1.0.0")

def token
  key = OpenSSL::PKey::EC.new(File.read(P8_PATH))
  payload = { iss: ISSUER, iat: Time.now.to_i, exp: Time.now.to_i + 600, aud: "appstoreconnect-v1" }
  JWT.encode(payload, key, "ES256", { kid: KEY_ID, typ: "JWT" })
end

def get(path)
  uri = URI("https://api.appstoreconnect.apple.com#{path}")
  req = Net::HTTP::Get.new(uri)
  req["Authorization"] = "Bearer #{token}"
  res = Net::HTTP.start(uri.host, uri.port, use_ssl: true) { |h| h.request(req) }
  [res.code, (JSON.parse(res.body) rescue res.body)]
end

puts "== App info (content rights) =="
code, appres = get("/v1/apps/#{APP_ID}")
puts "HTTP #{code}"
if appres.is_a?(Hash) && appres["data"]
  puts "  contentRightsDeclaration=#{appres['data']['attributes']['contentRightsDeclaration'].inspect}"
end

puts "\n== App Store versions =="
code, versions = get("/v1/apps/#{APP_ID}/appStoreVersions?limit=5")
puts "HTTP #{code}"
if versions.is_a?(Hash) && versions["data"]
  versions["data"].each do |v|
    a = v["attributes"]
    puts "  #{a['versionString']}  state=#{a['appStoreState']}  id=#{v['id']}"
  end
  ver = versions["data"].find { |v| v["attributes"]["versionString"] == VERSION } || versions["data"].first
  vid = ver && ver["id"]

  if vid
    puts "\n== Build attached to version =="
    c, b = get("/v1/appStoreVersions/#{vid}/build")
    puts "HTTP #{c}  #{b.is_a?(Hash) && b['data'] ? b['data']['id'] : b.inspect[0, 200]}"
  end
end

# Age rating declaration lives under AppInfo (ASC API 1.3+), not AppStoreVersion.
puts "\n== Age rating declaration =="
code, infos = get("/v1/apps/#{APP_ID}/appInfos")
puts "HTTP #{code}"
if infos.is_a?(Hash) && infos["data"]
  info = infos["data"].first
  if info
    c, ar = get("/v1/appInfos/#{info['id']}/ageRatingDeclaration")
    puts "HTTP #{c}"
    puts JSON.pretty_generate(ar["data"]["attributes"]) if ar.is_a?(Hash) && ar["data"]
  end
end

# No relationship for data-usage publish state currently exists on the Apps
# resource in the public ASC API (confirmed 404/"relationship does not exist"
# under several path variants) -- verify App Privacy in the ASC web UI instead.
puts "\n== App Privacy: data usages publish state (unreliable via API; check ASC web UI) =="
c, ps = get("/v1/apps/#{APP_ID}/dataUsagePublishState")
puts "HTTP #{c}"
puts JSON.pretty_generate(ps["data"]["attributes"]) if ps.is_a?(Hash) && ps["data"]
