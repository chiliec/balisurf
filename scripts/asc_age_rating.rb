#!/usr/bin/env ruby
# Clear the App Store Connect age-rating gate for BaliSurf, idempotently:
# age rating -> 4+ (all content NONE, all booleans false), via
# AppInfo -> AgeRatingDeclaration (ASC API 1.3+; it moved off AppStoreVersion).
#
# Content rights is already declared USES_THIRD_PARTY_CONTENT (the app shows
# Open-Meteo forecast data and Copernicus Sentinel-derived reef overlays), so
# this script does not touch it.
#
# Env: ASC_KEY_ID (default below), ASC_ISSUER_ID, ASC_APP_ID.
require "spaceship"

APP = ENV.fetch("ASC_APP_ID") { abort("Set ASC_APP_ID (numeric Apple App ID)") }
P8  = Dir[File.expand_path("../AuthKey_*.p8", __dir__)].first
abort("No AuthKey_*.p8 in the repo root") unless P8

Spaceship::ConnectAPI.token = Spaceship::ConnectAPI::Token.create(
  key_id: ENV.fetch("ASC_KEY_ID", "948K3FKL2H"),
  issuer_id: ENV.fetch("ASC_ISSUER_ID"),
  filepath: File.expand_path(P8)
)

app = Spaceship::ConnectAPI::App.get(app_id: APP)
info = app.fetch_edit_app_info
puts "Editable AppInfo: #{info.id}"

ard = info.fetch_age_rating_declaration
puts "AgeRatingDeclaration: #{ard.id}"

# 4+ : no objectionable content of any kind, no interactive/exposure features.
# Session logs stay on-device and are never shared, so userGeneratedContent=false.
attributes = {
  "alcoholTobaccoOrDrugUseOrReferences" => "NONE",
  "contests" => "NONE",
  "gamblingSimulated" => "NONE",
  "gunsOrOtherWeapons" => "NONE",
  "horrorOrFearThemes" => "NONE",
  "matureOrSuggestiveThemes" => "NONE",
  "medicalOrTreatmentInformation" => "NONE",
  "profanityOrCrudeHumor" => "NONE",
  "sexualContentGraphicAndNudity" => "NONE",
  "sexualContentOrNudity" => "NONE",
  "violenceCartoonOrFantasy" => "NONE",
  "violenceRealisticProlongedGraphicOrSadistic" => "NONE",
  "violenceRealistic" => "NONE",
  "advertising" => false,
  "ageAssurance" => false,
  "gambling" => false,
  "healthOrWellnessTopics" => false,
  "lootBox" => false,
  "messagingAndChat" => false,
  "parentalControls" => false,
  "unrestrictedWebAccess" => false,
  "userGeneratedContent" => false,
  "ageRatingOverrideV2" => "NONE",
  "koreaAgeRatingOverride" => "NONE",
  "kidsAgeBand" => nil
}

ard.update(attributes: attributes)
puts "Updated age rating attributes."

info2 = app.fetch_edit_app_info
puts "App Store age rating now: #{info2.app_store_age_rating.inspect}"
